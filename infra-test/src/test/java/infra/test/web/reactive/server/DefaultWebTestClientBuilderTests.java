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

package infra.test.web.reactive.server;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import infra.http.reactive.client.ClientHttpConnector;
import infra.web.reactive.client.ExchangeFilterFunction;
import infra.web.reactive.client.ExchangeStrategies;
import infra.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DefaultWebTestClientBuilderTests {

  private final ClientHttpConnector connector = new HttpHandlerConnector((request, response) -> response.setComplete());

  @Test
  void builderAppliesDefaultsFiltersAndExplicitUriFactory() {
    List<String> calls = new ArrayList<>();
    ExchangeFilterFunction first = (request, next) -> {
      calls.add("first");
      return next.exchange(request);
    };
    ExchangeFilterFunction second = (request, next) -> {
      calls.add("second");
      return next.exchange(request);
    };
    WebTestClient.Builder builder = new DefaultWebTestClientBuilder().clientConnector(connector)
            .baseUrl("https://ignored.example")
            .uriBuilderFactory(new DefaultUriBuilderFactory("https://actual.example"))
            .defaultHeader("X-Default", "one").defaultHeader("X-Remove", "old")
            .defaultHeader("X-Remove", (String[]) null)
            .defaultHeaders(headers -> headers.set("X-Other", "two"))
            .defaultCookie("first", "one").defaultCookies(cookies -> cookies.add("second", "two"))
            .filter(first).filters(filters -> filters.add(second));
    ExchangeResult result = builder.build().get().uri("/path").exchange().expectBody().isEmpty();
    assertThat(result.getUrl().toString()).isEqualTo("https://actual.example/path");
    assertThat(result.getRequestHeaders().getFirst("X-Default")).isEqualTo("one");
    assertThat(result.getRequestHeaders().getFirst("X-Other")).isEqualTo("two");
    assertThat(result.getRequestHeaders().getFirst("X-Remove")).isNull();
    assertThat(result.getRequestHeaders().get("Cookie")).containsExactly("first=one", "second=two");
    assertThat(calls).containsExactly("first", "second");
    calls.clear();
    builder.filters(List::clear);
    builder.build().get().uri("/").exchange().expectStatus().isOk();
    assertThat(calls).isEmpty();
  }

  @Test
  void consumersAndStrategyConfigurersAreRetainedByMutation() {
    AtomicInteger configured = new AtomicInteger();
    AtomicInteger consumed = new AtomicInteger();
    WebTestClient.Builder builder = new DefaultWebTestClientBuilder(connector)
            .exchangeStrategies(ExchangeStrategies.withDefaults())
            .exchangeStrategies(strategies -> configured.incrementAndGet())
            .codecs(codecs -> configured.incrementAndGet())
            .entityExchangeResultConsumer(result -> consumed.incrementAndGet())
            .entityExchangeResultConsumer(result -> consumed.incrementAndGet());
    WebTestClient client = builder.build();
    client.get().uri("/").exchange().expectBody().isEmpty();
    client.mutate().build().get().uri("/").exchange().expectBody().isEmpty();
    assertThat(configured).hasValue(4);
    assertThat(consumed).hasValue(4);
    new DefaultWebTestClientBuilder(connector).codecs(codecs -> configured.incrementAndGet())
            .exchangeStrategies(strategies -> configured.incrementAndGet()).build();
    assertThat(configured).hasValue(6);
  }

  @Test
  void configurerReceivesBuilderAndConnector() {
    DefaultWebTestClientBuilder builder = new DefaultWebTestClientBuilder(connector);
    builder.apply((actualBuilder, actualConnector) -> {
      assertThat(actualBuilder).isSameAs(builder);
      assertThat(actualConnector).isSameAs(connector);
      actualBuilder.defaultHeader("X-Configured", "yes");
    });
    ExchangeResult result = builder.build().get().uri("/").exchange().expectBody().isEmpty();
    assertThat(result.getRequestHeaders().getFirst("X-Configured")).isEqualTo("yes");
    assertThatIllegalArgumentException().isThrownBy(() -> builder.filter(null));
    assertThatIllegalArgumentException().isThrownBy(() -> builder.entityExchangeResultConsumer(null));
  }

  @Test
  void baseUrlAndDefaultStrategiesWorkWithCopies() {
    WebTestClient client = new DefaultWebTestClientBuilder(connector).baseUrl("https://example.org")
            .exchangeStrategies(ExchangeStrategies.withDefaults()).build();
    assertThat(client.mutate().build().get().uri("/test").exchange().returnResult(Void.class).getUrl().toString())
            .isEqualTo("https://example.org/test");
    assertThat(new DefaultWebTestClientBuilder().build()).isNotNull();
  }

}
