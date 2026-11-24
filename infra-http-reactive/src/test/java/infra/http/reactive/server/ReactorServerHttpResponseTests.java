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

package infra.http.reactive.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BiFunction;

import infra.core.io.buffer.DataBuffer;
import infra.http.HttpStatus;
import infra.http.ResponseCookie;
import infra.http.reactive.ZeroCopyHttpOutputMessage;
import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerResponse;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class ReactorServerHttpResponseTests {

  @Test
  void writesStatusHeadersAndCookieAttributes() {
    exchange((request, response) -> {
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getRawStatusCode()).isEqualTo(200);
      HttpServerResponse nativeResponse = DelegatingServerHttpResponse.getNativeResponse(response);
      assertThat(nativeResponse.status().code()).isEqualTo(200);

      assertThat(response.setStatusCode(HttpStatus.CREATED)).isTrue();
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getRawStatusCode()).isEqualTo(201);
      response.getHeaders().set("X-Result", "created");
      response.addCookie(ResponseCookie.builder("session", "token")
              .maxAge(Duration.ofMinutes(1)).domain("example.com").path("/items")
              .secure(true).httpOnly(true).partitioned(true).sameSite("None").build());
      response.addCookie(ResponseCookie.forSimple("theme", "dark"));
      return response.writeWith(Mono.fromSupplier(() -> buffer(response, "created")));
    }, (response, body) -> {
      assertThat(response.status().code()).isEqualTo(201);
      assertThat(response.responseHeaders().get("X-Result")).isEqualTo("created");
      assertThat(response.responseHeaders().getAll(HttpHeaderNames.SET_COOKIE)).anySatisfy(cookie ->
              assertThat(cookie).startsWith("session=token;").contains("Max-Age=60", "Domain=example.com",
                      "Path=/items", "Secure", "HTTPOnly", "Partitioned", "SameSite=None"))
              .contains("theme=dark");
      return body.aggregate().asString();
    }, "created");
  }

  @Test
  void sendsAllFlushGroupsInOrder() {
    exchange((request, response) -> response.writeAndFlushWith(Flux.just(
            Flux.just("first", "-second").map(value -> buffer(response, value)),
            Mono.fromSupplier(() -> buffer(response, "-third")))), (response, body) -> {
      assertThat(response.status().code()).isEqualTo(200);
      return body.aggregate().asString();
    }, "first-second-third");
  }

  @Test
  void writesRequestedFileRegion(@TempDir Path directory) throws Exception {
    Path file = Files.writeString(directory.resolve("body.txt"), "prefix-content-suffix");

    exchange((request, response) -> {
      response.setRawStatusCode(299);
      response.getHeaders().setContentLength(7);
      return ((ZeroCopyHttpOutputMessage) response).writeWith(file, 7, 7);
    }, (response, body) -> {
      assertThat(response.status().code()).isEqualTo(299);
      assertThat(response.responseHeaders().get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("7");
      return body.aggregate().asString();
    }, "content");
  }

  private static DataBuffer buffer(ServerHttpResponse response, String value) {
    return response.bufferFactory().wrap(value.getBytes(StandardCharsets.UTF_8));
  }

  private static void exchange(HttpHandler handler,
          BiFunction<HttpClientResponse, ByteBufFlux, Mono<String>> receive, String expectedBody) {
    Duration timeout = Duration.ofSeconds(10);
    DisposableServer server = HttpServer.create().host("127.0.0.1").port(0)
            .handle(new ReactorHttpHandlerAdapter(handler)).bindNow(timeout);
    try {
      StepVerifier.create(HttpClient.create().port(server.port()).host("127.0.0.1")
                      .get().uri("/items").response(receive))
              .expectNext(expectedBody).expectComplete().verify(timeout);
    }
    finally {
      server.disposeNow(timeout);
    }
  }

}
