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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import infra.core.MethodParameter;
import infra.http.HttpInputMessage;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.SmartHttpMessageConverter;

/**
 * Allows customizing the request before its body is read and converted into an
 * Object and also allows for processing of the resulting Object before it is
 * passed into a controller method as an {@code @RequestBody} or an
 * {@code HttpEntity} method argument.
 *
 * <p>Implementations of this contract may be registered directly with the
 * {@code RequestMappingHandlerAdapter} or more likely annotated with
 * {@code @ControllerAdvice} in which case they are auto-detected.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 21:42
 */
public interface RequestBodyAdvice {

  /**
   * Invoked first to determine if this interceptor applies.
   *
   * @param parameter the method parameter
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @param converter the selected converter
   * @return whether this interceptor should be invoked or not
   */
  boolean supports(MethodParameter parameter, Type targetType, HttpMessageConverter<?> converter);

  /**
   * Invoked second before the inputMessage body is read and converted.
   *
   * @param inputMessage the inputMessage
   * @param parameter the target method parameter
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @param converter the converter used to deserialize the body
   * @return the input inputMessage or a new instance (never {@code null})
   */
  HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
          Type targetType, HttpMessageConverter<?> converter) throws IOException;

  /**
   * Invoked to determine read hints if the converter is a {@link SmartHttpMessageConverter}.
   *
   * @param parameter the target method parameter
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, for example, for {@code HttpEntity<String>}.
   * @param selected the selected converter type
   * @return the hints determined otherwise {@code null}
   * @since 5.0
   */
  default @Nullable Map<String, Object> determineReadHints(MethodParameter parameter,
          Type targetType, SmartHttpMessageConverter<?> selected) {

    return null;
  }

  /**
   * Invoked third (and last) after the request body is converted to an Object.
   *
   * @param body set to the converter Object before the first advice is called
   * @param inputMessage the request
   * @param parameter the target method parameter
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @param converter the converter used to deserialize the body
   * @return the same body or a new instance
   */
  default Object afterBodyRead(Object body, HttpInputMessage inputMessage,
          MethodParameter parameter, Type targetType, HttpMessageConverter<?> converter) {

    return body;
  }

  /**
   * Invoked second (and last) if the body is empty.
   *
   * @param body usually set to {@code null} before the first advice is called
   * @param inputMessage the request
   * @param parameter the method parameter
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @param converter the selected converter
   * @return the value to use, or {@code null} which may then raise an
   * {@code HttpMessageNotReadableException} if the argument is required
   */
  @Nullable
  default Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage,
          MethodParameter parameter, Type targetType, HttpMessageConverter<?> converter) {

    return body;
  }

}

