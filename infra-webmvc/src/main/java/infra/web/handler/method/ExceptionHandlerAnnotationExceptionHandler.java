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
import infra.http.ProblemDetail;
import infra.http.ResponseEntity;
import infra.web.ErrorResponse;
import infra.web.ErrorResponseViewResolver;
import infra.util.Assert;
import infra.web.HttpContext;
import infra.web.HttpMediaTypeNotAcceptableException;
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

  private @Nullable ErrorResponseViewResolver errorResponseViewResolver;

  private List<ErrorResponse.Interceptor> errorResponseInterceptors = List.of();

  /**
   * Configure interceptors to apply before resolving an error response view.
   * @param interceptors the interceptors to invoke
   * @since 5.0
   */
  public void setErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
    this.errorResponseInterceptors = List.copyOf(interceptors);
  }

  /**
   * Configure a resolver for problem detail views when the client prefers HTML.
   * @param resolver the resolver, or {@code null} to use message converters
   * @since 5.0
   */
  public void setErrorResponseViewResolver(@Nullable ErrorResponseViewResolver resolver) {
    this.errorResponseViewResolver = resolver;
  }

  /**
   * Return the configured problem detail view resolver, if any.
   * @since 5.0
   */
  public @Nullable ErrorResponseViewResolver getErrorResponseViewResolver() {
    return this.errorResponseViewResolver;
  }

  /**
   * Enable rendering exceptions not handled by application exception handlers as
   * RFC 9457 problem details. The fallback runs after local and controller advice
   * methods, including when such a method rethrows or fails. Its return value uses
   * the normal content negotiation and error response interceptor infrastructure.
   * <p>Already committed responses are not rendered. The default is {@code false}.
   * @param enabled whether to render unhandled exceptions as problem details
   * @since 5.0
   * @see ProblemDetailExceptionHandler
   */
  public void setRenderUnhandledExceptionsAsProblemDetails(boolean enabled) {
    this.renderUnhandledExceptionsAsProblemDetails = enabled;
  }

  /**
   * Return whether unhandled exceptions are rendered as problem details.
   * @since 5.0
   * @see #setRenderUnhandledExceptionsAsProblemDetails(boolean)
   */
  public boolean isRenderUnhandledExceptionsAsProblemDetails() {
    return this.renderUnhandledExceptionsAsProblemDetails;
  }

  /**
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
  protected Object handleInternal(HttpContext context, @Nullable HandlerMethod handlerMethod, Throwable target) {
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
        return resolveErrorView(context, exHandler.invokeAndHandle(context, null,
                new Object[] { exception, context.getLocale() }));
      }
      return resolveErrorView(context, exHandler.invokeAndHandle(context, null, arguments));
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
            return resolveErrorView(context,
                    fallback.invokeAndHandle(context, null, new Object[] { exception, context.getLocale() }));
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
  protected InvocableHandlerMethod getExceptionHandlerMethod(HttpContext context, @Nullable HandlerMethod handlerMethod, Throwable exception) {
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

  private @Nullable Object resolveErrorView(HttpContext context, @Nullable Object value) throws Exception {
    if (this.errorResponseViewResolver == null || context.getResponseContentType() != null) {
      return value;
    }
    ErrorResponse error = value instanceof ErrorResponse response ? response : null;
    Object body = error != null ? error.getBody() :
            value instanceof ResponseEntity<?> entity ? entity.getBody() : value;
    if (!(body instanceof ProblemDetail detail)) {
      return value;
    }
    if ((value instanceof ResponseEntity<?> entity && entity.getHeaders().getContentType() != null)
            || (error != null && error.getHeaders().getContentType() != null)) {
      return value;
    }
    var metadata = context.getMatchingMetadata();
    if (metadata != null && metadata.getProducibleMediaTypes() != null
            && !metadata.getProducibleMediaTypes().isEmpty()
            && metadata.getProducibleMediaTypes().stream().noneMatch(MediaType.TEXT_HTML::isCompatibleWith)) {
      return value;
    }
    boolean html = false;
    for (MediaType type : this.contentNegotiationManager.resolveMediaTypes(context)) {
      if (type.isConcrete()) {
        html = MediaType.TEXT_HTML.isCompatibleWith(type);
        break;
      }
    }
    if (!html) {
      return value;
    }
    try {
      for (var interceptor : this.errorResponseInterceptors) {
        interceptor.handleError(detail, error, context);
      }
    }
    catch (Throwable ignored) {
      // Match message-converter interceptor handling: rendering must continue.
    }
    var view = this.errorResponseViewResolver.resolveErrorView(context, detail, error);
    if (view == null) {
      return value;
    }
    if (value instanceof ResponseEntity<?> entity) {
      context.addHeaders(entity.getHeaders());
    }
    else if (error != null) {
      context.addHeaders(error.getHeaders());
    }
    if (view.getStatus() == null) {
      view.setStatus(value instanceof ResponseEntity<?> entity ? entity.getStatusCode() :
              infra.http.HttpStatusCode.valueOf(detail.getStatus()));
    }
    context.setStatus(view.getStatus());
    return view;
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
      return true;
    }
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
