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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.web.reactive.BodyInserters;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class WebTestClientRequestCoverageTests {

  private final WebTestClient client = WebTestClient.bindToServer(new HttpHandlerConnector((request, response) -> {
    response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    return response.writeWith(request.getBody());
  })).baseUrl("https://example.org").build();

  @Test
  void uriFactoriesAndAllMethodsAreSupported() {
    List<ExchangeResult> results = new ArrayList<>();
    results.add(client.head().uri("/head").exchange().expectBody().isEmpty());
    results.add(client.put().uri("/{name}", Map.of("name", "put")).exchange().expectBody().isEmpty());
    results.add(client.patch().uri(builder -> builder.path("/patch").build()).exchange().expectBody().isEmpty());
    results.add(client.delete().uri(URI.create("https://example.org/delete")).exchange().expectBody().isEmpty());
    results.add(client.options().uri("/options").exchange().expectBody().isEmpty());
    results.add(client.method(HttpMethod.GET).exchange().expectBody().isEmpty());
    assertThat(results).extracting(ExchangeResult::getMethod)
            .containsExactly(HttpMethod.HEAD, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.GET);
    assertThat(results).extracting(result -> result.getUrl().getPath())
            .containsExactly("/head", "/put", "/patch", "/delete", "/options", "");
  }

  @Test
  void requestMetadataIsPassedToExchangeFilter() {
    ZonedDateTime modified = ZonedDateTime.parse("2026-01-01T00:00:00Z");
    WebTestClient mutated = client.mutateWith((builder, connector) -> builder.filter((request, next) -> {
      assertThat(request.attributes()).containsEntry("first", "one").containsEntry("second", "two");
      assertThat(request.headers().getContentLength()).isEqualTo(4);
      assertThat(request.headers().getIfModifiedSince()).isEqualTo(modified.toInstant().toEpochMilli());
      assertThat(request.headers().getIfNoneMatch()).containsExactly("\"tag\"");
      assertThat(request.headers().getAcceptCharset()).containsExactly(StandardCharsets.UTF_8);
      assertThat(request.cookies()).containsEntry("first", List.of("one")).containsEntry("second", List.of("two"));
      return next.exchange(request);
    }));
    mutated.post().uri("/metadata").attribute("first", "one").attributes(attributes -> attributes.put("second", "two"))
            .cookie("first", "one").cookies(cookies -> cookies.add("second", "two"))
            .accept(MediaType.TEXT_PLAIN).acceptCharset(StandardCharsets.UTF_8)
            .ifModifiedSince(modified).ifNoneMatch("\"tag\"").contentType(MediaType.TEXT_PLAIN).contentLength(4)
            .headers(headers -> headers.set("X-Custom", "yes")).bodyValue("body")
            .exchange().expectBody(String.class).isEqualTo("body");
  }

  @Test
  void publisherProducerAndInserterBodiesAreWritten() {
    ParameterizedTypeReference<String> type = new ParameterizedTypeReference<>() { };
    client.post().uri("/").body(Mono.just("publisher"), String.class)
            .exchange().expectBody(String.class).isEqualTo("publisher");
    client.post().uri("/").body(Mono.just("typed publisher"), type)
            .exchange().expectBody(type).isEqualTo("typed publisher");
    client.post().uri("/").body((Object) Mono.just("producer"), String.class)
            .exchange().expectBody(String.class).value(body -> assertThat(body).isEqualTo("producer"));
    client.post().uri("/").body((Object) Mono.just("typed producer"), type)
            .exchange().expectBody(type).consumeWith(result -> assertThat(result.getResponseBody()).isEqualTo("typed producer"));
    client.post().uri("/").body(BodyInserters.fromValue("inserter"))
            .exchange().expectBody(String.class).isEqualTo("inserter");
  }

  @Test
  void typedStreamingResultsAndEmptyBodiesCanBeReleased() {
    ParameterizedTypeReference<String> type = new ParameterizedTypeReference<>() { };
    FluxExchangeResult<String> result = client.post().uri("/").bodyValue("stream").exchange().returnResult(type);
    assertThat(result.getResponseBody().collectList().block()).containsExactly("stream");
    FluxExchangeResult<Void> empty = client.get().uri("/").exchange()
            .returnResult(new ParameterizedTypeReference<Void>() { });
    assertThat(empty.getResponseBody().collectList().block()).isEmpty();
  }

  @Test
  void listBodyAssertionsHandleSuccessfulAndFailedMatches() {
    WebTestClient jsonClient = WebTestClient.bindToServer(new HttpHandlerConnector((request, response) -> {
      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      return response.writeWith(Mono.just(response.bufferFactory().wrap("[1,2,3]".getBytes(StandardCharsets.UTF_8))));
    })).baseUrl("https://example.org").build();
    WebTestClient.ListBodySpec<Integer> body = jsonClient.get().uri("/").exchange().expectBodyList(Integer.class);
    assertThat(body.hasSize(3).contains(1, 2).doesNotContain(4).returnResult().getResponseBody())
            .containsExactly(1, 2, 3);
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> body.hasSize(1));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> body.contains(4));
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> body.doesNotContain(2));
    jsonClient.get().uri("/").exchange().expectBodyList(new ParameterizedTypeReference<Integer>() { })
            .hasSize(3).contains(3);
    WebTestClient.ListBodySpec<String> empty = client.get().uri("/").exchange().expectBodyList(String.class);
    empty.hasSize(0).doesNotContain("missing");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> empty.contains("missing"));
  }

  @Test
  void emptyExchangeAndResponseConsumersPreserveFailures() {
    WebTestClient emptyExchange = client.mutate().filter((request, next) -> Mono.empty()).build();
    assertThatIllegalStateException().isThrownBy(() -> emptyExchange.get().exchange())
            .withMessage("No ClientResponse");
    WebTestClient.ResponseSpec response = client.get().exchange();
    response.expectAll();
    IllegalArgumentException failure = new IllegalArgumentException("consumer failed");
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> response.expectAll(result -> { throw failure; })).isSameAs(failure);
  }

  @Test
  void rawBodyAssertionsReportMalformedJsonAndXmlAndDecodeDeclaredCharset() {
    WebTestClient.BodyContentSpec body = client.post().bodyValue("not-json-or-xml").exchange().expectBody();
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> body.json("{}"))
            .withMessage("JSON parsing error");
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> body.xml("<root/>"))
            .withMessage("XML parsing error");
    WebTestClient utf16 = WebTestClient.bindToServer(new HttpHandlerConnector((request, response) -> {
      response.getHeaders().setContentType(new MediaType("application", "json", StandardCharsets.UTF_16BE));
      return response.writeWith(Mono.just(response.bufferFactory().wrap("{\"name\":\"中文\"}".getBytes(StandardCharsets.UTF_16BE))));
    })).build();
    utf16.get().exchange().expectBody().jsonPath("$.name").isEqualTo("中文");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> client.get().exchange().expectBody().json("{}"));
  }

}
