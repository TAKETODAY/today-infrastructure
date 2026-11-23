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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.util.LinkedMultiValueMap;
import infra.web.reactive.BodyInserters;
import infra.web.testfixture.http.client.reactive.MockClientHttpRequest;
import infra.web.testfixture.http.client.reactive.MockClientHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DefaultWebClientBehaviorTests {

  @Test
  void mergesRequestMetadataBeforeWritingToConnector() {
    var captured = new AtomicReference<MockClientHttpRequest>();
    var client = WebClient.builder().baseURI("https://example.com")
            .defaultHeader("X-Override", "default")
            .defaultCookie("session", "default")
            .filter((request, next) -> {
              assertThat(request.attributes()).containsEntry("source", "test").containsEntry("attempt", 2);
              return next.exchange(request);
            })
            .clientConnector((method, uri, callback) -> {
              var request = new MockClientHttpRequest(method, uri);
              captured.set(request);
              return callback.apply(request).thenReturn(new MockClientHttpResponse(HttpStatus.OK));
            }).build();
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("X-Override", "request");
    var cookies = new LinkedMultiValueMap<String, String>();
    cookies.add("session", "request");
    ZonedDateTime modifiedSince = ZonedDateTime.parse("2026-01-10T00:00:00Z");

    client.put().uri("/items/{id}", Map.of("id", 7))
            .headers(headers).headers(value -> value.add("X-Request", "test"))
            .cookies(cookies).acceptCharset(StandardCharsets.UTF_8)
            .ifModifiedSince(modifiedSince).ifNoneMatch("\"v1\"")
            .attributes(Map.of("source", "test")).attributes(value -> value.put("attempt", 2))
            .contentLength(4).bodyValue("item").exchange().block();

    MockClientHttpRequest request = captured.get();
    assertThat(request.getMethod()).isEqualTo(HttpMethod.PUT);
    assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/items/7"));
    assertThat(request.getHeaders().get("X-Override")).containsExactly("request");
    assertThat(request.getHeaders().getFirst("X-Request")).isEqualTo("test");
    assertThat(request.getHeaders().getAcceptCharset()).containsExactly(StandardCharsets.UTF_8);
    assertThat(request.getHeaders().getIfModifiedSince()).isEqualTo(modifiedSince.toInstant().toEpochMilli());
    assertThat(request.getHeaders().getIfNoneMatch()).containsExactly("\"v1\"");
    assertThat(request.getHeaders().getContentLength()).isEqualTo(4);
    assertThat(request.getCookies().getFirst("session").getValue()).isEqualTo("request");
    assertThat(request.getBodyAsString().block()).isEqualTo("item");
  }

  @ParameterizedTest
  @MethodSource("bodyVariants")
  void serializesBodyVariants(Function<WebClient.RequestBodySpec, WebClient.RequestHeadersSpec<?>> body) {
    var captured = new AtomicReference<MockClientHttpRequest>();
    WebClient client = WebClient.builder().clientConnector((method, uri, callback) -> {
      var request = new MockClientHttpRequest(method, uri);
      captured.set(request);
      return callback.apply(request).thenReturn(new MockClientHttpResponse(HttpStatus.OK));
    }).build();

    body.apply(client.patch().uri("https://example.com/items").contentType(MediaType.APPLICATION_JSON)).exchange().block();

    assertThat(captured.get().getMethod()).isEqualTo(HttpMethod.PATCH);
    assertThat(captured.get().getBodyAsString().block()).isEqualTo("[\"item\"]");
  }

  static Stream<Function<WebClient.RequestBodySpec, WebClient.RequestHeadersSpec<?>>> bodyVariants() {
    var type = new ParameterizedTypeReference<List<String>>() { };
    return Stream.of(
            spec -> spec.bodyValue(List.of("item"), type),
            spec -> spec.body(Mono.just(List.of("item")), type),
            spec -> spec.body(CompletableFuture.completedFuture(List.of("item")), type),
            spec -> spec.body(CompletableFuture.completedFuture(new String[] { "item" }), String[].class),
            spec -> spec.body(BodyInserters.fromValue(List.of("item"))));
  }

  @Test
  void requestMethodShortcutsReachTheExchangeFunction() {
    var methods = new ArrayList<HttpMethod>();
    WebClient client = WebClient.builder().exchangeFunction(request -> {
      methods.add(request.method());
      return Mono.just(ClientResponse.create(HttpStatus.OK).build());
    }).build();

    for (var spec : List.<WebClient.RequestHeadersSpec<?>>of(client.head(), client.delete(), client.options(),
            client.method(HttpMethod.TRACE))) {
      spec.exchange().block();
    }

    assertThat(methods).containsExactly(HttpMethod.HEAD, HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.TRACE);
  }

  @Test
  void exchangeToMonoReleasesResponseAfterValueIsConsumed() {
    ClientResponse response = response();
    WebClient client = client(response);

    String value = client.get().uri("/items")
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class).defaultIfEmpty("ok"))
            .block();

    assertThat(value).isEqualTo("ok");
    verify(response).releaseBody();
  }

  @Test
  void exchangeToMonoReleasesResponseWhenHandlerFails() {
    ClientResponse response = response();
    WebClient client = client(response);
    RuntimeException failure = new RuntimeException("handler failed");

    assertThatThrownBy(() -> client.get().uri("/items")
            .exchangeToMono(clientResponse -> Mono.error(failure)).block())
            .isSameAs(failure);
    verify(response).releaseBody();
  }

  @Test
  void exchangeToFluxAppendsResponseReleaseAfterValues() {
    ClientResponse response = response();
    WebClient client = client(response);

    assertThat(client.get().uri("/items")
            .exchangeToFlux(clientResponse -> Flux.just("one", "two"))
            .collectList().block()).containsExactly("one", "two");
    verify(response).releaseBody();
  }

  @Test
  void exchangeFailsWhenConnectorCompletesWithoutResponse() {
    WebClient client = WebClient.builder()
            .exchangeFunction(request -> Mono.empty())
            .build();

    assertThatThrownBy(() -> client.get().uri("/items").exchange().block())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("without emitting a response");
  }

  @Test
  void appliesCustomStatusAndRawStatusHandlersBeforeDefaultHandler() {
    ClientResponse response = response();
    given(response.statusCode()).willReturn(HttpStatus.NOT_FOUND);
    RuntimeException custom = new RuntimeException("custom");
    WebClient client = client(response);

    assertThatThrownBy(() -> client.get().uri("/items?key=value")
            .retrieve()
            .onRawStatus(code -> code == 404, ignored -> Mono.error(custom))
            .bodyToMono(String.class).block())
            .isSameAs(custom)
            .satisfies(error -> assertThat(error.getSuppressed())
                    .anySatisfy(trace -> assertThat(trace.getMessage())
                            .contains("GET https://example.com/items [DefaultWebClient]").doesNotContain("key=value")));
    verify(response).releaseBody();
  }

  private WebClient client(ClientResponse response) {
    return WebClient.builder()
            .baseURI("https://example.com")
            .exchangeFunction(request -> Mono.just(response))
            .build();
  }

  private ClientResponse response() {
    ClientResponse response = mock(ClientResponse.class);
    given(response.statusCode()).willReturn(HttpStatus.OK);
    given(response.rawStatusCode()).willReturn(200);
    given(response.releaseBody()).willReturn(Mono.empty());
    given(response.bodyToMono(String.class)).willReturn(Mono.empty());
    given(response.bodyToFlux(String.class)).willReturn(Flux.empty());
    given(response.createException()).willReturn(Mono.just(new WebClientResponseException(
            HttpStatus.NOT_FOUND, "not found", null, null, null, null)));
    return response;
  }

  @Test
  void releasesResponseAfterEmptyMono() {
    ClientResponse response = response();
    StepVerifier.create(client(response).get().exchangeToMono(ignored -> Mono.empty())).verifyComplete();
    verify(response).releaseBody();
  }

  @Test
  void syncHandlerFailureSurvivesBodyReleaseFailure() {
    ClientResponse monoResponse = response();
    ClientResponse fluxResponse = response();
    IllegalStateException failure = new IllegalStateException("handler failed");
    given(monoResponse.releaseBody()).willReturn(Mono.error(new IllegalStateException("release failed")));
    given(fluxResponse.releaseBody()).willReturn(Mono.error(new IllegalStateException("release failed")));

    StepVerifier.create(client(monoResponse).get().exchangeToMono(ignored -> { throw failure; }))
            .expectErrorMatches(error -> error == failure).verify();
    StepVerifier.create(client(fluxResponse).get().exchangeToFlux(ignored -> { throw failure; }))
            .expectErrorMatches(error -> error == failure).verify();
    verify(monoResponse).releaseBody();
    verify(fluxResponse).releaseBody();
  }

  @Test
  void exchangeToFluxPreservesValuesBeforeHandlerFailureAndReleasesResponse() {
    ClientResponse response = response();
    IllegalStateException failure = new IllegalStateException("handler failed");

    StepVerifier.create(client(response).get().exchangeToFlux(ignored -> Flux.just("first").concatWith(Mono.error(failure))))
            .expectNext("first").expectErrorMatches(error -> error == failure).verify();
    verify(response).releaseBody();
  }

  @Test
  void emptyStatusHandlerAllowsReadingParameterizedFluxBody() {
    ClientResponse response = response();
    var type = new ParameterizedTypeReference<List<String>>() { };
    given(response.statusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(response.bodyToFlux(type)).willReturn(Flux.just(List.of("item")));

    StepVerifier.create(client(response).get().retrieve()
            .onStatus(status -> status == HttpStatus.NOT_FOUND, ignored -> Mono.empty())
            .bodyToFlux(type))
            .expectNext(List.of("item")).verifyComplete();
    verify(response, never()).createException();
    verify(response, never()).releaseBody();
  }

}
