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

package cn.taketoday.web;

import java.util.Collections;
import java.util.List;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base for exceptions related to media types. Adds a list of supported {@link MediaType MediaTypes}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 20:03
 */
public abstract class HttpMediaTypeException extends NestedRuntimeException implements ErrorResponse {

  private final List<MediaType> supportedMediaTypes;

  private final ProblemDetail body = ProblemDetail.forStatus(getStatusCode());

  private final String messageDetailCode;

  @Nullable
  private final Object[] messageDetailArguments;

  /**
   * Create a new HttpMediaTypeException with a list of supported media types.
   *
   * @param supportedMediaTypes the list of supported media types
   * @param messageDetailCode the code to use to resolve the problem "detail"
   * through a {@link cn.taketoday.context.MessageSource}
   * @param messageDetailArguments the arguments to make available when
   * resolving the problem "detail" through a {@code MessageSource}
   * @since 5.0
   */
  protected HttpMediaTypeException(@Nullable String message, List<MediaType> supportedMediaTypes,
          @Nullable String messageDetailCode, @Nullable Object[] messageDetailArguments) {

    super(message);
    this.messageDetailArguments = messageDetailArguments;
    this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
    this.messageDetailCode = messageDetailCode != null ? messageDetailCode : ErrorResponse.getDefaultDetailMessageCode(getClass(), null);
  }

  /**
   * Return the list of supported media types.
   */
  public List<MediaType> getSupportedMediaTypes() {
    return this.supportedMediaTypes;
  }

  @Override
  public ProblemDetail getBody() {
    return this.body;
  }

  @Override
  public String getDetailMessageCode() {
    return this.messageDetailCode;
  }

  @Override
  @Nullable
  public Object[] getDetailMessageArguments() {
    return this.messageDetailArguments;
  }

}
