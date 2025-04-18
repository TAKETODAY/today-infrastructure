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

package infra.web.handler.method;

import infra.core.MethodParameter;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.lang.Nullable;
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
  boolean supports(@Nullable Object body,
          @Nullable MethodParameter returnType, HttpMessageConverter<?> converter);

  /**
   * Invoked after an {@code HttpMessageConverter} is selected and just before
   * its write method is invoked.
   *
   * @param body the body to be written
   * @param returnType the return type of the controller method
   * @param contentType the content type selected through content negotiation
   * @param converter the converter selected to write to the response
   * @param context the current request context
   * @return the body that was passed in or a modified (possibly new) instance
   */
  @Nullable
  T beforeBodyWrite(@Nullable Object body, @Nullable MethodParameter returnType, MediaType contentType,
          HttpMessageConverter<?> converter, RequestContext context);

}

