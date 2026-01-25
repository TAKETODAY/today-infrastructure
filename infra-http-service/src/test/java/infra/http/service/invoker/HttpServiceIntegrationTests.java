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

package infra.http.service.invoker;

import org.jspecify.annotations.Nullable;
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

import infra.core.testfixture.DisabledIfInContinuousIntegration;
import infra.http.HttpHeaders;
import infra.http.service.annotation.GetExchange;
import infra.http.service.annotation.HttpExchange;
import infra.http.service.support.RestClientAdapter;
import infra.http.service.support.WebClientAdapter;
import infra.util.concurrent.Future;
import infra.web.annotation.PathVariable;
import infra.web.client.ClientResponse;
import infra.web.client.RestClient;
import infra.web.reactive.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/28 22:27
 */
@DisabledIfInContinuousIntegration
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
    return Arguments.of(name, Named.named(name, service));
  }

  @ParameterizedAdapterTest
  void headers(String type, Service service) {

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
  void status(String type, Service service) {
    if (type.equals("RestClient")) {
      assertThat(service.status(202)).succeedsWithin(Duration.ofSeconds(10))
              .extracting(ClientResponse::getRawStatusCode).isEqualTo(202);
    }
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
