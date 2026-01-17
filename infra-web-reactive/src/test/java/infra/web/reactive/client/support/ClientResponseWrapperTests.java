/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.reactive.client.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpStatus;
import infra.http.reactive.ReactiveHttpInputMessage;
import infra.http.ResponseCookie;
import infra.http.ResponseEntity;
import infra.util.MultiValueMap;
import infra.web.reactive.client.ClientResponse;
import infra.web.reactive.BodyExtractor;
import infra.web.reactive.BodyExtractors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class ClientResponseWrapperTests {

  private ClientResponse mockResponse;

  private ClientResponseWrapper wrapper;

  @BeforeEach
  public void createWrapper() {
    this.mockResponse = mock(ClientResponse.class);
    this.wrapper = new ClientResponseWrapper(mockResponse);
  }

  @Test
  public void response() {
    assertThat(wrapper.response()).isSameAs(mockResponse);
  }

  @Test
  public void statusCode() {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    given(mockResponse.statusCode()).willReturn(status);

    assertThat(wrapper.statusCode()).isSameAs(status);
  }

  @Test
  public void rawStatusCode() {
    int status = 999;
    given(mockResponse.rawStatusCode()).willReturn(status);

    assertThat(wrapper.rawStatusCode()).isEqualTo(status);
  }

  @Test
  public void headers() {
    ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
    given(mockResponse.headers()).willReturn(headers);

    assertThat(wrapper.headers()).isSameAs(headers);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void cookies() {
    MultiValueMap<String, ResponseCookie> cookies = mock(MultiValueMap.class);
    given(mockResponse.cookies()).willReturn(cookies);

    assertThat(wrapper.cookies()).isSameAs(cookies);
  }

  @Test
  public void bodyExtractor() {
    Mono<String> result = Mono.just("foo");
    BodyExtractor<Mono<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(String.class);
    given(mockResponse.body(extractor)).willReturn(result);

    assertThat(wrapper.body(extractor)).isSameAs(result);
  }

  @Test
  public void bodyToMonoClass() {
    Mono<String> result = Mono.just("foo");
    given(mockResponse.bodyToMono(String.class)).willReturn(result);

    assertThat(wrapper.bodyToMono(String.class)).isSameAs(result);
  }

  @Test
  public void bodyToMonoTypeReference() {
    Mono<String> result = Mono.just("foo");
    ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<>() { };
    given(mockResponse.bodyToMono(reference)).willReturn(result);

    assertThat(wrapper.bodyToMono(reference)).isSameAs(result);
  }

  @Test
  public void bodyToFluxClass() {
    Flux<String> result = Flux.just("foo");
    given(mockResponse.bodyToFlux(String.class)).willReturn(result);

    assertThat(wrapper.bodyToFlux(String.class)).isSameAs(result);
  }

  @Test
  public void bodyToFluxTypeReference() {
    Flux<String> result = Flux.just("foo");
    ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<>() { };
    given(mockResponse.bodyToFlux(reference)).willReturn(result);

    assertThat(wrapper.bodyToFlux(reference)).isSameAs(result);
  }

  @Test
  public void toEntityClass() {
    Mono<ResponseEntity<String>> result = Mono.just(new ResponseEntity<>("foo", HttpStatus.OK));
    given(mockResponse.toEntity(String.class)).willReturn(result);

    assertThat(wrapper.toEntity(String.class)).isSameAs(result);
  }

  @Test
  public void toEntityTypeReference() {
    Mono<ResponseEntity<String>> result = Mono.just(new ResponseEntity<>("foo", HttpStatus.OK));
    ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<>() { };
    given(mockResponse.toEntity(reference)).willReturn(result);

    assertThat(wrapper.toEntity(reference)).isSameAs(result);
  }

  @Test
  public void toEntityListClass() {
    Mono<ResponseEntity<List<String>>> result = Mono.just(new ResponseEntity<>(singletonList("foo"), HttpStatus.OK));
    given(mockResponse.toEntityList(String.class)).willReturn(result);

    assertThat(wrapper.toEntityList(String.class)).isSameAs(result);
  }

  @Test
  public void toEntityListTypeReference() {
    Mono<ResponseEntity<List<String>>> result = Mono.just(new ResponseEntity<>(singletonList("foo"), HttpStatus.OK));
    ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<>() { };
    given(mockResponse.toEntityList(reference)).willReturn(result);

    assertThat(wrapper.toEntityList(reference)).isSameAs(result);
  }

}
