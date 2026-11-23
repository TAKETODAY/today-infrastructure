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
import java.util.concurrent.atomic.AtomicReference;

import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.reactive.client.ClientHttpConnector;
import infra.web.testfixture.http.client.reactive.MockClientHttpResponse;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeFunctionsBehaviorTests {

  @Test
  void exchangesRequestAndMapsResponse() {
    AtomicReference<HttpMethod> method = new AtomicReference<>();
    AtomicReference<URI> uri = new AtomicReference<>();
    var response = new MockClientHttpResponse(HttpStatus.ACCEPTED);
    ClientHttpConnector connector = (requestMethod, requestUri, callback) -> {
      method.set(requestMethod);
      uri.set(requestUri);
      return Mono.just(response);
    };
    ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("https://example.com/items")).build();

    ClientResponse result = ExchangeFunctions.create(connector, ExchangeStrategies.empty().build()).exchange(request).block();

    assertThat(result).isNotNull();
    assertThat(result.statusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(result.rawStatusCode()).isEqualTo(202);
    assertThat(method).hasValue(HttpMethod.POST);
    assertThat(uri).hasValue(URI.create("https://example.com/items"));
    assertThat(result.logPrefix()).isEqualTo(request.logPrefix() + "[" + response.getId() + "] ");
  }

  @Test
  void wrapsConnectorFailureWithRequestDetails() {
    RuntimeException failure = new RuntimeException("connection failed");
    ClientHttpConnector connector = (method, uri, callback) -> Mono.error(failure);
    ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("https://example.com/items"))
            .header("X-Request-Id", "test").build();

    assertThatThrownBy(() -> ExchangeFunctions.create(connector).exchange(request).block())
            .hasCause(failure)
            .isInstanceOfSatisfying(WebClientRequestException.class, error -> {
              assertThat(error.getMethod()).isEqualTo(HttpMethod.GET);
              assertThat(error.getUri()).isEqualTo(request.uri());
              assertThat(error.getHeaders().getFirst("X-Request-Id")).isEqualTo("test");
            });
  }

  @Test
  void rejectsNullConnectorAndStrategies() {
    assertThatThrownBy(() -> ExchangeFunctions.create(null))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ExchangeFunctions.create((method, uri, callback) -> Mono.empty(), null))
            .isInstanceOf(IllegalArgumentException.class);
  }

}
