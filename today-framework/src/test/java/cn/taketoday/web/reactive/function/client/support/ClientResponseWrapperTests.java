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

package cn.taketoday.web.reactive.function.client.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.TypeReference;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.web.reactive.function.BodyExtractor;
import cn.taketoday.web.reactive.function.BodyExtractors;
import cn.taketoday.web.reactive.function.client.ClientResponse;
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
  public void bodyToMonoParameterizedTypeReference() {
    Mono<String> result = Mono.just("foo");
    TypeReference<String> reference = new TypeReference<>() { };
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
  public void bodyToFluxParameterizedTypeReference() {
    Flux<String> result = Flux.just("foo");
    TypeReference<String> reference = new TypeReference<>() { };
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
  public void toEntityParameterizedTypeReference() {
    Mono<ResponseEntity<String>> result = Mono.just(new ResponseEntity<>("foo", HttpStatus.OK));
    TypeReference<String> reference = new TypeReference<>() { };
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
  public void toEntityListParameterizedTypeReference() {
    Mono<ResponseEntity<List<String>>> result = Mono.just(new ResponseEntity<>(singletonList("foo"), HttpStatus.OK));
    TypeReference<String> reference = new TypeReference<>() { };
    given(mockResponse.toEntityList(reference)).willReturn(result);

    assertThat(wrapper.toEntityList(reference)).isSameAs(result);
  }

}
