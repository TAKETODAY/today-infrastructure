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

import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;

/**
 * Exception thrown when a client POSTs, PUTs, or PATCHes content of a type
 * not supported by request handler.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 20:16
 */
@SuppressWarnings("serial")
public class HttpMediaTypeNotSupportedException extends HttpMediaTypeException {

  @Nullable
  private final MediaType contentType;

  /**
   * Create a new HttpMediaTypeNotSupportedException.
   *
   * @param message the exception message
   */
  public HttpMediaTypeNotSupportedException(String message) {
    super(message);
    this.contentType = null;
  }

  /**
   * Create a new HttpMediaTypeNotSupportedException.
   *
   * @param contentType the unsupported content type
   * @param supportedMediaTypes the list of supported media types
   */
  public HttpMediaTypeNotSupportedException(
          @Nullable MediaType contentType, List<MediaType> supportedMediaTypes) {
    this(contentType, supportedMediaTypes,
            "Content type '" + (contentType != null ? contentType : "") + "' not supported");
  }

  /**
   * Create a new HttpMediaTypeNotSupportedException.
   *
   * @param contentType the unsupported content type
   * @param supportedMediaTypes the list of supported media types
   * @param msg the detail message
   */
  public HttpMediaTypeNotSupportedException(
          @Nullable MediaType contentType, List<MediaType> supportedMediaTypes, String msg) {

    super(msg, supportedMediaTypes);
    this.contentType = contentType;
  }

  /**
   * Return the HTTP request content type method that caused the failure.
   */
  @Nullable
  public MediaType getContentType() {
    return this.contentType;
  }

}

