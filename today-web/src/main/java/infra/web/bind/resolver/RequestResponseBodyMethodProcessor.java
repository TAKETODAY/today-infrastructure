/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import infra.core.MethodParameter;
import infra.http.ProblemDetail;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageNotReadableException;
import infra.web.ErrorResponse;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.RequestContext;
import infra.web.accept.ContentNegotiationManager;
import infra.web.annotation.RequestBody;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.handler.result.HandlerMethodReturnValueHandler;
import infra.web.view.ModelAndView;
import infra.web.view.View;
import infra.web.view.ViewRef;

/**
 * Resolves method arguments annotated with {@code @RequestBody} and handles return
 * values from methods annotated with {@code @ResponseBody} by reading and writing
 * to the body of the request or response with an {@link HttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/23 17:14
 */
public class RequestResponseBodyMethodProcessor extends AbstractMessageConverterMethodProcessor
        implements HandlerMethodReturnValueHandler {

  /**
   * Basic constructor with converters only. Suitable for resolving
   * {@code @RequestBody}. For handling {@code @ResponseBody} consider also
   * providing a {@code ContentNegotiationManager}.
   */
  public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters) {
    super(converters);
  }

  /**
   * Basic constructor with converters and {@code ContentNegotiationManager}.
   * Suitable for resolving {@code @RequestBody} and handling
   * {@code @ResponseBody} without {@code Request~} or
   * {@code ResponseBodyAdvice}.
   */
  public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters, @Nullable ContentNegotiationManager manager) {
    super(converters, manager);
  }

  /**
   * Complete constructor for resolving {@code @RequestBody} method arguments.
   * For handling {@code @ResponseBody} consider also providing a
   * {@code ContentNegotiationManager}.
   */
  public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters, @Nullable List<Object> requestResponseBodyAdvice) {
    super(converters, null, requestResponseBodyAdvice);
  }

  /**
   * Complete constructor for resolving {@code @RequestBody} and handling
   * {@code @ResponseBody}.
   */
  public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters,
          @Nullable ContentNegotiationManager manager, @Nullable List<Object> requestResponseBodyAdvice) {
    super(converters, manager, requestResponseBodyAdvice);
  }

  /**
   * Variant of{@link #RequestResponseBodyMethodProcessor(List, ContentNegotiationManager, List)}
   * with an additional {@link ErrorResponse.Interceptor} argument for return
   * value handling.
   *
   * @since 5.0
   */
  public RequestResponseBodyMethodProcessor(List<HttpMessageConverter<?>> converters, @Nullable ContentNegotiationManager manager,
          @Nullable List<Object> requestResponseBodyAdvice, List<ErrorResponse.Interceptor> interceptors) {
    super(converters, manager, requestResponseBodyAdvice, interceptors);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.hasParameterAnnotation(RequestBody.class);
  }

  /**
   * Throws MethodArgumentNotValidException if validation fails.
   *
   * @throws HttpMessageNotReadableException if {@link RequestBody#required()}
   * is {@code true} and there is no body content or if there is no suitable
   * converter to read the content with.
   */
  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    MethodParameter parameter = resolvable.getParameter();
    Object arg = readWithMessageConverters(context, parameter, parameter.getNestedGenericParameterType());
    validateIfApplicable(context, parameter, arg);
    return arg;
  }

  @Nullable
  @Override
  protected Object readWithMessageConverters(RequestContext request, MethodParameter parameter, Type paramType)
          throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException //
  {
    Object arg = super.readWithMessageConverters(request, parameter, paramType);
    if (arg == null && checkRequired(parameter)) {
      throw new HttpMessageNotReadableException("Required request body is missing: " +
              parameter.getExecutable().toGenericString(), request);
    }
    return arg;
  }

  protected boolean checkRequired(MethodParameter parameter) {
    RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
    return requestBody != null && requestBody.required() && !parameter.isOptional();
  }

  //---------------------------------------------------------------------
  // Implementation of ReturnValueHandler interface
  //---------------------------------------------------------------------

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    return handler.isResponseBody();
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return !(returnValue instanceof ModelAndView
            || returnValue instanceof View
            || returnValue instanceof ViewRef);
  }

  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof ProblemDetail detail) {
      context.setStatus(detail.getStatus());
      if (detail.getInstance() == null) {
        URI path = URI.create(context.getRequestURI());
        detail.setInstance(path);
      }

      invokeErrorResponseInterceptors(detail, null);
    }

    // Try even with null return value. ResponseBodyAdvice could get involved.
    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null && handlerMethod.getRawReturnType().isInstance(returnValue)) {
      writeWithMessageConverters(returnValue, handlerMethod.getReturnType(), context);
    }
    else if (returnValue != null) {
      // value can not-assignable to returnType, value maybe from HandlerInterceptor
      writeWithMessageConverters(returnValue, null, context);
    }
  }

}
