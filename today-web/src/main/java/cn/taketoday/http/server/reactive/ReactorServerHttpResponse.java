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

package cn.taketoday.http.server.reactive;

import org.reactivestreams.Publisher;

import java.nio.file.Path;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.ZeroCopyHttpOutputMessage;
import cn.taketoday.http.support.Netty4HttpHeaders;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
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
            ? Mono.from(dataBuffers).map(NettyDataBufferFactory::toByteBuf)
            : Flux.from(dataBuffers).map(NettyDataBufferFactory::toByteBuf);
  }

  @Override
  protected void touchDataBuffer(DataBuffer buffer) {
    if (logger.isDebugEnabled()) {
      if (response instanceof ChannelOperationsId operationsId) {
        DataBufferUtils.touch(buffer, "Channel id: " + operationsId.asLongText());
      }
      else {
        response.withConnection(connection -> {
          ChannelId id = connection.channel().id();
          DataBufferUtils.touch(buffer, "Channel id: " + id.asShortText());
        });
      }
    }
  }

}
