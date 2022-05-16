/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.reactive.function.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.core.TypeReference;
import cn.taketoday.core.codec.ByteArrayDecoder;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.reactive.ClientHttpResponse;
import cn.taketoday.http.codec.DecoderHttpMessageReader;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MultiValueMap;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import cn.taketoday.web.reactive.function.client.ClientResponse;
import cn.taketoday.web.reactive.function.client.DefaultClientResponse;
import cn.taketoday.web.reactive.function.client.ExchangeStrategies;
import cn.taketoday.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static cn.taketoday.web.reactive.function.BodyExtractors.toMono;

/**
 * @author Arjen Poutsma
 * @author Denys Ivano
 */
public class DefaultClientResponseTests {

  private ClientHttpResponse mockResponse;

  private final HttpHeaders httpHeaders = HttpHeaders.create();

  private ExchangeStrategies mockExchangeStrategies;

  private DefaultClientResponse defaultClientResponse;

  @BeforeEach
  public void createMocks() {
    mockResponse = mock(ClientHttpResponse.class);
    given(mockResponse.getHeaders()).willReturn(this.httpHeaders);
    mockExchangeStrategies = mock(ExchangeStrategies.class);
    defaultClientResponse = new DefaultClientResponse(mockResponse, mockExchangeStrategies, "", "", () -> null);
  }

  @Test
  public void statusCode() {
    HttpStatus status = HttpStatus.CONTINUE;
    given(mockResponse.getStatusCode()).willReturn(status);

    assertThat(defaultClientResponse.statusCode()).isEqualTo(status);
  }

  @Test
  public void rawStatusCode() {
    int status = 999;
    given(mockResponse.getRawStatusCode()).willReturn(status);

    assertThat(defaultClientResponse.rawStatusCode()).isEqualTo(status);
  }

  @Test
  public void header() {
    long contentLength = 42L;
    httpHeaders.setContentLength(contentLength);
    MediaType contentType = MediaType.TEXT_PLAIN;
    httpHeaders.setContentType(contentType);
    InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
    httpHeaders.setHost(host);
    List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(0, 42));
    httpHeaders.setRange(range);

    given(mockResponse.getHeaders()).willReturn(httpHeaders);

    ClientResponse.Headers headers = defaultClientResponse.headers();
    assertThat(headers.contentLength()).isEqualTo(OptionalLong.of(contentLength));
    assertThat(headers.contentType()).isEqualTo(Optional.of(contentType));
    assertThat(headers.asHttpHeaders()).isEqualTo(httpHeaders);
  }

  @Test
  public void cookies() {
    ResponseCookie cookie = ResponseCookie.from("foo", "bar").build();
    MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();
    cookies.add("foo", cookie);

    given(mockResponse.getCookies()).willReturn(cookies);

    assertThat(defaultClientResponse.cookies()).isSameAs(cookies);
  }

  @Test
  public void body() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    mockTextPlainResponse(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    Mono<String> resultMono = defaultClientResponse.body(toMono(String.class));
    assertThat(resultMono.block()).isEqualTo("foo");
  }

  @Test
  public void bodyToMono() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    mockTextPlainResponse(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    Mono<String> resultMono = defaultClientResponse.bodyToMono(String.class);
    assertThat(resultMono.block()).isEqualTo("foo");
  }

  @Test
  public void bodyToMonoTypeReference() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    mockTextPlainResponse(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    Mono<String> resultMono =
            defaultClientResponse.bodyToMono(new TypeReference<String>() {
            });
    assertThat(resultMono.block()).isEqualTo("foo");
  }

  @Test
  public void bodyToFlux() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    mockTextPlainResponse(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    Flux<String> resultFlux = defaultClientResponse.bodyToFlux(String.class);
    Mono<List<String>> result = resultFlux.collectList();
    assertThat(result.block()).isEqualTo(Collections.singletonList("foo"));
  }

  @Test
  public void bodyToFluxTypeReference() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    mockTextPlainResponse(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    Flux<String> resultFlux =
            defaultClientResponse.bodyToFlux(new TypeReference<String>() {
            });
    Mono<List<String>> result = resultFlux.collectList();
    assertThat(result.block()).isEqualTo(Collections.singletonList("foo"));
  }

  @Test
  public void toEntity() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    mockTextPlainResponse(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    ResponseEntity<String> result = defaultClientResponse.toEntity(String.class).block();
    assertThat(result.getBody()).isEqualTo("foo");
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  public void toEntityWithUnknownStatusCode() throws Exception {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getHeaders()).willReturn(httpHeaders);
    given(mockResponse.getStatusCode()).willReturn(HttpStatusCode.valueOf(999));
    given(mockResponse.getRawStatusCode()).willReturn(999);
    given(mockResponse.getBody()).willReturn(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    ResponseEntity<String> result = defaultClientResponse.toEntity(String.class).block();
    assertThat(result.getBody()).isEqualTo("foo");
    assertThat(result.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(999));
    assertThat(result.getStatusCodeValue()).isEqualTo(999);
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  public void toEntityTypeReference() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    mockTextPlainResponse(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    ResponseEntity<String> result = defaultClientResponse.toEntity(
            new TypeReference<String>() {
            }).block();
    assertThat(result.getBody()).isEqualTo("foo");
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  public void toEntityList() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    mockTextPlainResponse(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(String.class).block();
    assertThat(result.getBody()).isEqualTo(Collections.singletonList("foo"));
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  public void toEntityListWithUnknownStatusCode() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getHeaders()).willReturn(httpHeaders);
    given(mockResponse.getStatusCode()).willReturn(HttpStatusCode.valueOf(999));
    given(mockResponse.getRawStatusCode()).willReturn(999);
    given(mockResponse.getBody()).willReturn(body);

    List<HttpMessageReader<?>> messageReaders = Collections.singletonList(
            new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(String.class).block();
    assertThat(result.getBody()).isEqualTo(Collections.singletonList("foo"));
    assertThat(result.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(999));
    assertThat(result.getStatusCodeValue()).isEqualTo(999);
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  public void toEntityListTypeReference() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    mockTextPlainResponse(body);

    List<HttpMessageReader<?>> messageReaders = Collections
            .singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(
            new TypeReference<String>() { }).block();
    assertThat(result.getBody()).isEqualTo(Collections.singletonList("foo"));
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  public void createException() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(mockResponse.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
    given(mockResponse.getBody()).willReturn(body);

    List<HttpMessageReader<?>> messageReaders = Collections.singletonList(
            new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    Mono<WebClientResponseException> resultMono = defaultClientResponse.createException();
    WebClientResponseException exception = resultMono.block();
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(exception.getMessage()).isEqualTo("404 Not Found");
    assertThat(exception.getHeaders()).containsExactly(entry("Content-Type",
            Collections.singletonList("text/plain")));
    assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(bytes);
  }

  @Test
  public void createError() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);
    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
    given(mockResponse.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
    given(mockResponse.getBody()).willReturn(body);

    List<HttpMessageReader<?>> messageReaders = Collections.singletonList(
            new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
    given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

    Mono<String> resultMono = defaultClientResponse.createError();
    StepVerifier.create(resultMono)
            .consumeErrorWith(t -> {
              assertThat(t).isInstanceOf(WebClientResponseException.class);
              WebClientResponseException exception = (WebClientResponseException) t;
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getMessage()).isEqualTo("404 Not Found");
              assertThat(exception.getHeaders()).containsExactly(entry("Content-Type",
                      Collections.singletonList("text/plain")));
              assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(bytes);

            })
            .verify();
  }

  private void mockTextPlainResponse(Flux<DataBuffer> body) {
    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
    given(mockResponse.getStatusCode()).willReturn(HttpStatus.OK);
    given(mockResponse.getRawStatusCode()).willReturn(HttpStatus.OK.value());
    given(mockResponse.getBody()).willReturn(body);
  }

}
