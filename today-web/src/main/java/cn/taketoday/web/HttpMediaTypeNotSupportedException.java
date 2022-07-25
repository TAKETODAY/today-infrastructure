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

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

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

  @Nullable
  private final HttpMethod httpMethod;

  /**
   * Create a new HttpMediaTypeNotSupportedException.
   *
   * @param message the exception message
   */
  public HttpMediaTypeNotSupportedException(String message) {
    super(message);
    this.contentType = null;
    this.httpMethod = null;
    getBody().setDetail("Could not parse Content-Type.");
  }

  /**
   * Create a new HttpMediaTypeNotSupportedException.
   *
   * @param contentType the unsupported content type
   * @param mediaTypes the list of supported media types
   */
  public HttpMediaTypeNotSupportedException(@Nullable MediaType contentType, List<MediaType> mediaTypes) {
    this(contentType, mediaTypes, null);
  }

  /**
   * Create a new HttpMediaTypeNotSupportedException.
   *
   * @param contentType the unsupported content type
   * @param mediaTypes the list of supported media types
   * @param httpMethod the HTTP method of the request
   */
  public HttpMediaTypeNotSupportedException(
          @Nullable MediaType contentType, List<MediaType> mediaTypes, @Nullable HttpMethod httpMethod) {

    this(contentType, mediaTypes, httpMethod,
            "Content-Type " + (contentType != null ? "'" + contentType + "' " : "") + "is not supported");
  }

  /**
   * Create a new HttpMediaTypeNotSupportedException.
   *
   * @param contentType the unsupported content type
   * @param supportedMediaTypes the list of supported media types
   * @param httpMethod the HTTP method of the request
   * @param message the detail message
   */
  public HttpMediaTypeNotSupportedException(@Nullable MediaType contentType,
          List<MediaType> supportedMediaTypes, @Nullable HttpMethod httpMethod, String message) {

    super(message, supportedMediaTypes);
    this.contentType = contentType;
    this.httpMethod = httpMethod;
    getBody().setDetail("Content-Type '" + this.contentType + "' is not supported.");
  }

  /**
   * Return the HTTP request content type method that caused the failure.
   */
  @Nullable
  public MediaType getContentType() {
    return this.contentType;
  }

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
  }

  @Override
  public HttpHeaders getHeaders() {
    if (CollectionUtils.isEmpty(getSupportedMediaTypes())) {
      return HttpHeaders.EMPTY;
    }
    HttpHeaders headers = HttpHeaders.create();
    headers.setAccept(getSupportedMediaTypes());
    if (HttpMethod.PATCH.equals(this.httpMethod)) {
      headers.setAcceptPatch(getSupportedMediaTypes());
    }
    return headers;
  }

}

