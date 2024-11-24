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

package infra.web.service.invoker;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import infra.http.HttpHeaders;
import infra.lang.Nullable;
import infra.util.concurrent.Future;
import infra.web.annotation.PathVariable;
import infra.web.client.ClientResponse;
import infra.web.client.RestClient;
import infra.web.client.reactive.WebClient;
import infra.web.client.reactive.support.WebClientAdapter;
import infra.web.client.support.RestClientAdapter;
import infra.web.service.annotation.GetExchange;
import infra.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/28 22:27
 */
class HttpServiceIntegrationTests {

  public static Stream<Arguments> arguments() {
    return Stream.of(
            args("RestClient", () -> RestClientAdapter.create(RestClient.create())),
            args("WebClient", () -> WebClientAdapter.forClient(WebClient.create())));
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest
  @MethodSource("arguments")
  @interface ParameterizedAdapterTest {

  }

  private static Arguments args(String name, Supplier<HttpExchangeAdapter> adapterFactory) {
    HttpExchangeAdapter adapter = adapterFactory.get();
    Service service = HttpServiceProxyFactory.forAdapter(adapter).createClient(Service.class);
    return Arguments.of(Named.named(name, service));
  }

  @ParameterizedAdapterTest
  void headers(Service service) {

    HttpHeaders httpHeaders = service.headersSync();
    HttpHeaders headers = service.headers().join();
    HttpHeaders block = service.headersMono().block();
    HttpHeaders blockFirst = service.headersFlux().blockFirst();

    assertHeaders(httpHeaders);
    assertHeaders(headers);
    assertHeaders(block);
    assertHeaders(blockFirst);

  }

  @ParameterizedAdapterTest
  void status(Service service) {
    assertThat(service.status(202)).succeedsWithin(Duration.ofSeconds(10))
            .extracting(ClientResponse::getRawStatusCode).isEqualTo(202);

  }

  private void assertHeaders(@Nullable HttpHeaders headers) {
    assertThat(headers).isNotNull();
    assertThat(headers).containsEntry("Connection", List.of("keep-alive"));
    assertThat(headers).containsEntry("Access-Control-Allow-Credentials", List.of("true"));

  }

  @HttpExchange(url = "https://httpbin.org")
  interface Service {

    @GetExchange(url = "/headers")
    HttpHeaders headersSync();

    @GetExchange(url = "/headers")
    Future<HttpHeaders> headers();

    @GetExchange(url = "/headers")
    Mono<HttpHeaders> headersMono();

    @GetExchange(url = "/headers")
    Flux<HttpHeaders> headersFlux();

    @GetExchange
    Future<Void> execute(HttpHeaders headers);

    @GetExchange("/status/{code}")
    Future<ClientResponse> status(@PathVariable int code);

  }

}
