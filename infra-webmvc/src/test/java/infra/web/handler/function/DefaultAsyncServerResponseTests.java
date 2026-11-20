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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Arjen Poutsma
 */
class DefaultAsyncServerResponseTests {

  @Test
  @Timeout(5)
  void blockNotCompletedWithZeroTimeout() {
    ServerResponse wrappee = ServerResponse.ok().build();
    CompletableFuture<ServerResponse> future = CompletableFuture.supplyAsync(() -> wrappee,
            CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));
    AsyncServerResponse response = ServerResponse.async(future, Duration.ZERO);
    assertThat(response.block()).isSameAs(wrappee);
  }

  @ParameterizedTest
  @CsvSource({
          "PT-1S, Timeout must not be negative",
          "PT0.000000001S, Timeout must be zero or at least 1 millisecond",
          "PT9223372036854775807S, Timeout is too large"
  })
  void rejectsInvalidTimeout(Duration timeout, String message) {
    CompletableFuture<ServerResponse> pending = new CompletableFuture<>();
    CompletableFuture<ServerResponse> completed = CompletableFuture.completedFuture(ServerResponse.ok().build());

    assertThatIllegalArgumentException().isThrownBy(() -> ServerResponse.async(pending, timeout))
            .withMessage(message);
    assertThatIllegalArgumentException().isThrownBy(() -> ServerResponse.async(completed, timeout))
            .withMessage(message);
  }

  @Test
  void blockNotCompletedWithTimeoutExceeded() {
    CompletableFuture<ServerResponse> future = new CompletableFuture<>();
    AsyncServerResponse response = AsyncServerResponse.create(future, Duration.ofMillis(50));
    assertThatIllegalStateException().isThrownBy(response::block).withCauseInstanceOf(TimeoutException.class);
  }

  @Test
  void blockCompleted() {
    ServerResponse wrappee = ServerResponse.ok().build();
    CompletableFuture<ServerResponse> future = CompletableFuture.completedFuture(wrappee);
    AsyncServerResponse response = ServerResponse.async(future);

    assertThat(response.block()).isSameAs(wrappee);
  }

  @Test
  void blockNotCompleted() {
    ServerResponse wrappee = ServerResponse.ok().build();
    CompletableFuture<ServerResponse> future = CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(500);
        return wrappee;
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    });
    AsyncServerResponse response = AsyncServerResponse.create(future);

    assertThat(response.block()).isSameAs(wrappee);
  }

  @Test
  void createWithCompletableFuture() {
    ServerResponse expectedResponse = ServerResponse.ok().build();
    CompletableFuture<ServerResponse> future = CompletableFuture.completedFuture(expectedResponse);

    AsyncServerResponse asyncResponse = AsyncServerResponse.create(future);

    assertThat(asyncResponse.block()).isSameAs(expectedResponse);
  }

  @Test
  void createWithPublisher() {
    ServerResponse expectedResponse = ServerResponse.ok().build();
    Publisher<ServerResponse> publisher = Mono.just(expectedResponse);

    AsyncServerResponse asyncResponse = AsyncServerResponse.create(publisher);

    assertThat(asyncResponse.block()).isSameAs(expectedResponse);
  }

  @Test
  void createWithTimeout() {
    ServerResponse expectedResponse = ServerResponse.ok().build();
    CompletableFuture<ServerResponse> future = CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(100);
        return expectedResponse;
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    });

    AsyncServerResponse asyncResponse = AsyncServerResponse.create(future, Duration.ofSeconds(5));

    assertThat(asyncResponse.block()).isSameAs(expectedResponse);
  }

  @Test
  void createWithCompletedFutureReturnsCompletedAsyncResponse() {
    ServerResponse expectedResponse = ServerResponse.ok().build();
    CompletableFuture<ServerResponse> future = CompletableFuture.completedFuture(expectedResponse);

    AsyncServerResponse asyncResponse = AsyncServerResponse.create(future);

    assertThat(asyncResponse).isInstanceOf(CompletedAsyncServerResponse.class);
  }

  @Test
  void createWithUncompletedFutureReturnsDefaultAsyncResponse() {
    CompletableFuture<ServerResponse> future = new CompletableFuture<>();

    AsyncServerResponse asyncResponse = AsyncServerResponse.create(future);

    assertThat(asyncResponse).isInstanceOf(DefaultAsyncServerResponse.class);
  }

  @Test
  void createWithExceptionalFuture() {
    CompletableFuture<ServerResponse> future = CompletableFuture.failedFuture(new RuntimeException("test"));

    AsyncServerResponse asyncResponse = AsyncServerResponse.create(future);

    assertThat(asyncResponse).isInstanceOf(DefaultAsyncServerResponse.class);
  }

  @Test
  void createWithCancelledFuture() throws Exception {
    CompletableFuture<ServerResponse> future = new CompletableFuture<>();
    future.cancel(true);

    AsyncServerResponse asyncResponse = AsyncServerResponse.create(future);

    assertThat(asyncResponse).isInstanceOf(DefaultAsyncServerResponse.class);
  }

  @Test
  void blockWithTimeout() {
    ServerResponse expectedResponse = ServerResponse.ok().build();
    CompletableFuture<ServerResponse> future = CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(200);
        return expectedResponse;
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    });

    AsyncServerResponse asyncResponse = AsyncServerResponse.create(future, Duration.ofMillis(500));

    assertThat(asyncResponse.block()).isSameAs(expectedResponse);
  }

}
