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

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
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
public class HttpMediaTypeNotSupportedException extends HttpMediaTypeException {

  private static final String PARSE_ERROR_DETAIL_CODE =
          ErrorResponse.getDefaultDetailMessageCode(HttpMediaTypeNotSupportedException.class, "parseError");

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
    this(message, Collections.emptyList());
  }

  /**
   * Create a new HttpMediaTypeNotSupportedException for a parse error.
   *
   * @param message the exception message
   * @param mediaTypes list of supported media types
   */
  public HttpMediaTypeNotSupportedException(@Nullable String message, List<MediaType> mediaTypes) {
    super(message, mediaTypes, PARSE_ERROR_DETAIL_CODE, null);
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
  public HttpMediaTypeNotSupportedException(@Nullable MediaType contentType, List<MediaType> mediaTypes, @Nullable HttpMethod httpMethod) {
    this(contentType, mediaTypes, httpMethod,
            "Content-Type %sis not supported".formatted(contentType != null ? "'" + contentType + "' " : ""));
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
    super(message, supportedMediaTypes, null, new Object[] { contentType, supportedMediaTypes });
    this.contentType = contentType;
    this.httpMethod = httpMethod;
    getBody().setDetail("Content-Type '%s' is not supported.".formatted(this.contentType));
  }

  /**
   * Return the HTTP request content type method that caused the failure.
   */
  @Nullable
  public MediaType getContentType() {
    return this.contentType;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
  }

  @Override
  public HttpHeaders getHeaders() {
    if (CollectionUtils.isEmpty(getSupportedMediaTypes())) {
      return HttpHeaders.empty();
    }
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setAccept(getSupportedMediaTypes());
    if (httpMethod == HttpMethod.PATCH) {
      headers.setAcceptPatch(getSupportedMediaTypes());
    }
    return headers;
  }

}

