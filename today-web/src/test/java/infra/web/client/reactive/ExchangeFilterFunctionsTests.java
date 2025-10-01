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

package infra.web.client.reactive;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.web.reactive.function.BodyExtractors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link ExchangeFilterFunctions}.
 *
 * @author Arjen Poutsma
 */
public class ExchangeFilterFunctionsTests {

  private static final URI DEFAULT_URL = URI.create("https://example.com");

  @Test
  public void andThen() {
    ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
    ClientResponse response = Mockito.mock(ClientResponse.class);
    ExchangeFunction exchange = r -> Mono.just(response);

    boolean[] filtersInvoked = new boolean[2];
    ExchangeFilterFunction filter1 = (r, n) -> {
      assertThat(filtersInvoked[0]).isFalse();
      assertThat(filtersInvoked[1]).isFalse();
      filtersInvoked[0] = true;
      assertThat(filtersInvoked[1]).isFalse();
      return n.exchange(r);
    };
    ExchangeFilterFunction filter2 = (r, n) -> {
      assertThat(filtersInvoked[0]).isTrue();
      assertThat(filtersInvoked[1]).isFalse();
      filtersInvoked[1] = true;
      return n.exchange(r);
    };
    ExchangeFilterFunction filter = filter1.andThen(filter2);

    ClientResponse result = filter.filter(request, exchange).block();
    assertThat(result).isEqualTo(response);

    assertThat(filtersInvoked[0]).isTrue();
    assertThat(filtersInvoked[1]).isTrue();
  }

  @Test
  public void apply() {
    ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
    ClientResponse response = Mockito.mock(ClientResponse.class);
    ExchangeFunction exchange = r -> Mono.just(response);

    boolean[] filterInvoked = new boolean[1];
    ExchangeFilterFunction filter = (r, n) -> {
      assertThat(filterInvoked[0]).isFalse();
      filterInvoked[0] = true;
      return n.exchange(r);
    };

    ExchangeFunction filteredExchange = filter.apply(exchange);
    ClientResponse result = filteredExchange.exchange(request).block();
    assertThat(result).isEqualTo(response);
    assertThat(filterInvoked[0]).isTrue();
  }

  @Test
  public void basicAuthenticationUsernamePassword() {
    ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
    ClientResponse response = Mockito.mock(ClientResponse.class);

    ExchangeFunction exchange = r -> {
      assertThat(r.headers().containsKey(HttpHeaders.AUTHORIZATION)).isTrue();
      assertThat(r.headers().getFirst(HttpHeaders.AUTHORIZATION).startsWith("Basic ")).isTrue();
      return Mono.just(response);
    };

    ExchangeFilterFunction auth = ExchangeFilterFunctions.basicAuthentication("foo", "bar");
    assertThat(request.headers().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    ClientResponse result = auth.filter(request, exchange).block();
    assertThat(result).isEqualTo(response);
  }

  @Test
  public void basicAuthenticationInvalidCharacters() {
    ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
    ExchangeFunction exchange = r -> Mono.just(Mockito.mock(ClientResponse.class));

    assertThatIllegalArgumentException().isThrownBy(() ->
        ExchangeFilterFunctions.basicAuthentication("foo", "\ud83d\udca9").filter(request, exchange));
  }

  @Test
  public void basicAuthenticationAttributes() {
    ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL)
        .attributes(infra.web.client.reactive.ExchangeFilterFunctions
            .Credentials.basicAuthenticationCredentials("foo", "bar"))
        .build();
    ClientResponse response = Mockito.mock(ClientResponse.class);

    ExchangeFunction exchange = r -> {
      assertThat(r.headers().containsKey(HttpHeaders.AUTHORIZATION)).isTrue();
      assertThat(r.headers().getFirst(HttpHeaders.AUTHORIZATION).startsWith("Basic ")).isTrue();
      return Mono.just(response);
    };

    ExchangeFilterFunction auth = ExchangeFilterFunctions.basicAuthentication();
    assertThat(request.headers().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    ClientResponse result = auth.filter(request, exchange).block();
    assertThat(result).isEqualTo(response);
  }

  @Test
  public void basicAuthenticationAbsentAttributes() {
    ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
    ClientResponse response = Mockito.mock(ClientResponse.class);

    ExchangeFunction exchange = r -> {
      assertThat(r.headers().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
      return Mono.just(response);
    };

    ExchangeFilterFunction auth = ExchangeFilterFunctions.basicAuthentication();
    assertThat(request.headers().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    ClientResponse result = auth.filter(request, exchange).block();
    assertThat(result).isEqualTo(response);
  }

  @Test
  public void statusHandlerMatch() {
    ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
    ClientResponse response = Mockito.mock(ClientResponse.class);
    given(response.statusCode()).willReturn(HttpStatus.NOT_FOUND);

    ExchangeFunction exchange = r -> Mono.just(response);

    ExchangeFilterFunction errorHandler = ExchangeFilterFunctions.statusError(
        HttpStatusCode::is4xxClientError, r -> new MyException());

    Mono<ClientResponse> result = errorHandler.filter(request, exchange);

    StepVerifier.create(result)
        .expectError(MyException.class)
        .verify();
  }

  @Test
  public void statusHandlerNoMatch() {
    ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
    ClientResponse response = Mockito.mock(ClientResponse.class);
    given(response.statusCode()).willReturn(HttpStatus.NOT_FOUND);

    Mono<ClientResponse> result = ExchangeFilterFunctions
        .statusError(HttpStatusCode::is5xxServerError, req -> new MyException())
        .filter(request, req -> Mono.just(response));

    StepVerifier.create(result)
        .expectNext(response)
        .expectComplete()
        .verify();
  }

  @Test
  public void limitResponseSize() {
    DataBuffer b1 = dataBuffer("foo");
    DataBuffer b2 = dataBuffer("bar");
    DataBuffer b3 = dataBuffer("baz");

    ClientRequest request = ClientRequest.create(HttpMethod.GET, DEFAULT_URL).build();
    ClientResponse response = ClientResponse.create(HttpStatus.OK).body(Flux.just(b1, b2, b3)).build();

    Mono<ClientResponse> result = ExchangeFilterFunctions.limitResponseSize(5)
        .filter(request, req -> Mono.just(response));

    StepVerifier.create(result.flatMapMany(res -> res.body(BodyExtractors.toDataBuffers())))
        .consumeNextWith(buffer -> Assertions.assertThat(string(buffer)).isEqualTo("foo"))
        .consumeNextWith(buffer -> Assertions.assertThat(string(buffer)).isEqualTo("ba"))
        .expectComplete()
        .verify();

  }

  private String string(DataBuffer buffer) {
    String value = buffer.toString(UTF_8);
    buffer.release();
    return value;
  }

  private DataBuffer dataBuffer(String foo) {
    return DefaultDataBufferFactory.sharedInstance.wrap(foo.getBytes(StandardCharsets.UTF_8));
  }

  @SuppressWarnings("serial")
  private static class MyException extends Exception {

  }

}
