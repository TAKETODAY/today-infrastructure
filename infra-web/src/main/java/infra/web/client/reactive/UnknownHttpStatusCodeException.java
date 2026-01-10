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

package infra.web.client.reactive;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;

import infra.http.HttpHeaders;
import infra.http.HttpRequest;
import infra.http.HttpStatusCode;

/**
 * Exception thrown when an unknown (or custom) HTTP status code is received.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnknownHttpStatusCodeException extends WebClientResponseException {

  /**
   * Create a new instance of the {@code UnknownHttpStatusCodeException} with the given
   * parameters.
   */
  public UnknownHttpStatusCodeException(HttpStatusCode statusCode, HttpHeaders headers,
          byte[] responseBody, @Nullable Charset responseCharset, @Nullable HttpRequest request) {

    super("Unknown status code [%s]".formatted(statusCode), statusCode, "",
            headers, responseBody, responseCharset, request);
  }

}
