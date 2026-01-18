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

package infra.app.test.web.reactive.server;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.http.HttpStatus;
import infra.http.reactive.server.ContextPathCompositeHandler;
import infra.http.reactive.server.HttpHandler;
import infra.http.reactive.server.ServerHttpRequest;
import infra.http.reactive.server.ServerHttpResponse;
import infra.test.context.TestPropertySource;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.reactor.netty.ReactorNettyReactiveWebServerFactory;
import reactor.core.publisher.Mono;

/**
 * Tests for {@link WebTestClientContextCustomizer} with a custom base path for a reactive
 * web application.
 *
 * @author Madhura Bhave
 */
@InfraTest(webEnvironment = InfraTest.WebEnvironment.RANDOM_PORT,
        properties = "app.main.application-type=reactive_web")
@TestPropertySource(properties = "webflux.base-path=/test")
@Disabled
class WebTestClientContextCustomizerWithCustomBasePathTests {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void test() {
    this.webTestClient.get().uri("/hello")
            .exchange()
            .expectBody(String.class).isEqualTo("hello world");
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    ReactorNettyReactiveWebServerFactory webServerFactory() {
      return new ReactorNettyReactiveWebServerFactory(0);
    }

    @Bean
    HttpHandler httpHandler() {
      TestHandler httpHandler = new TestHandler();
      Map<String, HttpHandler> handlersMap = Collections.singletonMap("/test", httpHandler);
      return new ContextPathCompositeHandler(handlersMap);
    }

  }

  static class TestHandler implements HttpHandler {

    private static final DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      response.setStatusCode(HttpStatus.OK);
      return response.writeWith(Mono.just(factory.wrap("hello world".getBytes())));
    }

  }

}
