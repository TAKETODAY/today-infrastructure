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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.core.MethodParameter;
import infra.http.HttpInputMessage;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.web.RequestContext;
import infra.web.handler.method.ControllerAdviceBean;
import infra.web.handler.method.RequestBodyAdvice;
import infra.web.handler.method.ResponseBodyAdvice;

/**
 * Invokes {@link RequestBodyAdvice} and {@link ResponseBodyAdvice} where each
 * instance may be (and is most likely) wrapped with
 * {@link ControllerAdviceBean ControllerAdviceBean}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 21:44
 */
public class RequestResponseBodyAdviceChain implements RequestBodyAdvice, ResponseBodyAdvice<Object> {

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
  public static <T> List<T> getAdviceByType(@Nullable List<Object> bodyAdvice, Class<T> adviceType) {
    if (bodyAdvice != null) {
      ArrayList<T> result = new ArrayList<>();
      for (Object advice : bodyAdvice) {
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
  public boolean supports(MethodParameter param, Type type, HttpMessageConverter<?> converter) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean supports(@Nullable Object body, @Nullable MethodParameter returnType, HttpMessageConverter<?> converter) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
          Type targetType, HttpMessageConverter<?> converter) throws IOException {

    for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class, requestBodyAdvice)) {
      if (advice.supports(parameter, targetType, converter)) {
        inputMessage = advice.beforeBodyRead(inputMessage, parameter, targetType, converter);
      }
    }
    return inputMessage;
  }

  @Override
  public Object afterBodyRead(Object body, HttpInputMessage inputMessage,
          MethodParameter parameter, Type targetType, HttpMessageConverter<?> converter) {

    for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class, requestBodyAdvice)) {
      if (advice.supports(parameter, targetType, converter)) {
        body = advice.afterBodyRead(body, inputMessage, parameter, targetType, converter);
      }
    }
    return body;
  }

  @Override
  @Nullable
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Object beforeBodyWrite(@Nullable Object body, @Nullable MethodParameter returnType,
          MediaType contentType, HttpMessageConverter<?> converter, RequestContext context) {

    for (ResponseBodyAdvice advice : getMatchingAdvice(returnType, ResponseBodyAdvice.class, responseBodyAdvice)) {
      if (advice.supports(body, returnType, converter)) {
        body = advice.beforeBodyWrite(body, returnType, contentType, converter, context);
      }
    }
    return body;
  }

  @Override
  @Nullable
  public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage,
          MethodParameter parameter, Type targetType, HttpMessageConverter<?> converter) {

    for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class, requestBodyAdvice)) {
      if (advice.supports(parameter, targetType, converter)) {
        body = advice.handleEmptyBody(body, inputMessage, parameter, targetType, converter);
      }
    }
    return body;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <A> List<A> getMatchingAdvice(@Nullable MethodParameter parameter,
          Class<? extends A> adviceType, List<Object> availableAdvice) {
    if (CollectionUtils.isEmpty(availableAdvice)) {
      return Collections.emptyList();
    }

    Class<?> containingClass = null;
    ArrayList result = new ArrayList<>(availableAdvice.size());
    for (Object advice : availableAdvice) {
      if (advice instanceof ControllerAdviceBean adviceBean) {
        // TODO containingClass maybe null
        if (containingClass == null && parameter != null) {
          containingClass = parameter.getContainingClass();
        }

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

}

