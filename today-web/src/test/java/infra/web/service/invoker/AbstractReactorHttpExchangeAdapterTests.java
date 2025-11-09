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

package infra.web.service.invoker;

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
    public Mono<infra.web.client.reactive.ClientResponse> exchangeMono(HttpRequestValues requestValues) {
      return null;
    }
  }

}