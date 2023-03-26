/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.http.client.reactive;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base class for {@link ClientHttpRequest} implementations.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

  /**
   * COMMITTING -> COMMITTED is the period after doCommit is called but before
   * the response status and headers have been applied to the underlying
   * response during which time pre-commit actions can still make changes to
   * the response status and headers.
   */
  private enum State {
    NEW, COMMITTING, COMMITTED
  }

  private final HttpHeaders headers;
  private final MultiValueMap<String, HttpCookie> cookies;
  private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
  private final ArrayList<Supplier<? extends Publisher<Void>>> commitActions = new ArrayList<>(4);

  @Nullable
  private HttpHeaders readOnlyHeaders;

  public AbstractClientHttpRequest() {
    this(HttpHeaders.create());
  }

  public AbstractClientHttpRequest(HttpHeaders headers) {
    Assert.notNull(headers, "HttpHeaders must not be null");
    this.headers = headers;
    this.cookies = MultiValueMap.fromLinkedHashMap();
  }

  @Override
  public HttpHeaders getHeaders() {
    if (this.readOnlyHeaders != null) {
      return this.readOnlyHeaders;
    }
    else if (State.COMMITTED.equals(this.state.get())) {
      this.readOnlyHeaders = initReadOnlyHeaders();
      return this.readOnlyHeaders;
    }
    else {
      return this.headers;
    }
  }

  /**
   * Initialize the read-only headers after the request is committed.
   * <p>By default, this method simply applies a read-only wrapper.
   * Subclasses can do the same for headers from the native request.
   */
  protected HttpHeaders initReadOnlyHeaders() {
    return HttpHeaders.readOnlyHttpHeaders(this.headers);
  }

  @Override
  public MultiValueMap<String, HttpCookie> getCookies() {
    if (State.COMMITTED.equals(this.state.get())) {
      return MultiValueMap.unmodifiable(this.cookies);
    }
    return this.cookies;
  }

  @Override
  public void beforeCommit(Supplier<? extends Mono<Void>> action) {
    Assert.notNull(action, "Action must not be null");
    this.commitActions.add(action);
  }

  @Override
  public boolean isCommitted() {
    return this.state.get() != State.NEW;
  }

  /**
   * A variant of {@link #doCommit(Supplier)} for a request without body.
   *
   * @return a completion publisher
   */
  protected Mono<Void> doCommit() {
    return doCommit(null);
  }

  /**
   * Apply {@link #beforeCommit(Supplier) beforeCommit} actions, apply the
   * request headers/cookies, and write the request body.
   *
   * @param writeAction the action to write the request body (may be {@code null})
   * @return a completion publisher
   */
  protected Mono<Void> doCommit(@Nullable Supplier<? extends Publisher<Void>> writeAction) {
    if (!this.state.compareAndSet(State.NEW, State.COMMITTING)) {
      return Mono.empty();
    }

    this.commitActions.add(() -> Mono.fromRunnable(() -> {
      applyHeaders();
      applyCookies();
      this.state.set(State.COMMITTED);
    }));

    if (writeAction != null) {
      this.commitActions.add(writeAction);
    }

    ArrayList<Publisher<Void>> actions = new ArrayList<>(commitActions.size());
    for (var commitAction : commitActions) {
      actions.add(commitAction.get());
    }
    return Flux.concat(actions).then();
  }

  /**
   * Apply header changes from {@link #getHeaders()} to the underlying request.
   * This method is called once only.
   */
  protected abstract void applyHeaders();

  /**
   * Add cookies from {@link #getHeaders()} to the underlying request.
   * This method is called once only.
   */
  protected abstract void applyCookies();

}
