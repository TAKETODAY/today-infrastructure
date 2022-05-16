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

package cn.taketoday.web;

import java.util.Collections;
import java.util.List;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ProblemDetail;

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

  /**
   * Create a new HttpMediaTypeException.
   *
   * @param message the exception message
   */
  protected HttpMediaTypeException(String message) {
    super(message);
    this.supportedMediaTypes = Collections.emptyList();
  }

  /**
   * Create a new HttpMediaTypeException with a list of supported media types.
   *
   * @param supportedMediaTypes the list of supported media types
   */
  protected HttpMediaTypeException(String message, List<MediaType> supportedMediaTypes) {
    super(message);
    this.supportedMediaTypes = Collections.unmodifiableList(supportedMediaTypes);
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

}
