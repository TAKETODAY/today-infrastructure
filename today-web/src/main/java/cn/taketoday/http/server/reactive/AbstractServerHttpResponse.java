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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base class for {@link ServerHttpResponse} implementations.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractServerHttpResponse implements ServerHttpResponse {

  /**
   * COMMITTING -> COMMITTED is the period after doCommit is called but before
   * the response status and headers have been applied to the underlying
   * response during which time pre-commit actions can still make changes to
   * the response status and headers.
   */
  private enum State {
    NEW, COMMITTING, COMMIT_ACTION_FAILED, COMMITTED
  }

  @Nullable
  private Integer statusCode;

  @Nullable
  private HttpHeaders readOnlyHeaders;

  private final HttpHeaders headers;
  private final DataBufferFactory dataBufferFactory;
  private final MultiValueMap<String, ResponseCookie> cookies;

  private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

  private final CopyOnWriteArrayList<Supplier<? extends Mono<Void>>> commitActions = new CopyOnWriteArrayList<>();

  public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory) {
    this(dataBufferFactory, HttpHeaders.create());
  }

  public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory, HttpHeaders headers) {
    Assert.notNull(dataBufferFactory, "DataBufferFactory is required");
    Assert.notNull(headers, "HttpHeaders is required");
    this.headers = headers;
    this.dataBufferFactory = dataBufferFactory;
    this.cookies = MultiValueMap.forLinkedHashMap();
  }

  @Override
  public final DataBufferFactory bufferFactory() {
    return this.dataBufferFactory;
  }

  @Override
  public boolean setStatusCode(@Nullable HttpStatus status) {
    if (this.state.get() == State.COMMITTED) {
      return false;
    }
    else {
      this.statusCode = status != null ? status.value() : null;
      return true;
    }
  }

  @Override
  @Nullable
  public HttpStatusCode getStatusCode() {
    return this.statusCode != null ? HttpStatus.resolve(this.statusCode) : null;
  }

  @Override
  public boolean setRawStatusCode(@Nullable Integer statusCode) {
    if (this.state.get() == State.COMMITTED) {
      return false;
    }
    else {
      this.statusCode = statusCode;
      return true;
    }
  }

  @Override
  @Nullable
  public Integer getRawStatusCode() {
    return this.statusCode;
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.readOnlyHeaders != null) {
      return this.readOnlyHeaders;
    }
    else if (this.state.get() == State.COMMITTED) {
      this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
      return this.readOnlyHeaders;
    }
    else {
      return this.headers;
    }
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    return this.state.get() == State.COMMITTED
           ? MultiValueMap.forUnmodifiable(this.cookies) : this.cookies;
  }

  @Override
  public void addCookie(ResponseCookie cookie) {
    Assert.notNull(cookie, "ResponseCookie is required");

    if (this.state.get() == State.COMMITTED) {
      throw new IllegalStateException(
              "Can't add the cookie " + cookie + "because the HTTP response has already been committed");
    }
    else {
      getCookies().add(cookie.getName(), cookie);
    }
  }

  /**
   * Return the underlying server response.
   */
  public abstract <T> T getNativeResponse();

  @Override
  public void beforeCommit(Supplier<? extends Mono<Void>> action) {
    this.commitActions.add(action);
  }

  @Override
  public boolean isCommitted() {
    State state = this.state.get();
    return state != State.NEW && state != State.COMMIT_ACTION_FAILED;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    // For Mono we can avoid ChannelSendOperator and Reactor Netty is more optimized for Mono.
    // We must resolve value first however, for a chance to handle potential error.
    if (body instanceof Mono) {
      return ((Mono<? extends DataBuffer>) body).flatMap(buffer -> {
                touchDataBuffer(buffer);
                AtomicBoolean subscribed = new AtomicBoolean();
                return doCommit(() -> {
                  try {
                    return writeWithInternal(
                            Mono.fromCallable(() -> buffer)
                                    .doOnSubscribe(s -> subscribed.set(true))
                                    .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                    );
                  }
                  catch (Throwable ex) {
                    return Mono.error(ex);
                  }
                }).doOnError(ex -> DataBufferUtils.release(buffer)).doOnCancel(() -> {
                  if (!subscribed.get()) {
                    DataBufferUtils.release(buffer);
                  }
                });
              })
              .doOnError(t -> getHeaders().clearContentHeaders())
              .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
    }
    else {
      return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeWithInternal(inner)))
              .doOnError(t -> getHeaders().clearContentHeaders());
    }
  }

  @Override
  public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeAndFlushWithInternal(inner)))
            .doOnError(t -> getHeaders().clearContentHeaders());
  }

  @Override
  public Mono<Void> setComplete() {
    return !isCommitted() ? doCommit(null) : Mono.empty();
  }

  /**
   * A variant of {@link #doCommit(Supplier)} for a response without no body.
   *
   * @return a completion publisher
   */
  protected Mono<Void> doCommit() {
    return doCommit(null);
  }

  /**
   * Apply {@link #beforeCommit(Supplier) beforeCommit} actions, apply the
   * response status and headers/cookies, and write the response body.
   *
   * @param writeAction the action to write the response body (may be {@code null})
   * @return a completion publisher
   */
  protected Mono<Void> doCommit(@Nullable Supplier<? extends Mono<Void>> writeAction) {
    Flux<Void> allActions = Flux.empty();
    if (this.state.compareAndSet(State.NEW, State.COMMITTING)) {
      if (!this.commitActions.isEmpty()) {
        allActions = Flux.concat(Flux.fromIterable(this.commitActions).map(Supplier::get)).doOnError(ex -> {
          if (this.state.compareAndSet(State.COMMITTING, State.COMMIT_ACTION_FAILED)) {
            getHeaders().clearContentHeaders();
          }
        });
      }
    }
    else if (this.state.compareAndSet(State.COMMIT_ACTION_FAILED, State.COMMITTING)) {
      // Skip commit actions
    }
    else {
      return Mono.empty();
    }

    allActions = allActions.concatWith(Mono.fromRunnable(() -> {
      applyStatusCode();
      applyHeaders();
      applyCookies();
      this.state.set(State.COMMITTED);
    }));

    if (writeAction != null) {
      allActions = allActions.concatWith(writeAction.get());
    }

    return allActions.then();
  }

  /**
   * Write to the underlying the response.
   *
   * @param body the publisher to write with
   */
  protected abstract Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body);

  /**
   * Write to the underlying the response, and flush after each {@code Publisher<DataBuffer>}.
   *
   * @param body the publisher to write and flush with
   */
  protected abstract Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body);

  /**
   * Write the status code to the underlying response.
   * This method is called once only.
   */
  protected abstract void applyStatusCode();

  /**
   * Invoked when the response is getting committed allowing sub-classes to
   * make apply header values to the underlying response.
   * <p>Note that most sub-classes use an {@link HttpHeaders} instance that
   * wraps an adapter to the native response headers such that changes are
   * propagated to the underlying response on the go. That means this callback
   * is typically not used other than for specialized updates such as setting
   * the contentType or characterEncoding fields in a Servlet response.
   */
  protected abstract void applyHeaders();

  /**
   * Add cookies from {@link #getHeaders()} to the underlying response.
   * This method is called once only.
   */
  protected abstract void applyCookies();

  /**
   * Allow sub-classes to associate a hint with the data buffer if it is a
   * pooled buffer and supports leak tracking.
   *
   * @param buffer the buffer to attach a hint to
   */
  protected void touchDataBuffer(DataBuffer buffer) { }

}
