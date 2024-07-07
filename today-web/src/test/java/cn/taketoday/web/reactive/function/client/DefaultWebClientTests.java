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

package cn.taketoday.web.reactive.function.client;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.ClientCodecConfigurer;
import cn.taketoday.web.reactive.function.BodyExtractors;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link DefaultWebClient}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultWebClientTests {

  @Mock
  private ExchangeFunction exchangeFunction;

  @Captor
  private ArgumentCaptor<ClientRequest> captor;

  private WebClient.Builder builder;

  @BeforeEach
  public void setup() {
    ClientResponse mockResponse = mock(ClientResponse.class);
    when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);
    when(mockResponse.bodyToMono(Void.class)).thenReturn(Mono.empty());
    given(this.exchangeFunction.exchange(this.captor.capture())).willReturn(Mono.just(mockResponse));
    this.builder = WebClient.builder().baseURI("/base").exchangeFunction(this.exchangeFunction);
  }

  @Test
  public void basic() {
    this.builder.build().get().uri("/path")
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.uri().toString()).isEqualTo("/base/path");
    assertThat(request.headers()).isEqualTo(HttpHeaders.forWritable());
    assertThat(request.cookies()).isEqualTo(Collections.emptyMap());
  }

  @Test
  public void uriBuilder() {
    this.builder.build().get()
            .uri(builder -> builder.path("/path").queryParam("q", "12").build())
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.uri().toString()).isEqualTo("/base/path?q=12");
  }

  @Test // gh-22705
  public void uriBuilderWithUriTemplate() {
    this.builder.build().get()
            .uri("/path/{id}", builder -> builder.queryParam("q", "12").build("identifier"))
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.uri().toString()).isEqualTo("/base/path/identifier?q=12");
    assertThat(request.attribute(WebClient.class.getName() + ".uriTemplate").get()).isEqualTo("/path/{id}");
  }

  @Test
  public void uriBuilderWithPathOverride() {
    this.builder.build().get()
            .uri(builder -> builder.replacePath("/path").build())
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.uri().toString()).isEqualTo("/path");
  }

  @Test
  public void requestHeaderAndCookie() {
    this.builder.build().get().uri("/path").accept(MediaType.APPLICATION_JSON)
            .cookies(cookies -> cookies.add("id", "123"))  // SPR-16178
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.headers().getFirst("Accept")).isEqualTo("application/json");
    assertThat(request.cookies().getFirst("id")).isEqualTo("123");
  }

  @Test
  public void contextFromThreadLocal() {
    WebClient client = this.builder
            .filter((request, next) ->
                    // Async, continue on different thread
                    Mono.delay(Duration.ofMillis(10)).then(next.exchange(request)))
            .filter((request, next) ->
                    Mono.deferContextual(contextView -> {
                      String fooValue = contextView.get("foo");
                      return next.exchange(ClientRequest.from(request).header("foo", fooValue).build());
                    }))
            .build();

    ThreadLocal<String> fooHolder = new ThreadLocal<>();
    fooHolder.set("bar");
    try {
      client.get().uri("/path")
              .context(context -> context.put("foo", fooHolder.get()))
              .retrieve()
              .bodyToMono(Void.class)
              .block(Duration.ofSeconds(10));
    }
    finally {
      fooHolder.remove();
    }

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.headers().getFirst("foo")).isEqualTo("bar");
  }

  @Test
  public void httpRequest() {
    this.builder.build().get().uri("/path")
            .httpRequest(httpRequest -> { })
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.httpRequest()).isNotNull();
  }

  @Test
  public void defaultHeaderAndCookie() {
    WebClient client = this.builder
            .defaultHeader("Accept", "application/json")
            .defaultCookie("id", "123")
            .build();

    client.get().uri("/path")
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.headers().getFirst("Accept")).isEqualTo("application/json");
    assertThat(request.cookies().getFirst("id")).isEqualTo("123");
  }

  @Test
  public void defaultHeaderAndCookieOverrides() {
    WebClient client = this.builder
            .defaultHeader("Accept", "application/json")
            .defaultCookie("id", "123")
            .build();

    client.get().uri("/path")
            .header("Accept", "application/xml")
            .cookie("id", "456")
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.headers().getFirst("Accept")).isEqualTo("application/xml");
    assertThat(request.cookies().getFirst("id")).isEqualTo("456");
  }

  @Test
  public void defaultHeaderAndCookieCopies() {
    WebClient client1 = this.builder
            .defaultHeader("Accept", "application/json")
            .defaultCookie("id", "123")
            .build();
    WebClient client2 = this.builder
            .defaultHeader("Accept", "application/xml")
            .defaultCookies(cookies -> cookies.set("id", "456"))
            .build();

    client1.get().uri("/path")
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.headers().getFirst("Accept")).isEqualTo("application/json");
    assertThat(request.cookies().getFirst("id")).isEqualTo("123");

    client2.get().uri("/path")
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    request = verifyAndGetRequest();
    assertThat(request.headers().getFirst("Accept")).isEqualTo("application/xml");
    assertThat(request.cookies().getFirst("id")).isEqualTo("456");
  }

  @Test
  public void defaultRequest() {
    ThreadLocal<String> context = new NamedThreadLocal<>("foo");

    Map<String, Object> actual = new HashMap<>();
    ExchangeFilterFunction filter = (request, next) -> {
      actual.putAll(request.attributes());
      return next.exchange(request);
    };

    WebClient client = this.builder
            .defaultRequest(spec -> spec.attribute("foo", context.get()))
            .filter(filter)
            .build();

    try {
      context.set("bar");
      client.get().uri("/path").attribute("foo", "bar")
              .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));
    }
    finally {
      context.remove();
    }

    assertThat(actual.get("foo")).isEqualTo("bar");
  }

  @Test
  public void bodyObjectPublisher() {
    Mono<Void> mono = Mono.empty();
    WebClient client = this.builder.build();

    assertThatIllegalArgumentException().isThrownBy(() ->
            client.post().uri("https://example.com").bodyValue(mono));
  }

  @Test
  public void mutateDoesCopy() {
    // First, build the clients

    WebClient.Builder builder = WebClient.builder()
            .filter((request, next) -> next.exchange(request))
            .defaultHeader("foo", "bar")
            .defaultCookie("foo", "bar");

    WebClient client1 = builder.build();

    WebClient client2 = builder.filter((request, next) -> next.exchange(request))
            .defaultHeader("baz", "qux")
            .defaultCookie("baz", "qux")
            .build();

    WebClient client1a = client1.mutate()
            .filter((request, next) -> next.exchange(request))
            .defaultHeader("baz", "qux")
            .defaultCookie("baz", "qux")
            .build();

    // Now, verify what each client has..

    WebClient.Builder builder1 = client1.mutate();
    builder1.filters(filters -> Assertions.assertThat(filters.size()).isEqualTo(1));
    builder1.defaultHeaders(headers -> Assertions.assertThat(headers.size()).isEqualTo(1));
    builder1.defaultCookies(cookies -> Assertions.assertThat(cookies.size()).isEqualTo(1));

    WebClient.Builder builder2 = client2.mutate();
    builder2.filters(filters -> Assertions.assertThat(filters.size()).isEqualTo(2));
    builder2.defaultHeaders(headers -> Assertions.assertThat(headers.size()).isEqualTo(2));
    builder2.defaultCookies(cookies -> Assertions.assertThat(cookies.size()).isEqualTo(2));

    WebClient.Builder builder1a = client1a.mutate();
    builder1a.filters(filters -> Assertions.assertThat(filters.size()).isEqualTo(2));
    builder1a.defaultHeaders(headers -> Assertions.assertThat(headers.size()).isEqualTo(2));
    builder1a.defaultCookies(cookies -> Assertions.assertThat(cookies.size()).isEqualTo(2));
  }

  @Test
  void cloneBuilder() {
    Consumer<ClientCodecConfigurer> codecsConfig = c -> { };
    ExchangeFunction exchangeFunction = request -> Mono.empty();
    WebClient.Builder builder = WebClient.builder().baseURI("https://example.org")
            .exchangeFunction(exchangeFunction)
            .filter((request, next) -> Mono.empty())
            .codecs(codecsConfig);

    WebClient.Builder clonedBuilder = builder.clone();

    assertThat(clonedBuilder).extracting("baseUrl").isEqualTo("https://example.org");
    assertThat(clonedBuilder).extracting("filters").isNotNull();
    assertThat(clonedBuilder).extracting("strategiesConfigurers").isNotNull();
    assertThat(clonedBuilder).extracting("exchangeFunction").isEqualTo(exchangeFunction);
  }

  @Test
  public void withStringAttribute() {
    Map<String, Object> actual = new HashMap<>();
    ExchangeFilterFunction filter = (request, next) -> {
      actual.putAll(request.attributes());
      return next.exchange(request);
    };

    this.builder.filter(filter).build()
            .get().uri("/path")
            .attribute("foo", "bar")
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    assertThat(actual.get("foo")).isEqualTo("bar");

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.attribute("foo").get()).isEqualTo("bar");
  }

  @Test
  public void withNullAttribute() {
    Map<String, Object> actual = new HashMap<>();
    ExchangeFilterFunction filter = (request, next) -> {
      actual.putAll(request.attributes());
      return next.exchange(request);
    };

    this.builder.filter(filter).build()
            .get().uri("/path")
            .attribute("foo", null)
            .retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    assertThat(actual.get("foo")).isNull();

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.attribute("foo").isPresent()).isFalse();
  }

  @Test
  public void apply() {
    WebClient client = this.builder
            .apply(builder -> builder
                    .defaultHeader("Accept", "application/json")
                    .defaultCookie("id", "123"))
            .build();

    client.get().uri("/path").retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.headers().getFirst("Accept")).isEqualTo("application/json");
    assertThat(request.cookies().getFirst("id")).isEqualTo("123");
  }

  @Test
  public void switchToErrorOnEmptyClientResponseMono() {
    ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
    given(exchangeFunction.exchange(any())).willReturn(Mono.empty());
    WebClient client = WebClient.builder().baseURI("/base").exchangeFunction(exchangeFunction).build();
    StepVerifier.create(client.get().uri("/path").retrieve().bodyToMono(Void.class))
            .expectErrorMessage("The underlying HTTP client completed without emitting a response.")
            .verify(Duration.ofSeconds(5));
  }

  @Test
  public void shouldApplyFiltersAtSubscription() {
    WebClient client = this.builder
            .filter((request, next) ->
                    next.exchange(ClientRequest
                            .from(request)
                            .header("Custom", "value")
                            .build())
            )
            .build();

    Mono<Void> result = client.get().uri("/path").retrieve().bodyToMono(Void.class);

    verifyNoInteractions(this.exchangeFunction);
    result.block(Duration.ofSeconds(10));
    ClientRequest request = verifyAndGetRequest();
    assertThat(request.headers().getFirst("Custom")).isEqualTo("value");
  }

  @Test // gh-23880
  public void onStatusHandlersOrderIsPreserved() {

    ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
    given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

    Mono<Void> result = this.builder.build().get()
            .uri("/path")
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("1")))
            .onStatus(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("2")))
            .bodyToMono(Void.class);

    StepVerifier.create(result).expectErrorMessage("1").verify();
  }

  @Test
  public void onStatusHandlerRegisteredGlobally() {

    ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
    given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

    Mono<Void> result = this.builder
            .defaultStatusHandler(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("1")))
            .defaultStatusHandler(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("2")))
            .build().get()
            .uri("/path")
            .retrieve()
            .bodyToMono(Void.class);

    StepVerifier.create(result).expectErrorMessage("1").verify();
  }

  @Test
  public void onStatusHandlerRegisteredGloballyHaveLowerPrecedence() {

    ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
    given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

    Mono<Void> result = this.builder
            .defaultStatusHandler(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("1")))
            .build().get()
            .uri("/path")
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, resp -> Mono.error(new IllegalStateException("2")))
            .bodyToMono(Void.class);

    StepVerifier.create(result).expectErrorMessage("2").verify();
  }

  @Test // gh-23880
  @SuppressWarnings("unchecked")
  public void onStatusHandlersDefaultHandlerIsLast() {

    ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
    given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

    Predicate<HttpStatusCode> predicate1 = mock(Predicate.class);
    Predicate<HttpStatusCode> predicate2 = mock(Predicate.class);

    given(predicate1.test(HttpStatus.BAD_REQUEST)).willReturn(false);
    given(predicate2.test(HttpStatus.BAD_REQUEST)).willReturn(false);

    Mono<Void> result = this.builder.build().get()
            .uri("/path")
            .retrieve()
            .onStatus(predicate1, resp -> Mono.error(new IllegalStateException()))
            .onStatus(predicate2, resp -> Mono.error(new IllegalStateException()))
            .bodyToMono(Void.class);

    StepVerifier.create(result).expectError(WebClientResponseException.class).verify();

    verify(predicate1).test(HttpStatus.BAD_REQUEST);
    verify(predicate2).test(HttpStatus.BAD_REQUEST);
  }

  @Test // gh-26069
  public void onStatusHandlersApplyForToEntityMethods() {

    ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST).build();
    given(exchangeFunction.exchange(any())).willReturn(Mono.just(response));

    WebClient.ResponseSpec spec = this.builder.build().get().uri("/path").retrieve();

    testStatusHandlerForToEntity(spec.toEntity(String.class));
    testStatusHandlerForToEntity(spec.toEntity(new ParameterizedTypeReference<String>() { }));
    testStatusHandlerForToEntity(spec.toEntityList(String.class));
    testStatusHandlerForToEntity(spec.toEntityList(new ParameterizedTypeReference<String>() { }));
    testStatusHandlerForToEntity(spec.toEntityFlux(String.class));
    testStatusHandlerForToEntity(spec.toEntityFlux(new ParameterizedTypeReference<String>() { }));
    testStatusHandlerForToEntity(spec.toEntityFlux(BodyExtractors.toFlux(String.class)));
  }

  private void testStatusHandlerForToEntity(Publisher<?> responsePublisher) {
    StepVerifier.create(responsePublisher).expectError(WebClientResponseException.class).verify();
  }

  @Test
  void defaultRequestOverride() {
    WebClient client = this.builder
            .defaultRequest(spec -> spec.accept(MediaType.APPLICATION_JSON))
            .build();

    client.get().uri("/path")
            .accept(MediaType.IMAGE_PNG)
            .retrieve()
            .bodyToMono(Void.class)
            .block(Duration.ofSeconds(3));

    ClientRequest request = verifyAndGetRequest();
    assertThat(request.headers().getAccept()).containsExactly(MediaType.IMAGE_PNG);
  }

  private ClientRequest verifyAndGetRequest() {
    ClientRequest request = this.captor.getValue();
    verify(this.exchangeFunction).exchange(request);
    verifyNoMoreInteractions(this.exchangeFunction);
    return request;
  }

}
