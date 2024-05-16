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

import java.net.URI;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;

/**
 * Exceptions that contain actual HTTP request data.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebClientRequestException extends WebClientException {

  private final HttpMethod method;

  private final URI uri;

  private final HttpHeaders headers;

  /**
   * Constructor for throwable.
   */
  public WebClientRequestException(Throwable ex, HttpMethod method, URI uri, HttpHeaders headers) {
    super(ex.getMessage(), ex);

    this.method = method;
    this.uri = uri;
    this.headers = HttpHeaders.copyOf(headers);
  }

  /**
   * Return the HTTP request method.
   */
  public HttpMethod getMethod() {
    return this.method;
  }

  /**
   * Return the request URI.
   */
  public URI getUri() {
    return this.uri;
  }

  /**
   * Return the HTTP request headers.
   */
  public HttpHeaders getHeaders() {
    return this.headers;
  }

}
