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
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import infra.aop.support.AopUtils;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.util.Assert;
import infra.web.HttpContext;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.accept.ContentNegotiationManager;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.ExceptionHandler;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.handler.AbstractHandlerMethodExceptionHandler;
import infra.web.handler.function.HandlerFunction;
import infra.web.resource.ResourceHttpRequestHandler;
import infra.web.util.WebUtils;

/**
 * Handle exceptions through methods annotated with {@link ExceptionHandler @ExceptionHandler}.
 *
 * <p>Searches the controller class hierarchy first, then applicable
 * {@link ControllerAdvice @ControllerAdvice} beans in their configured order.
 * Handler selection takes both the exception type and the requested media types
 * into account.
 *
 * <p>An optional {@link ProblemDetailExceptionHandler} provides RFC 9457 problem
 * details when no application exception handler matches or the selected handler
 * rethrows or fails. Return values are processed by the normal return value
 * handling infrastructure; this handler does not resolve or render error views.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ExceptionHandlerMethodResolver
 * @see #setRenderUnhandledExceptionsAsProblemDetails(boolean)
 * @since 2.3.7 2019-06-22 19:17
 */
public class ExceptionHandlerAnnotationExceptionHandler extends AbstractHandlerMethodExceptionHandler
        implements ApplicationContextAware, InitializingBean {

  private final ConcurrentHashMap<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
          new ConcurrentHashMap<>(64);

  private final LinkedHashMap<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
          new LinkedHashMap<>();

  private @Nullable ApplicationContext applicationContext;

  private HandlerParameterFactory parameterFactory;

  private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

  private boolean renderUnhandledExceptionsAsProblemDetails;

  private @Nullable InvocableHandlerMethod problemDetailHandlerMethod;

  /**
   * Enable the RFC 9457 problem detail fallback for exceptions not handled by
   * controller-local or {@code @ControllerAdvice} exception handlers.
   * <p>The fallback also applies when the selected application exception handler
   * rethrows or fails, and handles the original exception rather than the handler
   * failure. Only {@link Exception} instances are eligible, and the fallback is
   * skipped if the response is already committed.
   * <p>The fallback result uses the normal return value handling infrastructure,
   * including content negotiation, error response interceptors, and configured
   * error views. The default is {@code false}. Configure this option before
   * {@link #afterPropertiesSet() initialization}.
   *
   * @param enabled whether to render unhandled exceptions as problem details
   * @see ProblemDetailExceptionHandler
   * @since 5.0
   */
  public void setRenderUnhandledExceptionsAsProblemDetails(boolean enabled) {
    this.renderUnhandledExceptionsAsProblemDetails = enabled;
  }

  /**
   * Return whether unhandled exceptions are rendered as problem details.
   *
   * @see #setRenderUnhandledExceptionsAsProblemDetails(boolean)
   * @since 5.0
   */
  public boolean isRenderUnhandledExceptionsAsProblemDetails() {
    return this.renderUnhandledExceptionsAsProblemDetails;
  }

  /**
   * Set the registry used to resolve arguments of exception handler methods.
   * <p>If not configured, initialization looks for a
   * {@link ParameterResolvingRegistry} in the application context, falling back
   * to a default {@link HandlerParameterFactory} if none is available.
   *
   * @param registry the parameter resolving registry
   * @since 5.0
   */
  public void setParameterResolvingRegistry(ParameterResolvingRegistry registry) {
    this.parameterFactory = new RegistryHandlerParameterFactory(registry);
  }

  @Override
  public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Set the manager used to determine the requested media types when selecting
   * an {@code @ExceptionHandler} method.
   * <p>By default, a new {@link ContentNegotiationManager} is used.
   *
   * @param contentNegotiationManager the content negotiation manager
   * @since 5.0
   */
  public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
    this.contentNegotiationManager = contentNegotiationManager;
  }

  @Override
  protected @Nullable Object handleInternal(HttpContext context, @Nullable HandlerMethod handlerMethod, Throwable target) {
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

      if (exHandler == this.problemDetailHandlerMethod && target instanceof Exception exception) {
        return exHandler.invokeAndHandle(context, null,
                new Object[] { exception, WebUtils.resolveLocale(context) });
      }
      return exHandler.invokeAndHandle(context, null, arguments);
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
      if (exHandler != this.problemDetailHandlerMethod) {
        var fallback = getProblemDetailHandlerMethod(context);
        if (fallback != null && target instanceof Exception exception) {
          var metadata = context.getMatchingMetadata();
          if (metadata != null) {
            metadata.setProducibleMediaTypes(null);
            metadata.setHandler(fallback);
          }
          try {
            return fallback.invokeAndHandle(context, null, new Object[] { exception, WebUtils.resolveLocale(context) });
          }
          catch (Exception fallbackFailure) {
            logger.warn("Failure in problem detail handler", fallbackFailure);
          }
        }
      }
      return null;
    }
  }

  /**
   * Find a handler method for the given exception and requested media types.
   * <p>The default implementation searches {@code @ExceptionHandler} methods in
   * the controller class hierarchy first, then applicable
   * {@link ControllerAdvice @ControllerAdvice} beans in their configured order.
   * If no method matches, the configured problem detail fallback is returned for
   * {@link Exception} instances unless the response is already committed.
   * <p>If the requested media types cannot be resolved, matching proceeds with
   * {@link MediaType#ALL}.
   *
   * @param context the original web request that resulted in a handler error
   * @param handlerMethod the controller method associated with the request,
   * or {@code null} if none is available
   * @param exception the raised exception
   * @return the matching exception handler or problem detail fallback,
   * or {@code null} if neither is available
   */
  protected @Nullable InvocableHandlerMethod getExceptionHandlerMethod(HttpContext context, @Nullable HandlerMethod handlerMethod, Throwable exception) {
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

    return exception instanceof Exception ? getProblemDetailHandlerMethod(context) : null;
  }

  private @Nullable InvocableHandlerMethod getProblemDetailHandlerMethod(HttpContext context) {
    return context.isCommitted() ? null : this.problemDetailHandlerMethod;
  }

  private InvocableHandlerMethod createHandlerMethod(ExceptionHandlerMappingInfo mappingInfo, HttpContext context, Object advice) {
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
    return !this.exceptionHandlerAdviceCache.isEmpty() || this.problemDetailHandlerMethod != null;
  }

  @Override
  protected boolean shouldApplyTo(HttpContext request, @Nullable Object handler) {
    if (this.problemDetailHandlerMethod != null && !hasHandlerMappings()) {
      return true;  // render problem details for any handler
    }
    else if ((handler instanceof ResourceHttpRequestHandler || handler instanceof HandlerFunction)
            && hasGlobalExceptionHandlers() && !hasHandlerMappings()) {
      return true;  // apply to ResourceHttpRequestHandler and HandlerFunction by default
    }
    else {
      return super.shouldApplyTo(request, handler);
    }
  }

  //

  /**
   * Initialize argument resolution, discover {@code @ControllerAdvice} exception
   * handlers, and prepare the problem detail fallback if enabled.
   *
   * @throws IllegalStateException if no application context has been configured
   */
  @Override
  public void afterPropertiesSet() {
    ApplicationContext context = applicationContext;
    Assert.state(context != null, "No ApplicationContext");
    if (parameterFactory == null) {
      ParameterResolvingRegistry registry = BeanFactoryUtils.find(context, ParameterResolvingRegistry.class);
      if (registry != null) {
        parameterFactory = new RegistryHandlerParameterFactory(registry);
      }
      else {
        parameterFactory = new HandlerParameterFactory();
      }
    }
    initExceptionHandlerAdviceCache(context);
    if (this.renderUnhandledExceptionsAsProblemDetails) {
      try {
        this.problemDetailHandlerMethod = new InvocableHandlerMethod(new ProblemDetailExceptionHandler(context),
                ProblemDetailExceptionHandler.class.getMethod("handleException", Exception.class, Locale.class),
                context, this.parameterFactory);
      }
      catch (NoSuchMethodException ex) {
        throw new IllegalStateException("Problem detail handler method not found", ex);
      }
    }
    else {
      this.problemDetailHandlerMethod = null;
    }
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
