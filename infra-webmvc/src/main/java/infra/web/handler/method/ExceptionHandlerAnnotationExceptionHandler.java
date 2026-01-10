/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import infra.aop.support.AopUtils;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.RequestContext;
import infra.web.accept.ContentNegotiationManager;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.ExceptionHandler;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.handler.AbstractHandlerMethodExceptionHandler;
import infra.web.resource.ResourceHttpRequestHandler;

/**
 * Handle {@link ExceptionHandler} annotated method
 * <p>
 * this method indicates that is a exception handler
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.3.7 2019-06-22 19:17
 */
@SuppressWarnings("NullAway.Init")
public class ExceptionHandlerAnnotationExceptionHandler extends AbstractHandlerMethodExceptionHandler
        implements ApplicationContextAware, InitializingBean {

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
        matchingMetadata.setProducibleMediaTypes(mappingInfo.getProducibleTypes());
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
