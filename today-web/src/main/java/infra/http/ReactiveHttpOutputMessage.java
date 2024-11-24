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

package infra.http;

import org.reactivestreams.Publisher;

import java.util.function.Supplier;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import reactor.core.publisher.Mono;

/**
 * A "reactive" HTTP output message that accepts output as a {@link Publisher}.
 *
 * <p>Typically implemented by an HTTP request on the client-side or an
 * HTTP response on the server-side.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ReactiveHttpOutputMessage extends HttpMessage {

  /**
   * Return a {@link DataBufferFactory} that can be used to create the body.
   *
   * @return a buffer factory
   * @see #writeWith(Publisher)
   */
  DataBufferFactory bufferFactory();

  /**
   * Register an action to apply just before the HttpOutputMessage is committed.
   * <p><strong>Note:</strong> the supplied action must be properly deferred,
   * e.g. via {@link Mono#defer} or {@link Mono#fromRunnable}, to ensure it's
   * executed in the right order, relative to other actions.
   *
   * @param action the action to apply
   */
  void beforeCommit(Supplier<? extends Mono<Void>> action);

  /**
   * Whether the HttpOutputMessage is committed.
   */
  boolean isCommitted();

  /**
   * Use the given {@link Publisher} to write the body of the message to the
   * underlying HTTP layer.
   *
   * @param body the body content publisher
   * @return a {@link Mono} that indicates completion or error
   */
  Mono<Void> writeWith(Publisher<? extends DataBuffer> body);

  /**
   * Use the given {@link Publisher} of {@code Publishers} to write the body
   * of the HttpOutputMessage to the underlying HTTP layer, flushing after
   * each {@code Publisher<DataBuffer>}.
   *
   * @param body the body content publisher
   * @return a {@link Mono} that indicates completion or error
   */
  Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body);

  /**
   * Indicate that message handling is complete, allowing for any cleanup or
   * end-of-processing tasks to be performed such as applying header changes
   * made via {@link #getHeaders()} to the underlying HTTP message (if not
   * applied already).
   * <p>This method should be automatically invoked at the end of message
   * processing so typically applications should not have to invoke it.
   * If invoked multiple times it should have no side effects.
   *
   * @return a {@link Mono} that indicates completion or error
   */
  Mono<Void> setComplete();

}
