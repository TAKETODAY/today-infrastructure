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

package infra.http.server.reactive;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.nio.file.Path;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.core.io.buffer.NettyDataBuffer;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.http.ZeroCopyHttpOutputMessage;
import infra.http.support.Netty4HttpHeaders;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ChannelOperationsId;
import reactor.netty.http.server.HttpServerResponse;

/**
 * Adapt {@link ServerHttpResponse} to the {@link HttpServerResponse}.
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ReactorServerHttpResponse extends AbstractServerHttpResponse implements ZeroCopyHttpOutputMessage {
  private static final Logger logger = LoggerFactory.getLogger(ReactorServerHttpResponse.class);

  private final HttpServerResponse response;

  public ReactorServerHttpResponse(HttpServerResponse response, DataBufferFactory bufferFactory) {
    super(bufferFactory, new Netty4HttpHeaders(response.responseHeaders()));
    this.response = response;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getNativeResponse() {
    return (T) this.response;
  }

  @Nullable
  @Override
  public HttpStatusCode getStatusCode() {
    HttpStatusCode status = super.getStatusCode();
    return (status != null ? status : HttpStatus.resolve(this.response.status().code()));
  }

  @Override
  public Integer getRawStatusCode() {
    Integer status = super.getRawStatusCode();
    return (status != null ? status : this.response.status().code());
  }

  @Override
  protected void applyStatusCode() {
    Integer status = super.getRawStatusCode();
    if (status != null) {
      this.response.status(status);
    }
  }

  @Override
  protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> publisher) {
    return this.response.send(toByteBuf(publisher)).then();
  }

  @Override
  protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
    return this.response.sendGroups(Flux.from(publisher).map(this::toByteBuf)).then();
  }

  @Override
  protected void applyHeaders() { }

  @Override
  protected void applyCookies() {
    for (String name : getCookies().keySet()) {
      for (ResponseCookie httpCookie : getCookies().get(name)) {
        DefaultCookie cookie = new DefaultCookie(name, httpCookie.getValue());
        if (!httpCookie.getMaxAge().isNegative()) {
          cookie.setMaxAge(httpCookie.getMaxAge().getSeconds());
        }
        if (httpCookie.getDomain() != null) {
          cookie.setDomain(httpCookie.getDomain());
        }
        if (httpCookie.getPath() != null) {
          cookie.setPath(httpCookie.getPath());
        }
        cookie.setSecure(httpCookie.isSecure());
        cookie.setHttpOnly(httpCookie.isHttpOnly());
        cookie.setPartitioned(httpCookie.isPartitioned());
        if (httpCookie.getSameSite() != null) {
          cookie.setSameSite(CookieHeaderNames.SameSite.valueOf(httpCookie.getSameSite()));
        }
        this.response.addCookie(cookie);
      }
    }
  }

  @Override
  public Mono<Void> writeWith(Path file, long position, long count) {
    return doCommit(() -> this.response.sendFile(file, position, count).then());
  }

  private Publisher<ByteBuf> toByteBuf(Publisher<? extends DataBuffer> dataBuffers) {
    return dataBuffers instanceof Mono
            ? Mono.from(dataBuffers).map(NettyDataBuffer::toByteBuf)
            : Flux.from(dataBuffers).map(NettyDataBuffer::toByteBuf);
  }

  @Override
  protected void touchDataBuffer(DataBuffer buffer) {
    if (logger.isDebugEnabled()) {
      if (response instanceof ChannelOperationsId operationsId) {
        buffer.touch("Channel id: " + operationsId.asLongText());
      }
      else {
        response.withConnection(connection -> {
          ChannelId id = connection.channel().id();
          buffer.touch("Channel id: " + id.asShortText());
        });
      }
    }
  }

}
