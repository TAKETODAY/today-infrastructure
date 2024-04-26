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

package cn.taketoday.framework.test.web.reactive.server;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.web.server.reactive.server.netty.ReactorNettyReactiveWebServerFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration test for {@link WebTestClientContextCustomizer} with a custom
 * {@link WebTestClient} bean.
 *
 * @author Phillip Webb
 */
@InfraTest(webEnvironment = InfraTest.WebEnvironment.RANDOM_PORT, properties = "app.main.application-type=reactive_web")
@DirtiesContext
class WebTestClientContextCustomizerWithOverrideIntegrationTests {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void test() {
    assertThat(this.webTestClient).isInstanceOf(CustomWebTestClient.class);
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ TestHandler.class, NoWebTestClientBeanChecker.class })
  static class TestConfig {

    @Bean
    ReactorNettyReactiveWebServerFactory webServerFactory() {
      return new ReactorNettyReactiveWebServerFactory(0);
    }

    @Bean
    WebTestClient webTestClient() {
      return mock(CustomWebTestClient.class);
    }

  }

  static class TestHandler implements HttpHandler {

    private static final DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      response.setStatusCode(HttpStatus.OK);
      return response.writeWith(Mono.just(factory.wrap("hello".getBytes())));
    }

  }

  interface CustomWebTestClient extends WebTestClient {

  }

}
