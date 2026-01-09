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

package infra.web;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.util.CollectionUtils;

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
  public HttpMediaTypeNotSupportedException(@Nullable String message, Collection<MediaType> mediaTypes) {
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
  public HttpMediaTypeNotSupportedException(@Nullable MediaType contentType, Collection<MediaType> mediaTypes) {
    this(contentType, mediaTypes, null);
  }

  /**
   * Create a new HttpMediaTypeNotSupportedException.
   *
   * @param contentType the unsupported content type
   * @param mediaTypes the list of supported media types
   * @param httpMethod the HTTP method of the request
   */
  public HttpMediaTypeNotSupportedException(@Nullable MediaType contentType, Collection<MediaType> mediaTypes, @Nullable HttpMethod httpMethod) {
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
          Collection<MediaType> supportedMediaTypes, @Nullable HttpMethod httpMethod, String message) {
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

