/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.reactive.function.client;

import java.nio.charset.Charset;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Nullable;

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
  public UnknownHttpStatusCodeException(int statusCode, HttpHeaders headers, byte[] responseBody, Charset responseCharset) {
    super("Unknown status code [%d]".formatted(statusCode), statusCode, "",
            headers, responseBody, responseCharset);
  }

  /**
   * Create a new instance of the {@code UnknownHttpStatusCodeException} with the given
   * parameters.
   */
  public UnknownHttpStatusCodeException(int statusCode, HttpHeaders headers, byte[] responseBody,
          @Nullable Charset responseCharset, @Nullable HttpRequest request) {

    super("Unknown status code [%d]".formatted(statusCode), statusCode, "",
            headers, responseBody, responseCharset, request);
  }

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
