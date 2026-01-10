/*
 * Copyright 2017 - 2026 the TODAY authors.
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
