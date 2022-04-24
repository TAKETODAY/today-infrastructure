/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.method;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import cn.taketoday.context.MessageSource;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.method.support.ModelAndViewContainer;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.view.View;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Extends {@link InvocableHandlerMethod} with the ability to handle return
 * values through a registered {@link HandlerMethodReturnValueHandler} and
 * also supports setting the response status based on a method-level
 * {@code @ResponseStatus} annotation.
 *
 * <p>A {@code null} return value (including void) may be interpreted as the
 * end of request processing in combination with a {@code @ResponseStatus}
 * annotation, a not-modified check condition
 * (see {@link cn.taketoday.web.RequestContext#checkNotModified(long)}), or
 * a method argument that provides access to the response stream.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:50
 */
public class ServletInvocableHandlerMethod extends InvocableHandlerMethod {

  private static final Method CALLABLE_METHOD = ReflectionUtils.getMethod(Callable.class, "call");

  @Nullable
  private ReturnValueHandlerManager returnValueHandlers;

  /**
   * Creates an instance from the given handler and method.
   */
  public ServletInvocableHandlerMethod(Object handler, Method method) {
    super(handler, method);
  }

  /**
   * Variant of {@link #ServletInvocableHandlerMethod(Object, Method)} that
   * also accepts a {@link MessageSource}, e.g. to resolve
   * {@code @ResponseStatus} messages with.
   */
  public ServletInvocableHandlerMethod(Object handler, Method method, @Nullable MessageSource messageSource) {
    super(handler, method, messageSource);
  }

  /**
   * Create an instance from a {@code HandlerMethod}.
   */
  public ServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
    super(handlerMethod);
  }

  /**
   * Register {@link HandlerMethodReturnValueHandler} instances to use to
   * handle return values.
   */
  public void setHandlerMethodReturnValueHandlers(ReturnValueHandlerManager returnValueHandlers) {
    this.returnValueHandlers = returnValueHandlers;
  }

  /**
   * Invoke the method and handle the return value through one of the
   * configured {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}.
   *
   * @param request the current request
   * @param mavContainer the ModelAndViewContainer for this request
   * @param providedArgs "given" arguments matched by type (not resolved)
   */
  public void invokeAndHandle(
          RequestContext request, ModelAndViewContainer mavContainer, Object... providedArgs) throws Throwable {
    request.setModelContainer(mavContainer);

    Object returnValue = invokeForRequest(request, mavContainer, providedArgs);
    setResponseStatus(request);

    if (returnValue == null) {
      if (isRequestNotModified(request) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
        disableContentCachingIfNecessary(request);
        mavContainer.setRequestHandled(true);
        return;
      }
    }
    else if (StringUtils.hasText(getResponseStatusReason())) {
      mavContainer.setRequestHandled(true);
      return;
    }

    mavContainer.setRequestHandled(false);

    Assert.state(this.returnValueHandlers != null, "No return value handlers");
    try {
      returnValueHandlers.obtainHandler(this)
              .handleReturnValue(request, this, returnValue);
    }
    catch (Exception ex) {
      if (log.isTraceEnabled()) {
        log.trace(formatErrorForReturnValue(returnValue), ex);
      }
      throw ex;
    }
  }

  /**
   * Set the response status according to the {@link ResponseStatus} annotation.
   */
  private void setResponseStatus(RequestContext response) throws IOException {
    HttpStatusCode status = getResponseStatus();
    if (status == null) {
      return;
    }

    String reason = getResponseStatusReason();
    if (StringUtils.hasText(reason)) {
      response.sendError(status.value(), reason);
    }
    else {
      response.setStatus(status.value());
    }

    // To be picked up by RedirectView
    response.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, status);
  }

  /**
   * Does the given request qualify as "not modified"?
   *
   * @see RequestContext#checkNotModified(long)
   * @see RequestContext#checkNotModified(String)
   */
  private boolean isRequestNotModified(RequestContext webRequest) {
    return webRequest.isNotModified();
  }

  private void disableContentCachingIfNecessary(RequestContext webRequest) {
    if (isRequestNotModified(webRequest)) {
      HttpServletResponse response = ServletUtils.getServletResponse(webRequest);
      Assert.notNull(response, "Expected HttpServletResponse");
      if (StringUtils.hasText(response.getHeader(HttpHeaders.ETAG))) {
        HttpServletRequest request = ServletUtils.getServletRequest(webRequest);
        Assert.notNull(request, "Expected HttpServletRequest");
      }
    }
  }

  private String formatErrorForReturnValue(@Nullable Object returnValue) {
    return "Error handling return value=[" + returnValue + "]" +
            (returnValue != null ? ", type=" + returnValue.getClass().getName() : "") +
            " in " + this;
  }

  /**
   * Create a nested ServletInvocableHandlerMethod subclass that returns the
   * given value (or raises an Exception if the value is one) rather than
   * actually invoking the controller method. This is useful when processing
   * async return values (e.g. Callable, DeferredResult, ListenableFuture).
   */
  ServletInvocableHandlerMethod wrapConcurrentResult(Object result) {
    return new ConcurrentResultHandlerMethod(result, new ConcurrentResultMethodParameter(result));
  }

  /**
   * A nested subclass of {@code ServletInvocableHandlerMethod} that uses a
   * simple {@link Callable} instead of the original controller as the handler in
   * order to return the fixed (concurrent) result value given to it. Effectively
   * "resumes" processing with the asynchronously produced return value.
   */
  private class ConcurrentResultHandlerMethod extends ServletInvocableHandlerMethod {

    private final MethodParameter returnType;

    public ConcurrentResultHandlerMethod(final Object result, ConcurrentResultMethodParameter returnType) {
      super((Callable<Object>) () -> {
        if (result instanceof Throwable) {
          throw ExceptionUtils.sneakyThrow((Throwable) result);
        }
        return result;
      }, CALLABLE_METHOD);

      if (ServletInvocableHandlerMethod.this.returnValueHandlers != null) {
        setHandlerMethodReturnValueHandlers(ServletInvocableHandlerMethod.this.returnValueHandlers);
      }
      this.returnType = returnType;
    }

    /**
     * Bridge to actual controller type-level annotations.
     */
    @Override
    public Class<?> getBeanType() {
      return ServletInvocableHandlerMethod.this.getBeanType();
    }

    /**
     * Bridge to actual return value or generic type within the declared
     * async return type, e.g. Foo instead of {@code DeferredResult<Foo>}.
     */
    @Override
    public MethodParameter getReturnValueType(@Nullable Object returnValue) {
      return this.returnType;
    }

    /**
     * Bridge to controller method-level annotations.
     */
    @Override
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
      return ServletInvocableHandlerMethod.this.getMethodAnnotation(annotationType);
    }

    /**
     * Bridge to controller method-level annotations.
     */
    @Override
    public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
      return ServletInvocableHandlerMethod.this.hasMethodAnnotation(annotationType);
    }

    @Override
    public boolean isReturn(Class<?> returnType) {
      return this.returnType.getParameterType() == returnType;
    }

    @Override
    public boolean isReturnTypeAssignableTo(Class<?> superClass) {
      return superClass.isAssignableFrom(returnType.getParameterType());
    }

  }

  /**
   * MethodParameter subclass based on the actual return value type or if
   * that's null falling back on the generic type within the declared async
   * return type, e.g. Foo instead of {@code DeferredResult<Foo>}.
   */
  private class ConcurrentResultMethodParameter extends HandlerMethodParameter {

    @Nullable
    private final Object returnValue;

    private final ResolvableType returnType;

    public ConcurrentResultMethodParameter(Object returnValue) {
      super(-1);
      this.returnValue = returnValue;
      this.returnType = (returnValue instanceof ReactiveTypeHandler.CollectedValuesList
                         ? ((ReactiveTypeHandler.CollectedValuesList) returnValue).getReturnType()
                         : ResolvableType.fromType(super.getGenericParameterType()).getGeneric());
    }

    public ConcurrentResultMethodParameter(ConcurrentResultMethodParameter original) {
      super(original);
      this.returnValue = original.returnValue;
      this.returnType = original.returnType;
    }

    @Override
    public Class<?> getParameterType() {
      if (this.returnValue != null) {
        return this.returnValue.getClass();
      }
      if (!ResolvableType.NONE.equals(this.returnType)) {
        return this.returnType.toClass();
      }
      return super.getParameterType();
    }

    @Override
    public Type getGenericParameterType() {
      return this.returnType.getType();
    }

    @Override
    public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
      // Ensure @ResponseBody-style handling for values collected from a reactive type
      // even if actual return type is ResponseEntity<Flux<T>>
      return (super.hasMethodAnnotation(annotationType)
              || (annotationType == ResponseBody.class &&
              this.returnValue instanceof ReactiveTypeHandler.CollectedValuesList));
    }

    @Override
    public ConcurrentResultMethodParameter clone() {
      return new ConcurrentResultMethodParameter(this);
    }
  }

}
