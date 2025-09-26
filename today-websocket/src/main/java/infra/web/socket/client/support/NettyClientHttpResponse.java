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

package infra.web.socket.client.support;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.client.ClientHttpResponse;
import infra.http.support.Netty4HttpHeaders;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/12/26 20:14
 */
final class NettyClientHttpResponse implements ClientHttpResponse {

  private final byte[] byteArray;

  private final HttpHeaders httpHeaders;

  private final FullHttpResponse response;

  NettyClientHttpResponse(FullHttpResponse response) {
    this.response = response;
    ByteBuf content = response.content();
    this.byteArray = new byte[content.readableBytes()];
    content.readBytes(byteArray);
    this.httpHeaders = new Netty4HttpHeaders(response.headers()).asReadOnly();
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(response.status().code());
  }

  @Override
  public String getStatusText() {
    return response.status().reasonPhrase();
  }

  @Override
  public void close() {
    ReferenceCountUtil.safeRelease(response);
  }

  @Override
  public InputStream getBody() {
    return new ByteArrayInputStream(byteArray);
  }

  public byte[] bodyByteArray() {
    return byteArray;
  }

  @Nullable
  public Charset getCharset() {
    MediaType contentType = httpHeaders.getContentType();
    return contentType != null ? contentType.getCharset() : null;
  }

  @Override
  public HttpHeaders getHeaders() {
    return httpHeaders;
  }

}
