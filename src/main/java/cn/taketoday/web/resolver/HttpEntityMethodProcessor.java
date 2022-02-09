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

package cn.taketoday.web.resolver;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.util.UriComponents;
import cn.taketoday.web.util.UriComponentsBuilder;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.RedirectModel;
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
public class HttpEntityMethodProcessor extends AbstractMessageConverterMethodProcessor {

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

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof HttpEntity && !(returnValue instanceof RequestEntity);
  }

  @Override
  public boolean supportsHandler(Object handler) {
    if (handler instanceof ActionMappingAnnotationHandler annotationHandler) {
      MethodParameter methodReturnType = annotationHandler.getMethod().getMethodReturnType();
      return HttpEntity.class.isAssignableFrom(methodReturnType.getParameterType())
              && !RequestEntity.class.isAssignableFrom(methodReturnType.getParameterType());
    }
    return false;
  }

  @Nullable
  @Override
  public Object resolveParameter(RequestContext context, ResolvableMethodParameter resolvable)
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
              HttpMethod.from(context.getMethodValue()), context.getURI());
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

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, @Nullable Object returnValue) throws IOException {
    context.setRequestHandled(true);
    if (returnValue == null) {
      return;
    }
    MethodParameter methodReturnType = null;
    if (handler instanceof ActionMappingAnnotationHandler annotationHandler) {
      HandlerMethod method = annotationHandler.getMethod();
      methodReturnType = method.getMethodReturnType();
    }
    else {
      // TODO
    }

    Assert.isInstanceOf(HttpEntity.class, returnValue);
    HttpEntity<?> httpEntity = (HttpEntity<?>) returnValue;

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
        String method = context.getMethodValue();
        if ((HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method))
                && isResourceNotModified(context)) {
          context.flush();
          return;
        }
      }
      else if (returnStatus / 100 == 3) {
        String location = outputHeaders.getFirst("location");
        if (location != null) {
          saveRedirectAttributes(context, location);
        }
      }
    }

    // Try even with null body. ResponseBodyAdvice could get involved.
    writeWithMessageConverters(httpEntity.getBody(), methodReturnType, context);

    // Ensure headers are flushed even if no body was written.
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

  private boolean isResourceNotModified(RequestContext context) {
    String method = context.getMethodValue();
    HttpHeaders responseHeaders = context.responseHeaders();
    String etag = responseHeaders.getETag();
    long lastModifiedTimestamp = responseHeaders.getLastModified();
    if (HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method)) {
      responseHeaders.remove(HttpHeaders.ETAG);
      responseHeaders.remove(HttpHeaders.LAST_MODIFIED);
    }
    return WebUtils.checkNotModified(etag, lastModifiedTimestamp, context);
  }

  private void saveRedirectAttributes(RequestContext request, String location) {
    Object attribute = request.getAttribute(RedirectModel.OUTPUT_ATTRIBUTE);
    if (attribute instanceof RedirectModel redirectModel) {
      if (redirectModelManager != null) {

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(location).build();
        redirectModel.setTargetRequestPath(uriComponents.getPath());
        redirectModel.addTargetRequestParams(uriComponents.getQueryParams());

        redirectModelManager.saveRedirectModel(request, redirectModel);
      }
    }
  }

  @Override
  protected Class<?> getReturnValueType(@Nullable Object returnValue, MethodParameter returnType) {
    if (returnValue != null) {
      return returnValue.getClass();
    }
    else {
      Type type = getHttpEntityType(returnType);
      type = type != null ? type : Object.class;
      return ResolvableType.forMethodParameter(returnType, type).toClass();
    }
  }

}
