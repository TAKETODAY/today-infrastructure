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
