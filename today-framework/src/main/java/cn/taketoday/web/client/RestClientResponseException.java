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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;

/**
 * Common base class for exceptions that contain actual HTTP response data.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class RestClientResponseException extends RestClientException {

  private static final long serialVersionUID = -8803556342728481792L;

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final int rawStatusCode;

  private final String statusText;

  private final byte[] responseBody;

  @Nullable
  private final HttpHeaders responseHeaders;

  @Nullable
  private final String responseCharset;

  /**
   * Construct a new instance of with the given response data.
   *
   * @param statusCode the raw status code value
   * @param statusText the status text
   * @param responseHeaders the response headers (may be {@code null})
   * @param responseBody the response body content (may be {@code null})
   * @param responseCharset the response body charset (may be {@code null})
   */
  public RestClientResponseException(String message, int statusCode, String statusText,
                                     @Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

    super(message);
    this.rawStatusCode = statusCode;
    this.statusText = statusText;
    this.responseHeaders = responseHeaders;
    this.responseBody = (responseBody != null ? responseBody : new byte[0]);
    this.responseCharset = (responseCharset != null ? responseCharset.name() : null);
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
  public byte[] getResponseBodyAsByteArray() {
    return this.responseBody;
  }

  /**
   * Return the response body converted to String. The charset used is that
   * of the response "Content-Type" or otherwise {@code "UTF-8"}.
   */
  public String getResponseBodyAsString() {
    return getResponseBodyAsString(DEFAULT_CHARSET);
  }

  /**
   * Return the response body converted to String. The charset used is that
   * of the response "Content-Type" or otherwise the one given.
   *
   * @param fallbackCharset the charset to use on if the response doesn't specify.
   * @since 4.0
   */
  public String getResponseBodyAsString(Charset fallbackCharset) {
    if (this.responseCharset == null) {
      return new String(this.responseBody, fallbackCharset);
    }
    try {
      return new String(this.responseBody, this.responseCharset);
    }
    catch (UnsupportedEncodingException ex) {
      // should not occur
      throw new IllegalStateException(ex);
    }
  }

}
