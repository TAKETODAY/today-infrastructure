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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.util.Assert;
import infra.util.Feature;
import infra.util.concurrent.Future;
import infra.web.util.TimeoutUtils;

import static infra.util.FeatureDetector.isPresent;

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
   * Block until the future response is obtained.
   * <p>If this response was created with a positive timeout, this method waits
   * at most for that timeout with millisecond precision. With no timeout or
   * {@link Duration#ZERO}, it waits indefinitely.
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
   * @param asyncResponse a {@code CompletableFuture<ServerResponse>},
   * {@code Future<ServerResponse>} or {@code Publisher<ServerResponse>}
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
   * <p>{@code null} leaves the MVC async request timeout unchanged, and
   * {@link Duration#ZERO} uses the container's zero-timeout behavior.
   * Neither value sets a time limit for {@link #block()}.
   *
   * @param asyncResponse a {@code CompletableFuture<ServerResponse>},
   * {@code Future<ServerResponse>} or {@code Publisher<ServerResponse>}
   * @param timeout the timeout, or {@code null} to leave it unspecified
   * @return the asynchronous response
   * @throws IllegalArgumentException if the timeout is negative, positive but
   * less than one millisecond, or cannot be represented in milliseconds
   */
  static AsyncServerResponse create(Object asyncResponse, @Nullable Duration timeout) {
    Assert.notNull(asyncResponse, "AsyncResponse is required");
    TimeoutUtils.validate(timeout);

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
    if (obj instanceof Future<?> future) {
      return (CompletableFuture<ServerResponse>) future.toCompletableFuture();
    }
    else if (obj instanceof CompletableFuture<?> futureResponse) {
      return (CompletableFuture<ServerResponse>) futureResponse;
    }
    else if (isPresent(Feature.REACTIVE_STREAMS)) {
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
