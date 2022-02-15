/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.client;

import java.io.Serial;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;

/**
 * Raised when no suitable
 * {@link cn.taketoday.http.converter.HttpMessageConverter} could be
 * found to extract the response.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class UnknownContentTypeException extends RestClientException {

  @Serial
  private static final long serialVersionUID = 2759516676367274084L;

  private final Type targetType;

  private final MediaType contentType;

  private final int rawStatusCode;

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

    super("Could not extract response: no suitable HttpMessageConverter found " +
                  "for response type [" + targetType + "] and content type [" + contentType + "]");

    this.targetType = targetType;
    this.contentType = contentType;
    this.rawStatusCode = statusCode;
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
   * Return the raw HTTP status code value.
   */
  public int getRawStatusCode() {
    return this.rawStatusCode;
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
    return new String(
            this.responseBody,
            this.contentType.getCharset() != null
            ? this.contentType.getCharset()
            : StandardCharsets.UTF_8);
  }

}
