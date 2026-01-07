/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.web.handler.method;

import com.fasterxml.jackson.annotation.JsonView;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import infra.core.MethodParameter;
import infra.http.HttpInputMessage;
import infra.http.converter.AbstractJacksonHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.SmartHttpMessageConverter;
import infra.lang.Assert;

/**
 * A {@link RequestBodyAdvice} implementation that adds support for Jackson's
 * {@code @JsonView} annotation declared on MVC {@code @HttpEntity} or
 * {@code @RequestBody} method parameter.
 *
 * <p>Note that despite {@code @JsonView} allowing for more than one class to
 * be specified, the use for a request body advice is only supported with
 * exactly one class argument. Consider the use of a composite interface.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see com.fasterxml.jackson.annotation.JsonView
 * @see tools.jackson.databind.ObjectMapper#readerWithView(Class)
 * @since 4.0 2022/2/11 21:08
 */
public class JsonViewRequestBodyAdvice implements RequestBodyAdvice {

  @Override
  public boolean supports(MethodParameter methodParameter, Type targetType, HttpMessageConverter<?> converterType) {
    return converterType instanceof AbstractJacksonHttpMessageConverter
            && methodParameter.getParameterAnnotation(JsonView.class) != null;
  }

  @Override
  public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage,
          MethodParameter methodParameter, Type targetType, HttpMessageConverter<?> selected) {
    return inputMessage;
  }

  @Override
  public @Nullable Map<String, Object> determineReadHints(MethodParameter parameter, Type targetType, SmartHttpMessageConverter<?> selected) {
    return Collections.singletonMap(JsonView.class.getName(), getJsonView(parameter));
  }

  private static Class<?> getJsonView(MethodParameter methodParameter) {
    JsonView ann = methodParameter.getParameterAnnotation(JsonView.class);
    Assert.state(ann != null, "No JsonView annotation");

    Class<?>[] classes = ann.value();
    if (classes.length != 1) {
      throw new IllegalArgumentException(
              "@JsonView only supported for request body advice with exactly 1 class argument: " + methodParameter);
    }
    return classes[0];
  }

}
