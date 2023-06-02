/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.handler.function;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import cn.taketoday.core.ReactiveAdapterRegistry;

/**
 * Asynchronous subtype of {@link ServerResponse} that exposes the future
 * response.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ServerResponse#async(Object)
 * @since 4.0
 */
public interface AsyncServerResponse extends ServerResponse {

  /**
   * Blocks indefinitely until the future response is obtained.
   */
  ServerResponse block();

  // Static creation methods

  /**
   * Create a {@code AsyncServerResponse} with the given asynchronous response.
   * Parameter {@code asyncResponse} can be a
   * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} or
   * {@link Publisher Publisher&lt;ServerResponse&gt;} (or any
   * asynchronous producer of a single {@code ServerResponse} that can be
   * adapted via the {@link ReactiveAdapterRegistry}).
   *
   * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
   * {@code Publisher<ServerResponse>}
   * @return the asynchronous response
   */
  static AsyncServerResponse create(Object asyncResponse) {
    return DefaultAsyncServerResponse.create(asyncResponse, null);
  }

  /**
   * Create a (built) response with the given asynchronous response.
   * Parameter {@code asyncResponse} can be a
   * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} or
   * {@link Publisher Publisher&lt;ServerResponse&gt;} (or any
   * asynchronous producer of a single {@code ServerResponse} that can be
   * adapted via the {@link ReactiveAdapterRegistry}).
   *
   * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
   * {@code Publisher<ServerResponse>}
   * @param timeout maximum time period to wait for before timing out
   * @return the asynchronous response
   */
  static AsyncServerResponse create(Object asyncResponse, Duration timeout) {
    return DefaultAsyncServerResponse.create(asyncResponse, timeout);
  }

}

