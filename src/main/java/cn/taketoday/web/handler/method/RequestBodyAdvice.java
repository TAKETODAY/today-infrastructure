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

package cn.taketoday.web.handler.method;

import java.io.IOException;
import java.lang.reflect.Type;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;

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
   * @param methodParameter the method parameter
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @param converterType the selected converter type
   * @return whether this interceptor should be invoked or not
   */
  boolean supports(MethodParameter methodParameter, Type targetType,
                   Class<? extends HttpMessageConverter<?>> converterType);

  /**
   * Invoked second before the request body is read and converted.
   *
   * @param inputMessage the request
   * @param parameter the target method parameter
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @param converterType the converter used to deserialize the body
   * @return the input request or a new instance (never {@code null})
   */
  HttpInputMessage beforeBodyRead(
          HttpInputMessage inputMessage, MethodParameter parameter,
          Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException;

  /**
   * Invoked third (and last) after the request body is converted to an Object.
   *
   * @param body set to the converter Object before the first advice is called
   * @param inputMessage the request
   * @param parameter the target method parameter
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @param converterType the converter used to deserialize the body
   * @return the same body or a new instance
   */
  Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                       Type targetType, Class<? extends HttpMessageConverter<?>> converterType);

  /**
   * Invoked second (and last) if the body is empty.
   *
   * @param body usually set to {@code null} before the first advice is called
   * @param inputMessage the request
   * @param parameter the method parameter
   * @param targetType the target type, not necessarily the same as the method
   * parameter type, e.g. for {@code HttpEntity<String>}.
   * @param converterType the selected converter type
   * @return the value to use, or {@code null} which may then raise an
   * {@code HttpMessageNotReadableException} if the argument is required
   */
  @Nullable
  Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                         Type targetType, Class<? extends HttpMessageConverter<?>> converterType);

}

