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
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Abstract base class for exceptions based on an {@link HttpStatusCode}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class HttpStatusCodeException extends RestClientResponseException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Construct a new instance with an {@link HttpStatusCode}.
   *
   * @param statusCode the status code
   */
  protected HttpStatusCodeException(HttpStatusCode statusCode) {
    this(statusCode, name(statusCode), null, null, null);
  }

  private static String name(HttpStatusCode statusCode) {
    if (statusCode instanceof HttpStatus status) {
      return status.name();
    }
    else {
      return "";
    }
  }

  /**
   * Construct a new instance with an {@link HttpStatusCode} and status text.
   *
   * @param statusCode the status code
   * @param statusText the status text
   */
  protected HttpStatusCodeException(HttpStatusCode statusCode, String statusText) {
    this(statusCode, statusText, null, null, null);
  }

  /**
   * Construct instance with an {@link HttpStatusCode}, status text, and content.
   *
   * @param statusCode the status code
   * @param statusText the status text
   * @param responseBody the response body content, may be {@code null}
   * @param responseCharset the response body charset, may be {@code null}
   */
  protected HttpStatusCodeException(HttpStatusCode statusCode, String statusText,
          @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

    this(statusCode, statusText, null, responseBody, responseCharset);
  }

  /**
   * Construct instance with an {@link HttpStatusCode}, status text, content, and
   * a response charset.
   *
   * @param statusCode the status code
   * @param statusText the status text
   * @param responseHeaders the response headers, may be {@code null}
   * @param responseBody the response body content, may be {@code null}
   * @param responseCharset the response body charset, may be {@code null}
   */
  protected HttpStatusCodeException(HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

    this(getMessage(statusCode, statusText),
            statusCode, statusText, responseHeaders, responseBody, responseCharset);
  }

  /**
   * Construct instance with an {@link HttpStatusCode}, status text, content, and
   * a response charset.
   *
   * @param message the exception message
   * @param statusCode the status code
   * @param statusText the status text
   * @param responseHeaders the response headers, may be {@code null}
   * @param responseBody the response body content, may be {@code null}
   * @param responseCharset the response body charset, may be {@code null}
   */
  protected HttpStatusCodeException(String message, HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

    super(message, statusCode, statusText, responseHeaders, responseBody, responseCharset);
  }

  private static String getMessage(HttpStatusCode statusCode, String statusText) {
    if (StringUtils.isEmpty(statusText) && statusCode instanceof HttpStatus status) {
      statusText = status.getReasonPhrase();
    }
    return statusCode.value() + " " + statusText;
  }

}
