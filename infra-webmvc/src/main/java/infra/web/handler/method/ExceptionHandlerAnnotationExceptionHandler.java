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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.util.Assert;
import infra.web.HandlerMatchingMetadata;
import infra.web.HttpContext;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.accept.ContentNegotiationManager;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.ExceptionHandler;
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

  private @Nullable ApplicationContext applicationContext;

  private volatile @Nullable ControllerMethodResolver methodResolver;

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
   * Set the shared resolver for this MVC application context before initialization.
   */
  public void setControllerMethodResolver(ControllerMethodResolver resolver) {
    this.methodResolver = resolver;
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

      var metadata = HandlerMatchingMetadata.find(context);
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
          var metadata = HandlerMatchingMetadata.find(context);
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

    ControllerMethodResolver resolver = this.methodResolver;
    Assert.state(resolver != null, "No ControllerMethodResolver");
    var resolved = resolver.resolveExceptionHandlerMethod(context, handlerMethod, exception, acceptedMediaTypes);
    if (resolved != null) {
      return resolved;
    }

    return exception instanceof Exception ? getProblemDetailHandlerMethod(context) : null;
  }

  private @Nullable InvocableHandlerMethod getProblemDetailHandlerMethod(HttpContext context) {
    return context.isCommitted() ? null : this.problemDetailHandlerMethod;
  }

  @Override
  protected boolean hasGlobalExceptionHandlers() {
    var resolver = this.methodResolver;
    return this.problemDetailHandlerMethod != null || (resolver != null && resolver.hasExceptionHandlerAdvice());
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
    var resolver = this.methodResolver;
    if (resolver == null) {
      resolver = ControllerMethodResolver.forExceptionHandlers(context);
    }
    resolver.initExceptionAdvice();
    this.methodResolver = resolver;
    if (this.renderUnhandledExceptionsAsProblemDetails) {
      try {
        this.problemDetailHandlerMethod = resolver.createHandlerMethod(new ProblemDetailExceptionHandler(context),
                ProblemDetailExceptionHandler.class.getMethod("handleException", Exception.class, Locale.class),
                context);
      }
      catch (NoSuchMethodException ex) {
        throw new IllegalStateException("Problem detail handler method not found", ex);
      }
    }
    else {
      this.problemDetailHandlerMethod = null;
    }
  }

}
