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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DefaultAsyncServerResponseTests {

  @Test
  void blockCompleted() {
    ServerResponse wrappee = ServerResponse.ok().build();
    CompletableFuture<ServerResponse> future = CompletableFuture.completedFuture(wrappee);
    AsyncServerResponse response = AsyncServerResponse.create(future);

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

}
