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

import java.util.Map;

import infra.core.MethodParameter;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.SmartHttpMessageConverter;
import infra.web.RequestContext;

/**
 * Allows customizing the response after the execution of an {@code @ResponseBody}
 * or a {@code ResponseEntity} controller method but before the body is written
 * with an {@code HttpMessageConverter}.
 *
 * <p>Implementations may be registered directly with
 * {@code RequestMappingHandlerAdapter} and {@code ExceptionHandlerExceptionHandler}
 * or more likely annotated with {@code @ControllerAdvice} in which case they
 * will be auto-detected by both.
 *
 * @param <T> the body type
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 21:43
 */
public interface ResponseBodyAdvice<T> {

  /**
   * Whether this component supports the given controller method return type
   * and the selected {@code HttpMessageConverter} type.
   *
   * @param returnType the return type
   * @param converter the selected converter
   * @return {@code true} if {@link #beforeBodyWrite} should be invoked;
   * {@code false} otherwise
   */
  boolean supports(@Nullable Object body, @Nullable MethodParameter returnType, HttpMessageConverter<?> converter);

  /**
   * Invoked after an {@code HttpMessageConverter} is selected and just before
   * its write method is invoked.
   *
   * @param body the body to be written
   * @param returnType the return type of the controller method
   * @param contentType the content type selected through content negotiation
   * @param selected the converter selected to write to the response
   * @param context the current request context
   * @return the body that was passed in or a modified (possibly new) instance
   */
  @Nullable
  T beforeBodyWrite(@Nullable Object body, @Nullable MethodParameter returnType, MediaType contentType,
          HttpMessageConverter<?> selected, RequestContext context);

  /**
   * Invoked to determine write hints if the converter is a {@link SmartHttpMessageConverter}.
   *
   * @param body the body to be written
   * @param returnType the return type of the controller method
   * @param selectedContentType the content type selected through content negotiation
   * @param selected the converter type selected to write to the response
   * @return the hints determined otherwise {@code null}
   * @since 5.0
   */
  default @Nullable Map<String, Object> determineWriteHints(@Nullable T body, @Nullable MethodParameter returnType,
          MediaType selectedContentType, SmartHttpMessageConverter<?> selected) {

    return null;
  }

}

