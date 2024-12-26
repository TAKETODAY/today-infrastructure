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

package infra.web.socket.client.support;

import java.net.URI;

import infra.http.AbstractHttpRequest;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/12/26 20:19
 */
final class NettyHttpRequest extends AbstractHttpRequest implements HttpRequest {

  private final URI uri;

  private final HttpHeaders headers;

  NettyHttpRequest(URI uri, HttpHeaders headers) {
    this.uri = uri;
    this.headers = headers.asReadOnly();
  }

  @Override
  public HttpMethod getMethod() {
    return HttpMethod.GET;
  }

  @Override
  public URI getURI() {
    return uri;
  }

  @Override
  public HttpHeaders getHeaders() {
    return headers;
  }

}
