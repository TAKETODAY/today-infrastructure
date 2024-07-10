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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.reactive.ReactorClientHttpConnector;
import cn.taketoday.http.client.ReactorResourceFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebClient integration tests focusing on data buffer management.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebClientDataBufferAllocatingTests extends AbstractDataBufferAllocatingTests {

  private static final Duration DELAY = Duration.ofSeconds(5);

  private final ReactorResourceFactory factory = new ReactorResourceFactory();
  private MockWebServer server;
  private WebClient webClient;

  @BeforeAll
  void setUpReactorResourceFactory() {
    this.factory.setShutdownQuietPeriod(Duration.ofMillis(100));
    this.factory.afterPropertiesSet();
  }

  @AfterAll
  void destroyReactorResourceFactory() {
    this.factory.destroy();
  }

  private void setUp(DataBufferFactory bufferFactory) {
    super.bufferFactory = bufferFactory;
    this.server = new MockWebServer();
    this.webClient = WebClient
            .builder()
            .clientConnector(initConnector())
            .baseURI(this.server.url("/").toString())
            .build();
  }

  private ReactorClientHttpConnector initConnector() {
    assertThat(super.bufferFactory).isNotNull();

    if (super.bufferFactory instanceof NettyDataBufferFactory) {
      ByteBufAllocator allocator = ((NettyDataBufferFactory) super.bufferFactory).getByteBufAllocator();
      return new ReactorClientHttpConnector(this.factory,
              client -> client.option(ChannelOption.ALLOCATOR, allocator));
    }
    else {
      return new ReactorClientHttpConnector();
    }
  }

  @ParameterizedDataBufferAllocatingTest
  void bodyToMonoVoid(DataBufferFactory bufferFactory) {
    setUp(bufferFactory);

    this.server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setHeader("Content-Type", "application/json")
            .setChunkedBody("{\"foo\" : {\"bar\" : \"123\", \"baz\" : \"456\"}}", 5));

    Mono<Void> mono = this.webClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Void.class);

    StepVerifier.create(mono).expectComplete().verify(Duration.ofSeconds(3));
    Assertions.assertThat(this.server.getRequestCount()).isEqualTo(1);
  }

  @ParameterizedDataBufferAllocatingTest
    // SPR-17482
  void bodyToMonoVoidWithoutContentType(DataBufferFactory bufferFactory) {
    setUp(bufferFactory);

    this.server.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.ACCEPTED.value())
            .setChunkedBody("{\"foo\" : \"123\",  \"baz\" : \"456\", \"baz\" : \"456\"}", 5));

    Mono<Map<String, String>> mono = this.webClient.get()
            .uri("/sample").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() { });

    StepVerifier.create(mono).expectError(WebClientResponseException.class).verify(Duration.ofSeconds(3));
    Assertions.assertThat(this.server.getRequestCount()).isEqualTo(1);
  }

  @ParameterizedDataBufferAllocatingTest
  void onStatusWithBodyNotConsumed(DataBufferFactory bufferFactory) {
    setUp(bufferFactory);

    RuntimeException ex = new RuntimeException("response error");
    testOnStatus(ex, response -> Mono.just(ex));
  }

  @ParameterizedDataBufferAllocatingTest
  void onStatusWithBodyConsumed(DataBufferFactory bufferFactory) {
    setUp(bufferFactory);

    RuntimeException ex = new RuntimeException("response error");
    testOnStatus(ex, response -> response.bodyToMono(Void.class).thenReturn(ex));
  }

  @ParameterizedDataBufferAllocatingTest
    // SPR-17473
  void onStatusWithMonoErrorAndBodyNotConsumed(DataBufferFactory bufferFactory) {
    setUp(bufferFactory);

    RuntimeException ex = new RuntimeException("response error");
    testOnStatus(ex, response -> Mono.error(ex));
  }

  @ParameterizedDataBufferAllocatingTest
  void onStatusWithMonoErrorAndBodyConsumed(DataBufferFactory bufferFactory) {
    setUp(bufferFactory);

    RuntimeException ex = new RuntimeException("response error");
    testOnStatus(ex, response -> response.bodyToMono(Void.class).then(Mono.error(ex)));
  }

  @ParameterizedDataBufferAllocatingTest
    // gh-23230
  void onStatusWithImmediateErrorAndBodyNotConsumed(DataBufferFactory bufferFactory) {
    setUp(bufferFactory);

    RuntimeException ex = new RuntimeException("response error");
    testOnStatus(ex, response -> {
      throw ex;
    });
  }

  @ParameterizedDataBufferAllocatingTest
  void releaseBody(DataBufferFactory bufferFactory) {
    setUp(bufferFactory);

    this.server.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/plain")
            .setBody("foo bar"));

    Mono<Void> result = this.webClient.get()
            .exchangeToMono(ClientResponse::releaseBody);

    StepVerifier.create(result)
            .expectComplete()
            .verify(Duration.ofSeconds(3));
  }

  @ParameterizedDataBufferAllocatingTest
  void exchangeToBodilessEntity(DataBufferFactory bufferFactory) {
    setUp(bufferFactory);

    this.server.enqueue(new MockResponse()
            .setResponseCode(201)
            .setHeader("Foo", "bar")
            .setBody("foo bar"));

    Mono<ResponseEntity<Void>> result = this.webClient.get()
            .exchangeToMono(ClientResponse::toBodilessEntity);

    StepVerifier.create(result)
            .assertNext(entity -> {
              Assertions.assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
              Assertions.assertThat(entity.getHeaders()).containsEntry("Foo", Collections.singletonList("bar"));
              Assertions.assertThat(entity.getBody()).isNull();
            })
            .expectComplete()
            .verify(Duration.ofSeconds(3));
  }

  private void testOnStatus(Throwable expected,
          Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

    HttpStatus errorStatus = HttpStatus.BAD_GATEWAY;

    this.server.enqueue(new MockResponse()
            .setResponseCode(errorStatus.value())
            .setHeader("Content-Type", "application/json")
            .setChunkedBody("{\"error\" : {\"status\" : 502, \"message\" : \"Bad gateway.\"}}", 5));

    Mono<String> mono = this.webClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(status -> status.equals(errorStatus), exceptionFunction)
            .bodyToMono(String.class);

    StepVerifier.create(mono).expectErrorSatisfies(actual -> Assertions.assertThat(actual).isSameAs(expected)).verify(DELAY);
    Assertions.assertThat(this.server.getRequestCount()).isEqualTo(1);
  }

}
