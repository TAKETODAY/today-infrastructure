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

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import reactor.core.publisher.Mono;

/**
 * Abstract base class for listener-based server responses, e.g. Servlet 3.1
 * and Undertow.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractListenerServerHttpResponse extends AbstractServerHttpResponse {

  private final AtomicBoolean writeCalled = new AtomicBoolean();

  public AbstractListenerServerHttpResponse(DataBufferFactory bufferFactory) {
    super(bufferFactory);
  }

  public AbstractListenerServerHttpResponse(DataBufferFactory bufferFactory, HttpHeaders headers) {
    super(bufferFactory, headers);
  }

  @Override
  protected final Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
    return writeAndFlushWithInternal(Mono.just(body));
  }

  @Override
  protected final Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    if (!this.writeCalled.compareAndSet(false, true)) {
      return Mono.error(new IllegalStateException(
              "writeWith() or writeAndFlushWith() has already been called"));
    }
    Processor<? super Publisher<? extends DataBuffer>, Void> processor = createBodyFlushProcessor();
    return Mono.from(subscriber -> {
      body.subscribe(processor);
      processor.subscribe(subscriber);
    });
  }

  /**
   * Abstract template method to create a {@code Processor<Publisher<DataBuffer>, Void>}
   * that will write the response body with flushes to the underlying output. Called from
   * {@link #writeAndFlushWithInternal(Publisher)}.
   */
  protected abstract Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor();

}
