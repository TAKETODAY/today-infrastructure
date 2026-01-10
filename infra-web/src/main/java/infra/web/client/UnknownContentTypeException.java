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

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.MediaType;

/**
 * Raised when no suitable
 * {@link infra.http.converter.HttpMessageConverter} could be
 * found to extract the response.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnknownContentTypeException extends RestClientException {

  private final transient Type targetType;

  private final MediaType contentType;

  private final HttpStatusCode statusCode;

  private final String statusText;

  private final byte[] responseBody;

  private final HttpHeaders responseHeaders;

  /**
   * Construct a new instance of with the given response data.
   *
   * @param targetType the expected target type
   * @param contentType the content type of the response
   * @param statusCode the raw status code value
   * @param statusText the status text
   * @param responseHeaders the response headers (may be {@code null})
   * @param responseBody the response body content (may be {@code null})
   */
  public UnknownContentTypeException(Type targetType, MediaType contentType,
          int statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody) {

    this(targetType, contentType, HttpStatusCode.valueOf(statusCode), statusText, responseHeaders, responseBody);
  }

  /**
   * Construct a new instance of with the given response data.
   *
   * @param targetType the expected target type
   * @param contentType the content type of the response
   * @param statusCode the raw status code value
   * @param statusText the status text
   * @param responseHeaders the response headers (may be {@code null})
   * @param responseBody the response body content (may be {@code null})
   */
  public UnknownContentTypeException(Type targetType, MediaType contentType,
          HttpStatusCode statusCode, String statusText, HttpHeaders responseHeaders, byte[] responseBody) {

    super("Could not extract response: no suitable HttpMessageConverter found for response type [%s] and content type [%s]"
            .formatted(targetType, contentType));

    this.targetType = targetType;
    this.contentType = contentType;
    this.statusCode = statusCode;
    this.statusText = statusText;
    this.responseHeaders = responseHeaders;
    this.responseBody = responseBody;
  }

  /**
   * Return the target type expected for the response.
   */
  public Type getTargetType() {
    return this.targetType;
  }

  /**
   * Return the content type of the response, or "application/octet-stream".
   */
  public MediaType getContentType() {
    return this.contentType;
  }

  /**
   * Return the HTTP status code value.
   */
  public HttpStatusCode getStatusCode() {
    return this.statusCode;
  }

  /**
   * Return the raw HTTP status code value.
   */
  public int getRawStatusCode() {
    return this.statusCode.value();
  }

  /**
   * Return the HTTP status text.
   */
  public String getStatusText() {
    return this.statusText;
  }

  /**
   * Return the HTTP response headers.
   */
  @Nullable
  public HttpHeaders getResponseHeaders() {
    return this.responseHeaders;
  }

  /**
   * Return the response body as a byte array.
   */
  public byte[] getResponseBody() {
    return this.responseBody;
  }

  /**
   * Return the response body converted to String using the charset from the
   * response "Content-Type" or {@code "UTF-8"} otherwise.
   */
  public String getResponseBodyAsString() {
    return new String(responseBody, contentType.getCharset() != null ? contentType.getCharset() : StandardCharsets.UTF_8);
  }

}
