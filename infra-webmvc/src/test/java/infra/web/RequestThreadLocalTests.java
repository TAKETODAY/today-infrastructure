/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web;

import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;

import infra.test.classpath.ClassPathExclusions;
import infra.util.InfraStrategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/4/30 21:43
 */
class RequestThreadLocalTests {

  @Test
  @ClassPathExclusions("netty-comm*")
  void nettyNotPresent() {
    RequestThreadLocal requestThreadLocal = RequestThreadLocal.lookup();
    assertThat(requestThreadLocal).isInstanceOf(RequestThreadLocal.Default.class);
  }

  @Test
  void lookupReturnsRegisteredStrategy() {
    RequestThreadLocal strategy = mock(RequestThreadLocal.class);
    try (var strategies = mockStatic(InfraStrategies.class)) {
      strategies.when(() -> InfraStrategies.findFirst(RequestThreadLocal.class, null)).thenReturn(strategy);

      assertThat(RequestThreadLocal.lookup()).isSameAs(strategy);
    }
  }

  @Test
  void defaultStorageIsThreadLocalAndCanBeCleared() throws Exception {
    RequestThreadLocal storage = new RequestThreadLocal.Default();
    ServerHttpExchange exchange = mock(ServerHttpExchange.class);
    assertThat(storage.get()).isNull();

    storage.set(exchange);
    try {
      FutureTask<ServerHttpExchange> otherThread = new FutureTask<>(storage::get);
      new Thread(otherThread).start();
      assertThat(otherThread.get()).isNull();
      assertThat(storage.get()).isSameAs(exchange);

      storage.set(null);
      assertThat(storage.get()).isNull();

      storage.set(exchange);
      storage.remove();
      assertThat(storage.get()).isNull();
    }
    finally {
      storage.remove();
    }
  }

}
