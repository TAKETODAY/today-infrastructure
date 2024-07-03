/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.method;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.AbstractHandlerMethodExceptionHandler;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;
import cn.taketoday.web.util.DisconnectedClientHelper;

/**
 * Handle {@link ExceptionHandler} annotated method
 * <p>
 * this method indicates that is a exception handler
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.3.7 2019-06-22 19:17
 */
public class ExceptionHandlerAnnotationExceptionHandler extends AbstractHandlerMethodExceptionHandler
        implements ApplicationContextAware, InitializingBean {

  /**
   * Log category to use for network failure after a client has gone away.
   *
   * @see DisconnectedClientHelper
   */
  private static final String DISCONNECTED_CLIENT_LOG_CATEGORY = "cn.taketoday.web.handler.DisconnectedClient";

  private static final DisconnectedClientHelper disconnectedClientHelper =
          new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);

  private final ConcurrentHashMap<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
          new ConcurrentHashMap<>(64);

  private final LinkedHashMap<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
          new LinkedHashMap<>();

  @Nullable
  private ApplicationContext applicationContext;

  private ResolvableParameterFactory parameterFactory;

  private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

  /**
   * @since 5.0
   */
  public void setParameterResolvingRegistry(ParameterResolvingRegistry registry) {
    this.parameterFactory = new RegistryResolvableParameterFactory(registry);
  }

  @Override
  public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Set the {@link ContentNegotiationManager} to use to determine requested media types.
   * If not set, the default constructor is used.
   *
   * @since 5.0
   */
  public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
    this.contentNegotiationManager = contentNegotiationManager;
  }

  @Nullable
  @Override
  protected Object handleInternal(RequestContext context, @Nullable HandlerMethod handlerMethod, Throwable target) {
    // catch all handlers
    var exHandler = getExceptionHandlerMethod(context, handlerMethod, target);
    if (exHandler == null) {
      return null; // next
    }

    ArrayList<Throwable> exceptions = new ArrayList<>();
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Using @ExceptionHandler {}", exHandler);
      }
      // Expose causes as provided arguments as well
      Throwable exToExpose = target;
      while (exToExpose != null) {
        exceptions.add(exToExpose);
        Throwable cause = exToExpose.getCause();
        exToExpose = cause != exToExpose ? cause : null;
      }

      // efficient arraycopy call in ArrayList
      Object[] arguments = exceptions.toArray(new Object[exceptions.size() + 2]);
      if (handlerMethod != null) {
        arguments[arguments.length - 1] = handlerMethod;
        arguments[arguments.length - 2] = handlerMethod.getMethod();
      }

      var metadata = context.getMatchingMetadata();
      if (metadata != null) {
        metadata.setHandler(exHandler);
      }

      return exHandler.invokeAndHandle(context, arguments);
    }
    catch (Throwable invocationEx) {
      if (!disconnectedClientHelper.checkAndLogClientDisconnectedException(invocationEx)) {
        // Any other than the original exception (or a cause) is unintended here,
        // probably an accident (e.g. failed assertion or the like).
        if (!exceptions.contains(invocationEx) && logger.isWarnEnabled()) {
          logger.warn("Failure in @ExceptionHandler {}", exHandler, invocationEx);
        }
      }
      // Continue with default processing of the original exception...
      return null;
    }
  }

  /**
   * Find an {@code @ExceptionHandler} method for the given exception. The default
   * implementation searches methods in the class hierarchy of the controller first
   * and if not found, it continues searching for additional {@code @ExceptionHandler}
   * methods assuming some {@linkplain ControllerAdvice @ControllerAdvice}
   * Infra-managed beans were detected.
   *
   * @param context the original web request that resulted in a handler error
   * @param handlerMethod the method where the exception was raised (may be {@code null})
   * @param exception the raised exception
   * @return a method to handle the exception, or {@code null} if none
   */
  @Nullable
  protected InvocableHandlerMethod getExceptionHandlerMethod(RequestContext context, @Nullable HandlerMethod handlerMethod, Throwable exception) {
    List<MediaType> acceptedMediaTypes = List.of(MediaType.ALL);
    try {
      acceptedMediaTypes = contentNegotiationManager.resolveMediaTypes(context);
    }
    catch (HttpMediaTypeNotAcceptableException mediaTypeExc) {
      if (logger.isDebugEnabled()) {
        logger.debug("Could not resolve accepted media types for @ExceptionHandler [{}]",
                context.requestHeaders().getFirst(HttpHeaders.ACCEPT), mediaTypeExc);
      }
    }

    Class<?> handlerType = null;
    if (handlerMethod != null) {
      // Local exception handler methods on the controller class itself.
      // To be invoked through the proxy, even in case of an interface-based proxy.
      handlerType = handlerMethod.getBeanType();
      var resolver = exceptionHandlerCache.computeIfAbsent(handlerType, ExceptionHandlerMethodResolver::new);
      for (MediaType mediaType : acceptedMediaTypes) {
        ExceptionHandlerMappingInfo mappingInfo = resolver.resolveExceptionMapping(exception, mediaType);
        if (mappingInfo != null) {
          return createHandlerMethod(mappingInfo, context, handlerMethod.getBean());
        }
      }

      // For advice applicability check below (involving base packages, assignable types
      // and annotation presence), use target class instead of interface-based proxy.
      if (Proxy.isProxyClass(handlerType)) {
        handlerType = AopUtils.getTargetClass(handlerMethod.getBean());
      }
    }

    for (var entry : exceptionHandlerAdviceCache.entrySet()) {
      ControllerAdviceBean advice = entry.getKey();
      if (advice.isApplicableToBeanType(handlerType)) {
        ExceptionHandlerMethodResolver resolver = entry.getValue();
        for (MediaType mediaType : acceptedMediaTypes) {
          ExceptionHandlerMappingInfo mappingInfo = resolver.resolveExceptionMapping(exception, mediaType);
          if (mappingInfo != null) {
            return createHandlerMethod(mappingInfo, context, advice.resolveBean());
          }
        }
      }
    }

    return null;
  }

  private InvocableHandlerMethod createHandlerMethod(ExceptionHandlerMappingInfo mappingInfo, RequestContext context, Object advice) {
    if (!mappingInfo.getProducibleTypes().isEmpty()) {
      var matchingMetadata = context.getMatchingMetadata();
      if (matchingMetadata != null) {
        matchingMetadata.setProducibleMediaTypes(mappingInfo.getProducibleTypes().toArray(new MediaType[0]));
      }
    }
    return new InvocableHandlerMethod(advice, mappingInfo.getHandlerMethod(), applicationContext, parameterFactory);
  }

  @Override
  protected boolean hasGlobalExceptionHandlers() {
    return !this.exceptionHandlerAdviceCache.isEmpty();
  }

  @Override
  protected boolean shouldApplyTo(RequestContext request, @Nullable Object handler) {
    return (handler instanceof ResourceHttpRequestHandler ?
            hasGlobalExceptionHandlers() : super.shouldApplyTo(request, handler));
  }

  //

  @Override
  public void afterPropertiesSet() {
    ApplicationContext context = applicationContext;
    Assert.state(context != null, "No ApplicationContext");
    if (parameterFactory == null) {
      ParameterResolvingRegistry registry = BeanFactoryUtils.find(context, ParameterResolvingRegistry.class);
      if (registry != null) {
        parameterFactory = new RegistryResolvableParameterFactory(registry);
      }
      else {
        parameterFactory = new ResolvableParameterFactory();
      }
    }
    initExceptionHandlerAdviceCache(context);
  }

  private void initExceptionHandlerAdviceCache(ApplicationContext applicationContext) {
    var adviceBeans = ControllerAdviceBean.findAnnotatedBeans(applicationContext);
    for (ControllerAdviceBean adviceBean : adviceBeans) {
      Class<?> beanType = adviceBean.getBeanType();
      if (beanType == null) {
        throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
      }
      var resolver = new ExceptionHandlerMethodResolver(beanType);
      if (resolver.hasExceptionMappings()) {
        exceptionHandlerAdviceCache.put(adviceBean, resolver);
      }
    }

    if (logger.isDebugEnabled()) {
      int handlerSize = exceptionHandlerAdviceCache.size();
      if (handlerSize == 0) {
        logger.debug("ControllerAdvice beans: none");
      }
      else {
        logger.debug("ControllerAdvice beans: {} @ExceptionHandler", handlerSize);
      }
    }
  }

}
