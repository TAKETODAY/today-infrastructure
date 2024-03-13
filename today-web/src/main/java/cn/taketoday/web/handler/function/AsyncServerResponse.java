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

package cn.taketoday.web.handler.function;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ReactiveStreams;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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
    return create(asyncResponse, null);
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
  static AsyncServerResponse create(Object asyncResponse, @Nullable Duration timeout) {
    Assert.notNull(asyncResponse, "AsyncResponse is required");

    CompletableFuture<ServerResponse> futureResponse = toCompletableFuture(asyncResponse);
    if (futureResponse.isDone()
            && !futureResponse.isCancelled()
            && !futureResponse.isCompletedExceptionally()) {

      try {
        ServerResponse completedResponse = futureResponse.get();
        return new CompletedAsyncServerResponse(completedResponse);
      }
      catch (InterruptedException | ExecutionException ignored) {
        // fall through to use DefaultAsyncServerResponse
      }
    }
    return new DefaultAsyncServerResponse(futureResponse, timeout);
  }

  @SuppressWarnings("unchecked")
  private static CompletableFuture<ServerResponse> toCompletableFuture(Object obj) {
    if (obj instanceof CompletableFuture<?> futureResponse) {
      return (CompletableFuture<ServerResponse>) futureResponse;
    }
    else if (ReactiveStreams.isPresent) {
      ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
      ReactiveAdapter publisherAdapter = registry.getAdapter(obj.getClass());
      if (publisherAdapter != null) {
        Publisher<ServerResponse> publisher = publisherAdapter.toPublisher(obj);
        ReactiveAdapter futureAdapter = registry.getAdapter(CompletableFuture.class);
        if (futureAdapter != null) {
          return (CompletableFuture<ServerResponse>) futureAdapter.fromPublisher(publisher);
        }
      }
    }
    throw new IllegalArgumentException("Asynchronous type not supported: " + obj.getClass());
  }

}

