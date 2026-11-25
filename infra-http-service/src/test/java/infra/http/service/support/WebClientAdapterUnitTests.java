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

package infra.http.service.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.http.HttpMethod;
import infra.http.ResponseEntity;
import infra.http.service.invoker.HttpRequestValues;
import infra.http.service.invoker.ReactiveHttpRequestValues;
import infra.web.reactive.client.ClientResponse;
import infra.web.reactive.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

class WebClientAdapterUnitTests {

  private static final URI URI_VALUE = URI.create("https://example.test/items");

  private static final Duration TIMEOUT = Duration.ofSeconds(2);

  private static final ParameterizedTypeReference<String> BODY_TYPE = new ParameterizedTypeReference<>() { };

  private final WebClient client = mock(WebClient.class);

  private final WebClient.RequestBodyUriSpec request = mock(WebClient.RequestBodyUriSpec.class);

  private final WebClientAdapter adapter = WebClientAdapter.forClient(this.client);

  private final HttpRequestValues values = HttpRequestValues.builder()
          .setHttpMethod(HttpMethod.GET).setURI(URI_VALUE).build();

  @BeforeEach
  void setUp() {
    given(this.client.method(HttpMethod.GET)).willReturn(this.request);
    given(this.request.uri(URI_VALUE)).willReturn(this.request);
  }

  @Test
  void retrievesHeadersAndBodilessEntity() {
    var response = mock(WebClient.ResponseSpec.class);
    ResponseEntity<Void> entity = ResponseEntity.noContent().header("X-Trace", "123").build();
    given(this.request.retrieve()).willReturn(response);
    given(response.toBodilessEntity()).willReturn(Mono.just(entity));

    assertThat(this.adapter.exchangeForHeadersMono(this.values).block(TIMEOUT).getFirst("X-Trace")).isEqualTo("123");
    assertThat(this.adapter.exchangeForBodilessEntityMono(this.values).block(TIMEOUT)).isSameAs(entity);
  }

  @Test
  void preservesStreamingResponseBody() {
    var response = mock(WebClient.ResponseSpec.class);
    var body = Flux.just("first", "second");
    var entity = ResponseEntity.ok(body);
    given(this.request.retrieve()).willReturn(response);
    given(response.bodyToFlux(BODY_TYPE)).willReturn(body);
    given(response.toEntityFlux(BODY_TYPE)).willReturn(Mono.just(entity));

    StepVerifier.create(this.adapter.exchangeForBodyFlux(this.values, BODY_TYPE))
            .expectNext("first", "second").expectComplete().verify(TIMEOUT);
    assertThat(this.adapter.exchangeForEntityFlux(this.values, BODY_TYPE).block(TIMEOUT)).isSameAs(entity);
  }

  @Test
  void adaptsReactiveBodyAndEmptyCompletionToFutures() {
    var response = mock(WebClient.ResponseSpec.class);
    given(this.request.retrieve()).willReturn(response);
    given(response.bodyToMono(BODY_TYPE)).willReturn(Mono.just("body"));
    given(response.toBodiless()).willReturn(Mono.empty());

    assertThat(this.adapter.exchangeAsyncBody(this.values, BODY_TYPE)).succeedsWithin(TIMEOUT).isEqualTo("body");
    assertThat(this.adapter.exchangeAsyncVoid(this.values)).succeedsWithin(TIMEOUT).isNull();
  }

  @Test
  void exposesRawReactiveResponse() throws Exception {
    var response = mock(ClientResponse.class);
    var result = Mono.just(response);
    given(this.request.exchange()).willReturn(result);
    var method = ReturnTypes.class.getMethod("response");
    var execution = this.adapter.createRequestExecution(method, new MethodParameter(method, -1), false);

    assertThat(execution).isNotNull();
    assertThat(execution.execute(this.values)).isSameAs(result);
  }

  @Test
  void forwardsExplicitApiVersionAndPublisherBody() {
    Publisher<List<String>> body = Flux.just(List.of("first"), List.of("second"));
    var elementType = new ParameterizedTypeReference<List<String>>() { };
    var builder = ReactiveHttpRequestValues.builder();
    builder.setHttpMethod(HttpMethod.GET).setURI(URI_VALUE).setApiVersion("2");
    builder.setBodyPublisher(body, elementType);

    assertThat(this.adapter.newRequest(builder.build())).isSameAs(this.request);
    then(this.request).should().apiVersion("2");
    then(this.request).should().body((Publisher<?>) body, elementType);
  }

  @Test
  void rejectsPublisherBodyWithoutElementType() {
    var builder = ReactiveHttpRequestValues.builder();
    builder.setHttpMethod(HttpMethod.GET).setURI(URI_VALUE);
    builder.setBodyPublisher(Flux.just("body"), null);

    assertThatIllegalArgumentException().isThrownBy(() -> this.adapter.newRequest(builder.build()))
            .withMessage("Publisher body element type is required");
  }

  @Test
  void rejectsRequestValuesWithoutUriOrTemplate() {
    var incomplete = mock(HttpRequestValues.class);
    given(incomplete.getHttpMethod()).willReturn(HttpMethod.GET);

    assertThatIllegalStateException().isThrownBy(() -> this.adapter.newRequest(incomplete))
            .withMessage("Neither full URL nor URI template");
  }

  private interface ReturnTypes {

    ClientResponse response();
  }

}
