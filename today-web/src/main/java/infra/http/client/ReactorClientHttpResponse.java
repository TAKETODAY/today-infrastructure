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

package infra.http.client;

import org.reactivestreams.FlowAdapters;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.support.Netty4HttpHeaders;
import infra.lang.Nullable;
import infra.util.StreamUtils;
import io.netty.buffer.ByteBuf;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClientResponse;

/**
 * {@link ClientHttpResponse} implementation for the Reactor-Netty HTTP client.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ReactorClientHttpResponse implements ClientHttpResponse, Function<ByteBuf, byte[]> {

  private final HttpClientResponse response;

  private final Connection connection;

  private final HttpHeaders headers;

  @Nullable
  private volatile InputStream body;

  /**
   * Create a response instance.
   *
   * @param response the Reactor Netty response
   * @param connection the connection for the exchange
   */
  public ReactorClientHttpResponse(HttpClientResponse response, Connection connection) {
    this.response = response;
    this.connection = connection;
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
  public byte[] apply(ByteBuf byteBuf) {
    byte[] bytes = new byte[byteBuf.readableBytes()];
    byteBuf.readBytes(bytes);
    byteBuf.release();
    return bytes;
  }

  @Override
  public InputStream getBody() throws IOException {
    InputStream body = this.body;
    if (body != null) {
      return body;
    }

    try {
      var sis = new SubscriberInputStream<>(this, ByteBuf::release, 16);
      this.connection.inbound().receive().retain().subscribe(FlowAdapters.toSubscriber(sis));
      this.body = sis;
      return sis;
    }
    catch (RuntimeException ex) {
      throw ReactorClientHttpRequest.convertException(ex);
    }
  }

  @Override
  public void close() {
    try {
      InputStream body = getBody();
      StreamUtils.drain(body);
      body.close();
    }
    catch (IOException ignored) {
    }
  }

}
