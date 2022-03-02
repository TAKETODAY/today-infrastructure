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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ControllerAdviceBean;
import cn.taketoday.web.handler.method.RequestBodyAdvice;
import cn.taketoday.web.handler.method.ResponseBodyAdvice;

/**
 * Invokes {@link RequestBodyAdvice} and {@link ResponseBodyAdvice} where each
 * instance may be (and is most likely) wrapped with
 * {@link ControllerAdviceBean ControllerAdviceBean}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 21:44
 */
class RequestResponseBodyAdviceChain implements RequestBodyAdvice, ResponseBodyAdvice<Object> {

  private final ArrayList<Object> requestBodyAdvice = new ArrayList<>(4);
  private final ArrayList<Object> responseBodyAdvice = new ArrayList<>(4);

  /**
   * Create an instance from a list of objects that are either of type
   * {@code ControllerAdviceBean} or {@code RequestBodyAdvice}.
   */
  public RequestResponseBodyAdviceChain(@Nullable List<Object> requestResponseBodyAdvice) {
    this.requestBodyAdvice.addAll(getAdviceByType(requestResponseBodyAdvice, RequestBodyAdvice.class));
    this.responseBodyAdvice.addAll(getAdviceByType(requestResponseBodyAdvice, ResponseBodyAdvice.class));
  }

  @SuppressWarnings("unchecked")
  static <T> List<T> getAdviceByType(@Nullable List<Object> requestResponseBodyAdvice, Class<T> adviceType) {
    if (requestResponseBodyAdvice != null) {
      ArrayList<T> result = new ArrayList<>();
      for (Object advice : requestResponseBodyAdvice) {
        Class<?> beanType = advice instanceof ControllerAdviceBean adviceBean
                            ? adviceBean.getBeanType() : advice.getClass();
        if (beanType != null && adviceType.isAssignableFrom(beanType)) {
          result.add((T) advice);
        }
      }
      return result;
    }
    return Collections.emptyList();
  }

  @Override
  public boolean supports(MethodParameter param, Type type, Class<? extends HttpMessageConverter<?>> converterType) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public HttpInputMessage beforeBodyRead(
          HttpInputMessage request, MethodParameter parameter,
          Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {

    for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class)) {
      if (advice.supports(parameter, targetType, converterType)) {
        request = advice.beforeBodyRead(request, parameter, targetType, converterType);
      }
    }
    return request;
  }

  @Override
  public Object afterBodyRead(
          Object body, HttpInputMessage inputMessage, MethodParameter parameter,
          Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

    for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class)) {
      if (advice.supports(parameter, targetType, converterType)) {
        body = advice.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
      }
    }
    return body;
  }

  @Override
  @Nullable
  public Object beforeBodyWrite(
          @Nullable Object body, MethodParameter returnType, MediaType contentType,
          Class<? extends HttpMessageConverter<?>> converterType, RequestContext context) {

    return processBody(body, returnType, contentType, converterType, context);
  }

  @Override
  @Nullable
  public Object handleEmptyBody(
          @Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter,
          Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

    for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class)) {
      if (advice.supports(parameter, targetType, converterType)) {
        body = advice.handleEmptyBody(
                body, inputMessage, parameter, targetType, converterType);
      }
    }
    return body;
  }

  @Nullable
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <T> Object processBody(
          @Nullable Object body, MethodParameter returnType, MediaType contentType,
          Class<? extends HttpMessageConverter<?>> converterType, RequestContext context) {

    for (ResponseBodyAdvice<?> advice : getMatchingAdvice(returnType, ResponseBodyAdvice.class)) {
      if (advice.supports(returnType, converterType)) {
        body = ((ResponseBodyAdvice) advice).beforeBodyWrite(
                body, returnType, contentType, converterType, context);
      }
    }
    return body;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <A> List<A> getMatchingAdvice(MethodParameter parameter, Class<? extends A> adviceType) {
    List<Object> availableAdvice = getAdvice(adviceType);
    if (CollectionUtils.isEmpty(availableAdvice)) {
      return Collections.emptyList();
    }
    Class<?> containingClass = parameter.getContainingClass();
    ArrayList result = new ArrayList<>(availableAdvice.size());
    for (Object advice : availableAdvice) {
      if (advice instanceof ControllerAdviceBean adviceBean) {
        if (!adviceBean.isApplicableToBeanType(containingClass)) {
          continue;
        }
        advice = adviceBean.resolveBean();
      }
      if (adviceType.isAssignableFrom(advice.getClass())) {
        result.add(advice);
      }
    }
    return result;
  }

  private List<Object> getAdvice(Class<?> adviceType) {
    if (RequestBodyAdvice.class == adviceType) {
      return this.requestBodyAdvice;
    }
    else if (ResponseBodyAdvice.class == adviceType) {
      return this.responseBodyAdvice;
    }
    else {
      throw new IllegalArgumentException("Unexpected adviceType: " + adviceType);
    }
  }

}

