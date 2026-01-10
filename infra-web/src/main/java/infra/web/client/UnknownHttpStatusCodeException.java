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

/**
 * Exception thrown when an unknown (or custom) HTTP status code is received.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnknownHttpStatusCodeException extends RestClientResponseException {

  /**
   * Construct a new instance of {@code HttpStatusCodeException} based on a
   * status code, status text, and response body content.
   *
   * @param rawStatusCode the raw status code value
   * @param statusText the status text
   * @param responseHeaders the response headers (may be {@code null})
   * @param responseBody the response body content (may be {@code null})
   * @param responseCharset the response body charset (may be {@code null})
   */
  public UnknownHttpStatusCodeException(int rawStatusCode, String statusText, @Nullable HttpHeaders responseHeaders,
          byte @Nullable [] responseBody, @Nullable Charset responseCharset) {

    this("Unknown status code [%d] %s".formatted(rawStatusCode, statusText),
            rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);
  }

  /**
   * Construct a new instance of {@code HttpStatusCodeException} based on a
   * status code, status text, and response body content.
   *
   * @param rawStatusCode the raw status code value
   * @param statusText the status text
   * @param responseHeaders the response headers (may be {@code null})
   * @param responseBody the response body content (may be {@code null})
   * @param responseCharset the response body charset (may be {@code null})
   */
  public UnknownHttpStatusCodeException(String message, int rawStatusCode, String statusText,
          @Nullable HttpHeaders responseHeaders, byte @Nullable [] responseBody, @Nullable Charset responseCharset) {

    super(message, rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);
  }
}
