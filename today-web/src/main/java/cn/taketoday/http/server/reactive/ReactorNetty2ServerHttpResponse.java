/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.server.reactive;

import org.reactivestreams.Publisher;

import java.nio.file.Path;
import java.util.List;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.Netty5DataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.ZeroCopyHttpOutputMessage;
import cn.taketoday.http.support.Netty5HeadersAdapter;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty5.ChannelOperationsId;
import reactor.netty5.http.server.HttpServerResponse;

/**
 * Adapt {@link ServerHttpResponse} to the {@link HttpServerResponse}.
 *
 * <p>This class is based on {@link ReactorServerHttpResponse}.
 *
 * @author Violeta Georgieva
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ReactorNetty2ServerHttpResponse extends AbstractServerHttpResponse implements ZeroCopyHttpOutputMessage {

  private static final Logger logger = LoggerFactory.getLogger(ReactorNetty2ServerHttpResponse.class);

  private final HttpServerResponse response;

  public ReactorNetty2ServerHttpResponse(HttpServerResponse response, DataBufferFactory bufferFactory) {
    super(bufferFactory, HttpHeaders.from(new Netty5HeadersAdapter(response.responseHeaders())));
    Assert.notNull(response, "HttpServerResponse must not be null");
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
    return (status != null ? status : HttpStatusCode.valueOf(this.response.status().code()));
  }

  @Override
  @Deprecated
  public Integer getRawStatusCode() {
    Integer status = super.getRawStatusCode();
    return (status != null ? status : this.response.status().code());
  }

  @Override
  protected void applyStatusCode() {
    HttpStatusCode status = super.getStatusCode();
    if (status != null) {
      this.response.status(status.value());
    }
  }

  @Override
  protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> publisher) {
    return this.response.send(toByteBufs(publisher)).then();
  }

  @Override
  protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
    return this.response.sendGroups(Flux.from(publisher).map(this::toByteBufs)).then();
  }

  @Override
  protected void applyHeaders() {
  }

  @Override
  protected void applyCookies() {
    // Netty Cookie doesn't support sameSite. When this is resolved, we can adapt to it again:
    // https://github.com/netty/netty/issues/8161
    for (List<ResponseCookie> cookies : getCookies().values()) {
      for (ResponseCookie cookie : cookies) {
        this.response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
      }
    }
  }

  @Override
  public Mono<Void> writeWith(Path file, long position, long count) {
    return doCommit(() -> this.response.sendFile(file, position, count).then());
  }

  private Publisher<Buffer> toByteBufs(Publisher<? extends DataBuffer> dataBuffers) {
    return dataBuffers instanceof Mono ?
           Mono.from(dataBuffers).map(Netty5DataBufferFactory::toBuffer) :
           Flux.from(dataBuffers).map(Netty5DataBufferFactory::toBuffer);
  }

  @Override
  protected void touchDataBuffer(DataBuffer buffer) {
    if (logger.isDebugEnabled()) {
      if (this.response instanceof ChannelOperationsId operationsId) {
        DataBufferUtils.touch(buffer, "Channel id: " + operationsId.asLongText());
      }
      else {
        this.response.withConnection(connection -> {
          ChannelId id = connection.channel().id();
          DataBufferUtils.touch(buffer, "Channel id: " + id.asShortText());
        });
      }
    }
  }

}
