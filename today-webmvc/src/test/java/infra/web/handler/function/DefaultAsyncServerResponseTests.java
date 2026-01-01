/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.function;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DefaultAsyncServerResponseTests {

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
