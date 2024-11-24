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

package infra.web.bind.resolver;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;
import infra.http.RequestEntity;
import infra.http.ResponseEntity;
import infra.http.converter.HttpMessageConverter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.StringUtils;
import infra.web.ErrorResponse;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.RedirectModelManager;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.accept.ContentNegotiationManager;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.handler.result.HandlerMethodReturnValueHandler;

/**
 * Resolves {@link HttpEntity} and {@link RequestEntity} method argument values
 * and also handles {@link HttpEntity} and {@link ResponseEntity} return values.
 *
 * <p>An {@link HttpEntity} return type has a specific purpose. Therefore this
 * handler should be configured ahead of handlers that support any return
 * value type annotated with {@code @ModelAttribute} or {@code @ResponseBody}
 * to ensure they don't take over.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/23 17:43
 */
public class HttpEntityMethodProcessor extends AbstractMessageConverterMethodProcessor implements HandlerMethodReturnValueHandler {

  @Nullable
  private final RedirectModelManager redirectModelManager;

  /**
   * Basic constructor with converters only. Suitable for resolving
   * {@code HttpEntity}. For handling {@code ResponseEntity} consider also
   * providing a {@code ContentNegotiationManager}.
   */
  public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters, @Nullable RedirectModelManager redirectModelManager) {
    super(converters);
    this.redirectModelManager = redirectModelManager;
  }

  /**
   * Complete constructor for resolving {@code HttpEntity} method arguments.
   * For handling {@code ResponseEntity} consider also providing a
   * {@code ContentNegotiationManager}.
   */
  public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
          @Nullable List<Object> requestResponseBodyAdvice, @Nullable RedirectModelManager redirectModelManager) {
    super(converters, null, requestResponseBodyAdvice);
    this.redirectModelManager = redirectModelManager;
  }

  /**
   * Complete constructor for resolving {@code HttpEntity} and handling
   * {@code ResponseEntity}.
   */
  public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters, @Nullable ContentNegotiationManager manager,
          @Nullable List<Object> requestResponseBodyAdvice, @Nullable RedirectModelManager redirectModelManager) {
    super(converters, manager, requestResponseBodyAdvice);
    this.redirectModelManager = redirectModelManager;
  }

  /**
   * Variant of {@link #HttpEntityMethodProcessor(List, ContentNegotiationManager, List, RedirectModelManager)}
   * with additional list of {@link ErrorResponse.Interceptor}s for return
   * value handling.
   *
   * @since 5.0
   */
  public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters, @Nullable ContentNegotiationManager manager,
          @Nullable List<Object> requestResponseBodyAdvice, @Nullable RedirectModelManager redirectModelManager, List<ErrorResponse.Interceptor> interceptors) {
    super(converters, manager, requestResponseBodyAdvice, interceptors);
    this.redirectModelManager = redirectModelManager;
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.is(HttpEntity.class)
            || resolvable.is(RequestEntity.class);
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable)
          throws IOException, HttpMediaTypeNotSupportedException {
    MethodParameter parameter = resolvable.getParameter();
    Type paramType = getHttpEntityType(parameter);
    if (paramType == null) {
      throw new IllegalArgumentException("HttpEntity parameter '%s' in method %s is not parameterized"
              .formatted(parameter.getParameterName(), parameter.getMethod()));
    }

    Object body = readWithMessageConverters(context, parameter, paramType);
    if (RequestEntity.class == parameter.getParameterType()) {
      return new RequestEntity<>(body, context.requestHeaders(),
              context.getMethod(), context.getURI());
    }
    else {
      return new HttpEntity<>(body, context.requestHeaders());
    }
  }

  @Nullable
  private Type getHttpEntityType(MethodParameter parameter) {
    Assert.isAssignable(HttpEntity.class, parameter.getParameterType());
    Type parameterType = parameter.getGenericParameterType();
    if (parameterType instanceof ParameterizedType type) {
      if (type.getActualTypeArguments().length != 1) {
        throw new IllegalArgumentException("Expected single generic parameter on '%s' in method %s"
                .formatted(parameter.getParameterName(), parameter.getMethod()));
      }
      return type.getActualTypeArguments()[0];
    }
    else if (parameterType instanceof Class) {
      return Object.class;
    }
    else {
      return null;
    }
  }

  //---------------------------------------------------------------------
  // Implementation of ReturnValueHandler interface
  //---------------------------------------------------------------------

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof HttpEntity && !(returnValue instanceof RequestEntity);
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handlerMethod) {
    MethodParameter returnType = handlerMethod.getReturnType();
    Class<?> type = returnType.getParameterType();
    return (HttpEntity.class.isAssignableFrom(type) && !RequestEntity.class.isAssignableFrom(type))
            || ErrorResponse.class.isAssignableFrom(type)
            || ProblemDetail.class.isAssignableFrom(type);
  }

  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue == null) {
      return;
    }

    HttpEntity<?> httpEntity;
    if (returnValue instanceof ErrorResponse response) {
      httpEntity = new ResponseEntity<>(response.getBody(), response.getHeaders(), response.getStatusCode());
    }
    else if (returnValue instanceof ProblemDetail detail) {
      httpEntity = ResponseEntity.of(detail).build();
    }
    else {
      Assert.isInstanceOf(HttpEntity.class, returnValue);
      httpEntity = (HttpEntity<?>) returnValue;
    }

    Object body = httpEntity.getBody();
    if (body instanceof ProblemDetail detail) {
      if (detail.getInstance() == null) {
        URI path = URI.create(context.getRequestURI());
        detail.setInstance(path);
      }

      if (logger.isWarnEnabled() && httpEntity instanceof ResponseEntity<?> responseEntity) {
        if (responseEntity.getStatusCode().value() != detail.getStatus()) {
          HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
          if (handlerMethod != null) {
            logger.warn("%s returned ResponseEntity: %s, but its status doesn't match the ProblemDetail status: %d"
                    .formatted(handlerMethod.getMethod().toGenericString(), responseEntity, detail.getStatus()));
          }
        }
      }
      invokeErrorResponseInterceptors(detail, returnValue instanceof ErrorResponse response ? response : null);
    }

    HttpHeaders entityHeaders = httpEntity.getHeaders();
    if (!entityHeaders.isEmpty()) {
      HttpHeaders outputHeaders = context.responseHeaders();

      for (Map.Entry<String, List<String>> entry : entityHeaders.entrySet()) {
        String key = entry.getKey();
        List<String> value = entry.getValue();
        if (HttpHeaders.VARY.equals(key) && outputHeaders.containsKey(HttpHeaders.VARY)) {
          List<String> values = getVaryRequestHeadersToAdd(outputHeaders, entityHeaders);
          if (!values.isEmpty()) {
            outputHeaders.setVary(values);
          }
        }
        else {
          outputHeaders.put(key, value);
        }
      }
    }

    if (httpEntity instanceof ResponseEntity<?> responseEntity) {
      HttpStatusCode returnStatus = responseEntity.getStatusCode();
      context.setStatus(returnStatus);
      if (returnStatus.is2xxSuccessful()) {
        HttpMethod method = context.getMethod();
        if ((HttpMethod.GET == method || HttpMethod.HEAD == method)
                && isResourceNotModified(context, method)) {
          return;
        }
      }
      else if (returnStatus.is3xxRedirection()) {
        String location = context.responseHeaders().getFirst(HttpHeaders.LOCATION);
        if (location != null) {
          saveRedirectAttributes(context, location);
        }
      }
    }

    HandlerMethod handlerMethod = HandlerMethod.unwrap(handler);
    if (handlerMethod != null) {
      MethodParameter methodReturnType = handlerMethod.getReturnType();
      // Try even with null body. ResponseBodyAdvice could get involved.
      writeWithMessageConverters(body, methodReturnType, context);
    }
    else if (body != null) {
      // for other handler's result
      writeWithMessageConverters(body, null, context);
    }
  }

  private List<String> getVaryRequestHeadersToAdd(HttpHeaders responseHeaders, HttpHeaders entityHeaders) {
    List<String> entityHeadersVary = entityHeaders.getVary();
    List<String> vary = responseHeaders.get(HttpHeaders.VARY);
    if (vary != null) {
      ArrayList<String> result = new ArrayList<>(entityHeadersVary);
      for (String header : vary) {
        for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
          if ("*".equals(existing)) {
            return Collections.emptyList();
          }
          for (String value : entityHeadersVary) {
            if (value.equalsIgnoreCase(existing)) {
              result.remove(value);
            }
          }
        }
      }
      return result;
    }
    return entityHeadersVary;
  }

  private boolean isResourceNotModified(RequestContext context, HttpMethod method) {
    HttpHeaders responseHeaders = context.responseHeaders();
    String etag = responseHeaders.getETag();
    long lastModifiedTimestamp = responseHeaders.getLastModified();
    if (HttpMethod.GET == method || HttpMethod.HEAD == method) {
      responseHeaders.remove(HttpHeaders.ETAG);
      responseHeaders.remove(HttpHeaders.LAST_MODIFIED);
    }
    return context.checkNotModified(etag, lastModifiedTimestamp);
  }

  private void saveRedirectAttributes(RequestContext request, String location) {
    RequestContextUtils.saveRedirectModel(location, request, redirectModelManager);
  }

  protected Class<?> getReturnValueType(@Nullable Object returnValue, @Nullable MethodParameter returnType) {
    if (returnValue != null) {
      return returnValue.getClass();
    }

    if (returnType != null) {
      Type type = getHttpEntityType(returnType);
      if (type == null) {
        type = Object.class;
      }
      return ResolvableType.forMethodParameter(returnType, type).toClass();
    }
    throw new IllegalStateException("return-value and return-type must not be null at same time");
  }

}
