/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.web.client.reactive;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import infra.core.ParameterizedTypeReference;
import infra.core.codec.ByteArrayDecoder;
import infra.core.codec.StringDecoder;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpRange;
import infra.http.HttpRequest;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.ResponseEntity;
import infra.http.client.reactive.ClientHttpResponse;
import infra.http.codec.DecoderHttpMessageReader;
import infra.http.codec.json.JacksonJsonDecoder;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static infra.web.reactive.function.BodyExtractors.toMono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 * @author Denys Ivano
 */
class DefaultClientResponseTests {

  private static final ParameterizedTypeReference<String> STRING_TYPE = new ParameterizedTypeReference<>() { };

  private final ClientHttpResponse mockResponse = mock();

  private final HttpHeaders httpHeaders = HttpHeaders.forWritable();

  private final ExchangeStrategies mockExchangeStrategies = mock();

  private @Nullable HttpRequest httpRequest = null;

  private DefaultClientResponse defaultClientResponse;

  @BeforeEach
  void configureMocks() {
    given(mockResponse.getHeaders()).willReturn(this.httpHeaders);
    defaultClientResponse = new DefaultClientResponse(mockResponse, mockExchangeStrategies, "", "", () -> httpRequest);
  }

  @Test
  void statusCode() {
    HttpStatus status = HttpStatus.CONTINUE;
    given(mockResponse.getStatusCode()).willReturn(status);

    assertThat(defaultClientResponse.statusCode()).isEqualTo(status);
  }

  @Test
  void header() {
    long contentLength = 42L;
    httpHeaders.setContentLength(contentLength);
    MediaType contentType = MediaType.TEXT_PLAIN;
    httpHeaders.setContentType(contentType);
    InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
    httpHeaders.setHost(host);
    httpHeaders.setRange(List.of(HttpRange.createByteRange(0, 42)));

    given(mockResponse.getHeaders()).willReturn(httpHeaders);

    ClientResponse.Headers headers = defaultClientResponse.headers();
    assertThat(headers.contentLength()).isEqualTo(OptionalLong.of(contentLength));
    assertThat(headers.contentType()).contains(contentType);
    assertThat(headers.asHttpHeaders()).isEqualTo(httpHeaders);
  }

  @Test
  void cookies() {
    ResponseCookie cookie = ResponseCookie.from("foo", "bar").build();
    MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();
    cookies.add("foo", cookie);

    given(mockResponse.getCookies()).willReturn(cookies);

    assertThat(defaultClientResponse.cookies()).isSameAs(cookies);
  }

  @Test
  void body() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    mockTextPlainResponse(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    Mono<String> resultMono = defaultClientResponse.body(toMono(String.class));
    assertThat(resultMono.block()).isEqualTo("foo");
  }

  @Test
  void bodyToMono() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    mockTextPlainResponse(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    Mono<String> resultMono = defaultClientResponse.bodyToMono(String.class);
    assertThat(resultMono.block()).isEqualTo("foo");
  }

  @Test
  void bodyToMonoTypeReference() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    mockTextPlainResponse(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    Mono<String> resultMono = defaultClientResponse.bodyToMono(STRING_TYPE);
    assertThat(resultMono.block()).isEqualTo("foo");
  }

  @Test
  void bodyToFlux() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    mockTextPlainResponse(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    Flux<String> resultFlux = defaultClientResponse.bodyToFlux(String.class);
    Mono<List<String>> result = resultFlux.collectList();
    assertThat(result.block()).containsExactly("foo");
  }

  @Test
  void bodyToFluxTypeReference() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    mockTextPlainResponse(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    Flux<String> resultFlux = defaultClientResponse.bodyToFlux(STRING_TYPE);
    Mono<List<String>> result = resultFlux.collectList();
    assertThat(result.block()).containsExactly("foo");
  }

  @Test
  void toEntity() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    mockTextPlainResponse(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    ResponseEntity<String> result = defaultClientResponse.toEntity(String.class).block();
    assertThat(result.getBody()).isEqualTo("foo");
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void toEntityWithUnknownStatusCode() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getHeaders()).willReturn(httpHeaders);
    given(mockResponse.getStatusCode()).willReturn(HttpStatusCode.valueOf(999));
    given(mockResponse.getBody()).willReturn(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    ResponseEntity<String> result = defaultClientResponse.toEntity(String.class).block();
    assertThat(result.getBody()).isEqualTo("foo");
    assertThat(result.getStatusCode().value()).isEqualTo(999);
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void toEntityTypeReference() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    mockTextPlainResponse(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    ResponseEntity<String> result = defaultClientResponse.toEntity(STRING_TYPE).block();
    assertThat(result.getBody()).isEqualTo("foo");
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void toEntityList() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    mockTextPlainResponse(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(String.class).block();
    assertThat(result.getBody()).containsExactly("foo");
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void toEntityListWithUnknownStatusCode() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getHeaders()).willReturn(httpHeaders);
    given(mockResponse.getStatusCode()).willReturn(HttpStatusCode.valueOf(999));
    given(mockResponse.getBody()).willReturn(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(String.class).block();
    assertThat(result.getBody()).containsExactly("foo");
    assertThat(result.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(999));
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void toEntityListTypeReference() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

    mockTextPlainResponse(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

    ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(STRING_TYPE).block();
    assertThat(result.getBody()).containsExactly("foo");
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void createException() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(mockResponse.getBody()).willReturn(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(new ByteArrayDecoder())));

    Mono<WebClientResponseException> resultMono = defaultClientResponse.createException();
    WebClientResponseException exception = resultMono.block();
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(exception.getMessage()).isEqualTo("404 Not Found");
    assertThat(exception.getHeaders()).containsExactly(entry("Content-Type", List.of("text/plain")));
    assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(bytes);
  }

  @Test
  @SuppressWarnings("unchecked")
  void createExceptionAndDecodeContent() {
    byte[] bytes = "{\"name\":\"Jason\"}".getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(mockResponse.getBody()).willReturn(Flux.just(buffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(List.of(
            new DecoderHttpMessageReader<>(new ByteArrayDecoder()),
            new DecoderHttpMessageReader<>(new JacksonJsonDecoder())));

    WebClientResponseException ex = defaultClientResponse.createException().block();
    assertThat(ex.getResponseBodyAs(Map.class)).containsExactly(entry("name", "Jason"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void createExceptionAndDecodeWithoutContent() {
    byte[] bytes = new byte[0];
    DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(mockResponse.getBody()).willReturn(Flux.just(buffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(List.of(
            new DecoderHttpMessageReader<>(new ByteArrayDecoder()),
            new DecoderHttpMessageReader<>(new JacksonJsonDecoder())));

    WebClientResponseException ex = defaultClientResponse.createException().block();
    assertThat(ex.getResponseBodyAs(Map.class)).isNull();
  }

  @Test
  void createError() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(mockResponse.getBody()).willReturn(Flux.just(dataBuffer));

    given(mockExchangeStrategies.messageReaders()).willReturn(
            List.of(new DecoderHttpMessageReader<>(new ByteArrayDecoder())));

    Mono<String> resultMono = defaultClientResponse.createError();
    StepVerifier.create(resultMono)
            .consumeErrorWith(t -> {
              assertThat(t).isInstanceOf(WebClientResponseException.class);
              WebClientResponseException exception = (WebClientResponseException) t;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getMessage()).isEqualTo("404 Not Found");
              assertThat(exception.getHeaders()).containsExactly(entry("Content-Type", List.of("text/plain")));
              assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(bytes);
            })
            .verify();
  }

  private void mockTextPlainResponse(Flux<DataBuffer> body) {
    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getStatusCode()).willReturn(HttpStatus.OK);
    given(mockResponse.getBody()).willReturn(body);
  }

}
