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

package infra.http.service.invoker;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapterRegistry;
import infra.http.HttpHeaders;
import infra.http.ResponseEntity;
import infra.util.concurrent.Future;
import infra.web.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:24
 */
class AbstractReactorHttpExchangeAdapterTests {

  @Test
  void shouldSetAndGetReactiveAdapterRegistry() {
    // given
    AbstractReactorHttpExchangeAdapter adapter = new TestReactorHttpExchangeAdapter();
    ReactiveAdapterRegistry registry = mock(ReactiveAdapterRegistry.class);

    // when
    adapter.setReactiveAdapterRegistry(registry);

    // then
    assertThat(adapter.getReactiveAdapterRegistry()).isEqualTo(registry);
  }

  @Test
  void shouldUseDefaultReactiveAdapterRegistry() {
    // given
    AbstractReactorHttpExchangeAdapter adapter = new TestReactorHttpExchangeAdapter();

    // when & then
    assertThat(adapter.getReactiveAdapterRegistry()).isEqualTo(ReactiveAdapterRegistry.getSharedInstance());
  }

  @Test
  void shouldSetAndGetBlockTimeout() {
    // given
    AbstractReactorHttpExchangeAdapter adapter = new TestReactorHttpExchangeAdapter();
    Duration timeout = Duration.ofSeconds(30);

    // when
    adapter.setBlockTimeout(timeout);

    // then
    assertThat(adapter.getBlockTimeout()).isEqualTo(timeout);
  }

  @Test
  void shouldReturnNullBlockTimeoutByDefault() {
    // given
    AbstractReactorHttpExchangeAdapter adapter = new TestReactorHttpExchangeAdapter();

    // when & then
    assertThat(adapter.getBlockTimeout()).isNull();
  }

  @Test
  void shouldSetBlockTimeoutToNull() {
    // given
    AbstractReactorHttpExchangeAdapter adapter = new TestReactorHttpExchangeAdapter();
    adapter.setBlockTimeout(Duration.ofSeconds(30));

    // when
    adapter.setBlockTimeout(null);

    // then
    assertThat(adapter.getBlockTimeout()).isNull();
  }

  static class TestReactorHttpExchangeAdapter extends AbstractReactorHttpExchangeAdapter {

    @Override
    public boolean supportsRequestAttributes() {
      return false;
    }

    @Override
    public ClientResponse exchange(HttpRequestValues requestValues) {
      return null;
    }

    @Override
    public Future<ClientResponse> exchangeAsync(HttpRequestValues requestValues) {
      return null;
    }

    @Override
    public <T> Future<T> exchangeAsyncBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyTypeRef) {
      return null;
    }

    @Override
    public Future<Void> exchangeAsyncVoid(HttpRequestValues requestValues) {
      return null;
    }

    @Override
    public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
      return null;
    }

    @Override
    public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
      return null;
    }

    @Override
    public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
      return null;
    }

    @Override
    public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
      return null;
    }

    @Override
    public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
      return null;
    }

    @Override
    public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
      return null;
    }

    @Override
    public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
      return null;
    }

    @Override
    public Mono<infra.web.reactive.client.ClientResponse> exchangeMono(HttpRequestValues requestValues) {
      return null;
    }
  }

}