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

package infra.http.client.reactive;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.ReactiveHttpOutputMessage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

/**
 * @author Arjen Poutsma
 */
public class ClientHttpConnectorTests {

  private static final int BUF_SIZE = 1024;

  private static final Set<HttpMethod> METHODS_WITH_BODY =
          Set.of(HttpMethod.PUT, HttpMethod.POST, HttpMethod.PATCH);

  private final MockWebServer server = new MockWebServer();

  @BeforeEach
  void startServer() throws IOException {
    server.start();
  }

  @AfterEach
  void stopServer() throws IOException {
    server.shutdown();
  }

  // Do not auto-close arguments since HttpComponentsClientHttpConnector implements
  // AutoCloseable and is shared between parameterized test invocations.
  @ParameterizedTest(autoCloseArguments = false)
  @MethodSource("infra.http.client.reactive.ClientHttpConnectorTests#methodsWithConnectors")
  void basic(ClientHttpConnector connector, HttpMethod method) throws Exception {
    URI uri = this.server.url("/").uri();

    String responseBody = "bar\r\n";
    prepareResponse(response -> {
      response.setResponseCode(200);
      response.addHeader("Baz", "Qux");
      response.setBody(responseBody);
    });

    String requestBody = "foo\r\n";
    boolean requestHasBody = METHODS_WITH_BODY.contains(method);

    Mono<ClientHttpResponse> futureResponse = connector.connect(method, uri, request -> {
      assertThat(request.getMethod()).isEqualTo(method);
      assertThat(request.getURI()).isEqualTo(uri);
      request.getHeaders().add("Foo", "Bar");
      if (requestHasBody) {
        Mono<DataBuffer> body = Mono.fromCallable(() -> {
          byte[] bytes = requestBody.getBytes(StandardCharsets.UTF_8);
          return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
        });
        return request.writeWith(body);
      }
      else {
        return request.setComplete();
      }
    });

    CountDownLatch latch = new CountDownLatch(1);
    StepVerifier.create(futureResponse)
            .assertNext(response -> {
              assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
              assertThat(response.getHeaders().getFirst("Baz")).isEqualTo("Qux");
              DataBufferUtils.join(response.getBody())
                      .map(buffer -> {
                        String s = buffer.toString(StandardCharsets.UTF_8);
                        DataBufferUtils.release(buffer);
                        return s;
                      }).subscribe(
                              s -> assertThat(s).isEqualTo(responseBody),
                              throwable -> {
                                latch.countDown();
                                Assertions.fail(throwable.getMessage(), throwable);
                              },
                              latch::countDown);
            })
            .verifyComplete();
    latch.await();

    expectRequest(request -> {
      assertThat(request.getMethod()).isEqualTo(method.name());
      assertThat(request.getHeader("Foo")).isEqualTo("Bar");
      if (requestHasBody) {
        assertThat(request.getBody().readUtf8()).isEqualTo(requestBody);
      }
    });
  }

  @ParameterizedConnectorTest
  void errorInRequestBody(ClientHttpConnector connector) {
    Exception error = new RuntimeException();
    Flux<DataBuffer> body = Flux.concat(
            stringBuffer("foo"),
            Mono.error(error)
    );
    prepareResponse(response -> response.setResponseCode(200));
    Mono<ClientHttpResponse> futureResponse =
            connector.connect(HttpMethod.POST, this.server.url("/").uri(), request -> request.writeWith(body));
    StepVerifier.create(futureResponse)
            .expectErrorSatisfies(throwable -> assertThat(throwable).isSameAs(error))
            .verify();
  }

  @ParameterizedConnectorTest
  void cancelResponseBody(ClientHttpConnector connector) {
    Buffer responseBody = randomBody(100);
    prepareResponse(response -> response.setBody(responseBody));

    ClientHttpResponse response = connector.connect(HttpMethod.POST, this.server.url("/").uri(),
            ReactiveHttpOutputMessage::setComplete).block();
    assertThat(response).isNotNull();

    StepVerifier.create(response.getBody(), 1)
            .expectNextCount(1)
            .thenRequest(1)
            .thenCancel()
            .verify();
  }

  @ParameterizedConnectorTest
  void cookieExpireValueSetAsMaxAge(ClientHttpConnector connector) {
    ZonedDateTime tomorrow = ZonedDateTime.now(ZoneId.of("UTC")).plusDays(1);
    String formattedDate = tomorrow.format(DateTimeFormatter.RFC_1123_DATE_TIME);

    prepareResponse(response -> {
      response.setResponseCode(200);
      response.addHeader("Set-Cookie", "id=test; Expires= " + formattedDate + ";");
    });
    Mono<ClientHttpResponse> futureResponse =
            connector.connect(HttpMethod.GET, this.server.url("/").uri(), ReactiveHttpOutputMessage::setComplete);
    StepVerifier.create(futureResponse)
            .assertNext(response -> {
                      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                      assertThat(response.getCookies().getFirst("id").getMaxAge()).isCloseTo(Duration.ofDays(1), Duration.ofSeconds(10));
                    }
            )
            .verifyComplete();
  }

  private Buffer randomBody(int size) {
    Buffer responseBody = new Buffer();
    Random rnd = new Random();
    for (int i = 0; i < size; i++) {
      byte[] bytes = new byte[BUF_SIZE];
      rnd.nextBytes(bytes);
      responseBody.write(bytes);
    }
    return responseBody;
  }

  private void prepareResponse(Consumer<MockResponse> consumer) {
    MockResponse response = new MockResponse();
    consumer.accept(response);
    this.server.enqueue(response);
  }

  private void expectRequest(Consumer<RecordedRequest> consumer) {
    try {
      consumer.accept(this.server.takeRequest());
    }
    catch (InterruptedException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  // Do not auto-close arguments since HttpComponentsClientHttpConnector implements
  // AutoCloseable and is shared between parameterized test invocations.
  @ParameterizedTest(name = "{0}", autoCloseArguments = false)
  @MethodSource("infra.http.client.reactive.ClientHttpConnectorTests#connectors")
  public @interface ParameterizedConnectorTest {
  }

  static List<Named<ClientHttpConnector>> connectors() {
    return Arrays.asList(
            named("Reactor Netty", new ReactorClientHttpConnector()),
            named("HttpComponents", new HttpComponentsClientHttpConnector()),
            named("Jdk", new JdkClientHttpConnector())
    );
  }

  static List<Arguments> methodsWithConnectors() {
    List<Arguments> result = new ArrayList<>();
    for (Named<ClientHttpConnector> connector : connectors()) {
      for (HttpMethod method : HttpMethod.values()) {
        if (method != HttpMethod.CONNECT) {
          result.add(Arguments.of(connector, method));
        }
      }
    }
    return result;
  }

  private Mono<DataBuffer> stringBuffer(String value) {
    return Mono.fromCallable(() -> {
      byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
      DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer(bytes.length);
      buffer.write(bytes);
      return buffer;
    });
  }

}