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

package cn.taketoday.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.support.Netty4HttpHeaders;
import cn.taketoday.lang.Nullable;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClientResponse;

/**
 * {@link ClientHttpResponse} implementation for the Reactor-Netty HTTP client.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ReactorNettyClientResponse implements ClientHttpResponse {

  private final HttpClientResponse response;

  private final Connection connection;

  private final HttpHeaders headers;

  private final Duration readTimeout;

  @Nullable
  private volatile InputStream body;

  public ReactorNettyClientResponse(HttpClientResponse response, Connection connection, Duration readTimeout) {
    this.response = response;
    this.connection = connection;
    this.readTimeout = readTimeout;
    this.headers = new Netty4HttpHeaders(response.responseHeaders()).asReadOnly();
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(this.response.status().code());
  }

  @Override
  public String getStatusText() {
    return this.response.status().reasonPhrase();
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public InputStream getBody() throws IOException {
    if (this.body == null) {
      InputStream body = this.connection.inbound().receive()
              .aggregate().asInputStream().block(this.readTimeout);
      if (body != null) {
        this.body = body;
      }
      else {
        throw new IOException("Could not receive body");
      }
    }
    return this.body;
  }

  @Override
  public void close() {
    this.connection.dispose();
  }
}
