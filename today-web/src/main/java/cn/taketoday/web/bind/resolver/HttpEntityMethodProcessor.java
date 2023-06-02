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

package cn.taketoday.web.bind.resolver;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.view.RedirectModelManager;

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
public class HttpEntityMethodProcessor
        extends AbstractMessageConverterMethodProcessor implements HandlerMethodReturnValueHandler {

  @Nullable
  private final RedirectModelManager redirectModelManager;

  /**
   * Basic constructor with converters only. Suitable for resolving
   * {@code HttpEntity}. For handling {@code ResponseEntity} consider also
   * providing a {@code ContentNegotiationManager}.
   */
  public HttpEntityMethodProcessor(
          List<HttpMessageConverter<?>> converters,
          @Nullable RedirectModelManager redirectModelManager) {
    super(converters);
    this.redirectModelManager = redirectModelManager;
  }

  /**
   * Basic constructor with converters and {@code ContentNegotiationManager}.
   * Suitable for resolving {@code HttpEntity} and handling {@code ResponseEntity}
   * without {@code Request~} or {@code ResponseBodyAdvice}.
   */
  public HttpEntityMethodProcessor(
          List<HttpMessageConverter<?>> converters,
          @Nullable ContentNegotiationManager manager,
          @Nullable RedirectModelManager redirectModelManager) {
    super(converters, manager);
    this.redirectModelManager = redirectModelManager;
  }

  /**
   * Complete constructor for resolving {@code HttpEntity} method arguments.
   * For handling {@code ResponseEntity} consider also providing a
   * {@code ContentNegotiationManager}.
   */
  public HttpEntityMethodProcessor(
          List<HttpMessageConverter<?>> converters,
          @Nullable List<Object> requestResponseBodyAdvice,
          @Nullable RedirectModelManager redirectModelManager) {
    super(converters, null, requestResponseBodyAdvice);
    this.redirectModelManager = redirectModelManager;
  }

  /**
   * Complete constructor for resolving {@code HttpEntity} and handling
   * {@code ResponseEntity}.
   */
  public HttpEntityMethodProcessor(
          List<HttpMessageConverter<?>> converters,
          @Nullable ContentNegotiationManager manager,
          List<Object> requestResponseBodyAdvice,
          @Nullable RedirectModelManager redirectModelManager) {

    super(converters, manager, requestResponseBodyAdvice);
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
      throw new IllegalArgumentException("HttpEntity parameter '" + parameter.getParameterName() +
              "' in method " + parameter.getMethod() + " is not parameterized");
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
        throw new IllegalArgumentException("Expected single generic parameter on '" +
                parameter.getParameterName() + "' in method " + parameter.getMethod());
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
  public void handleReturnValue(
          RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
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
    }

    HttpHeaders outputHeaders = context.responseHeaders();
    HttpHeaders entityHeaders = httpEntity.getHeaders();
    if (!entityHeaders.isEmpty()) {

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
      int returnStatus = responseEntity.getStatusCodeValue();
      context.setStatus(returnStatus);
      if (returnStatus == 200) {
        HttpMethod method = context.getMethod();
        if ((HttpMethod.GET == method || HttpMethod.HEAD == method)
                && isResourceNotModified(context, method)) {
          context.flush();
          return;
        }
      }
      else if (returnStatus / 100 == 3) {
        String location = outputHeaders.getFirst(HttpHeaders.LOCATION);
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

    // Ensure headers are flushed even if no-body was written.
    context.flush();
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
