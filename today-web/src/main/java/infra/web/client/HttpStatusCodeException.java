/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.util.StringUtils;

/**
 * Abstract base class for exceptions based on an {@link HttpStatusCode}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class HttpStatusCodeException extends RestClientResponseException {

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
  protected HttpStatusCodeException(@Nullable String message, HttpStatusCode statusCode, String statusText,
          @Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {
    super(message == null ? getMessage(statusCode, statusText) : message, statusCode, statusText, responseHeaders, responseBody, responseCharset);
  }

  private static String getMessage(HttpStatusCode statusCode, String statusText) {
    if (StringUtils.isEmpty(statusText) && statusCode instanceof HttpStatus status) {
      statusText = status.getReasonPhrase();
    }
    return statusCode.value() + " " + statusText;
  }

}
