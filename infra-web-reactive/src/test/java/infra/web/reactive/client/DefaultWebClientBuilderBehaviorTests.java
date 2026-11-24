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

package infra.web.reactive.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.web.testfixture.http.client.reactive.MockClientHttpResponse;
import infra.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultWebClientBuilderBehaviorTests {

  @Test
  void copiesBuilderStateWithoutSharingMutableHeadersCookiesAndVariables() {
    List<ClientRequest> requests = new ArrayList<>();
    Map<String, String> variables = new HashMap<>(Map.of("id", "original"));
    HttpHeaders defaults = HttpHeaders.forWritable();
    defaults.set("X-Test", "one");
    WebClient.Builder original = WebClient.builder()
            .baseURI(URI.create("https://example.com"))
            .defaultUriVariables(variables)
            .defaultHeaders(defaults)
            .defaultCookie("session", "one")
            .exchangeFunction(request -> {
              requests.add(request);
              return Mono.just(ClientResponse.create(HttpStatus.OK).build());
            });

    WebClient.Builder copy = original.clone()
            .defaultHeaders(headers -> headers.set("X-Test", "two"))
            .defaultCookies(cookies -> cookies.set("session", "two"));

    variables.put("id", "updated");
    original.build().get().uri("/items/{id}", Map.of()).exchange().block();
    copy.build().get().uri("/items/{id}", Map.of()).exchange().block();

    assertThat(requests).extracting(ClientRequest::uri).containsExactly(
            URI.create("https://example.com/items/updated"), URI.create("https://example.com/items/original"));
    assertThat(requests).extracting(request -> request.headers().getFirst("X-Test")).containsExactly("one", "two");
    assertThat(requests).extracting(request -> request.cookies().getFirst("session")).containsExactly("one", "two");
  }

  @Test
  void appliesMultipleStrategyConfigurersAndCustomUriBuilderFactory() {
    ExchangeStrategies strategies = ExchangeStrategies.empty().build();
    DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory("https://example.com");
    List<String> configurers = new ArrayList<>();
    WebClient client = WebClient.builder()
            .exchangeStrategies(strategies)
            .exchangeStrategies(builder -> configurers.add("strategies"))
            .codecs(codecs -> {
              configurers.add("codecs");
              codecs.registerDefaults(true);
            })
            .uriBuilderFactory(uriFactory)
            .clientConnector((method, uri, callback) -> {
              assertThat(uri).isEqualTo(URI.create("https://example.com/items"));
              MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
              response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
              response.setBody("decoded");
              return Mono.just(response);
            })
            .build();

    assertThat(client.get().uri("/items").retrieve().bodyToMono(String.class).block()).isEqualTo("decoded");
    assertThat(configurers).containsExactly("strategies", "codecs");
    assertThat(strategies.messageReaders()).isEmpty();
  }

  @Test
  void acceptsCustomFiltersAndAppliesDefaultRequestInOrder() {
    HttpHeaders seen = HttpHeaders.forWritable();
    WebClient client = WebClient.builder()
            .defaultRequest(spec -> spec.header("X-Default", "one"))
            .defaultRequest(spec -> spec.header("X-Default", "two"))
            .filters(filters -> filters.add((request, next) -> {
              seen.addAll(request.headers());
              return next.exchange(request);
            }))
            .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK).build()))
            .build();

    client.get().uri("https://example.com/items").exchange().block();

    assertThat(seen.get("X-Default")).containsExactly("two");
  }

}
