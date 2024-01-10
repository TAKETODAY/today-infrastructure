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

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ServerHttpResponse} decorator for HTTP HEAD requests.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpHeadResponseDecorator extends ServerHttpResponseDecorator {

  public HttpHeadResponseDecorator(ServerHttpResponse delegate) {
    super(delegate);
  }

  /**
   * Consume and release the body without writing.
   * <p>If the headers contain neither Content-Length nor Transfer-Encoding,
   * and the body is a {@link Mono}, count the bytes and set Content-Length.
   */
  @Override
  public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    if (shouldSetContentLength() && body instanceof Mono) {
      return ((Mono<? extends DataBuffer>) body).doOnSuccess(buffer -> {
                if (buffer != null) {
                  getHeaders().setContentLength(buffer.readableByteCount());
                  DataBufferUtils.release(buffer);
                }
                else {
                  getHeaders().setContentLength(0);
                }
              })
              .then();
    }
    else {
      return Flux.from(body)
              .doOnNext(DataBufferUtils::release)
              .then();
    }
  }

  private boolean shouldSetContentLength() {
    HttpHeaders httpHeaders = getHeaders();
    return httpHeaders.getFirst(HttpHeaders.CONTENT_LENGTH) == null
            && httpHeaders.getFirst(HttpHeaders.TRANSFER_ENCODING) == null;
  }

  /**
   * Invoke {@link #setComplete()} without writing.
   * <p>RFC 7302 allows HTTP HEAD response without content-length and it's not
   * something that can be computed on a streaming response.
   */
  @Override
  public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    // Not feasible to count bytes on potentially streaming response.
    // RFC 7302 allows HEAD without content-length.
    return setComplete();
  }

}
