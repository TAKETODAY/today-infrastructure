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
import java.nio.charset.Charset;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when an unknown (or custom) HTTP status code is received.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class UnknownHttpStatusCodeException extends RestClientResponseException {

  @Serial
  private static final long serialVersionUID = 7103980251635005491L;

  /**
   * Construct a new instance of {@code HttpStatusCodeException} based on an
   * {@link HttpStatus}, status text, and response body content.
   *
   * @param rawStatusCode the raw status code value
   * @param statusText the status text
   * @param responseHeaders the response headers (may be {@code null})
   * @param responseBody the response body content (may be {@code null})
   * @param responseCharset the response body charset (may be {@code null})
   */
  public UnknownHttpStatusCodeException(
          int rawStatusCode, String statusText, @Nullable HttpHeaders responseHeaders,
          @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

    this("Unknown status code [" + rawStatusCode + "]" + " " + statusText,
         rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);
  }

  /**
   * Construct a new instance of {@code HttpStatusCodeException} based on an
   * {@link HttpStatus}, status text, and response body content.
   *
   * @param rawStatusCode the raw status code value
   * @param statusText the status text
   * @param responseHeaders the response headers (may be {@code null})
   * @param responseBody the response body content (may be {@code null})
   * @param responseCharset the response body charset (may be {@code null})
   */
  public UnknownHttpStatusCodeException(
          String message, int rawStatusCode, String statusText,
          @Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody,
          @Nullable Charset responseCharset) {

    super(message, rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);
  }
}
