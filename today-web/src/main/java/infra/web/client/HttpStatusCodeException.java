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
          byte @Nullable [] responseBody, @Nullable Charset responseCharset) {

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
          @Nullable HttpHeaders responseHeaders, byte @Nullable [] responseBody, @Nullable Charset responseCharset) {

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
          @Nullable HttpHeaders responseHeaders, byte @Nullable [] responseBody, @Nullable Charset responseCharset) {
    super(message == null ? getMessage(statusCode, statusText) : message, statusCode, statusText, responseHeaders, responseBody, responseCharset);
  }

  private static String getMessage(HttpStatusCode statusCode, String statusText) {
    if (StringUtils.isEmpty(statusText) && statusCode instanceof HttpStatus status) {
      statusText = status.getReasonPhrase();
    }
    return statusCode.value() + " " + statusText;
  }

}
