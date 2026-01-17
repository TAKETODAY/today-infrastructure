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

package infra.http.reactive.server;

import org.junit.jupiter.api.Order;

import java.net.URI;
import java.time.Duration;

import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.core.testfixture.DisabledIfInContinuousIntegration;
import infra.http.RequestEntity;
import infra.http.ResponseEntity;
import infra.http.reactive.server.HttpHandler;
import infra.http.reactive.server.ServerHttpRequest;
import infra.http.reactive.server.ServerHttpResponse;
import infra.web.client.RestTemplate;
import infra.web.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import infra.web.http.server.reactive.HttpServer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Maldini
 * @since 4.0
 */
@Order(Integer.MAX_VALUE)
@DisabledIfInContinuousIntegration(disabledReason = "server port error")
class AsyncIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  private final Scheduler asyncGroup = Schedulers.parallel();

  @Override
  protected AsyncHandler createHttpHandler() {
    return new AsyncHandler();
  }

  @ParameterizedHttpServerTest
  void basicTest(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    URI url = new URI("http://localhost:" + port);
    ResponseEntity<String> response = new RestTemplate().exchange(RequestEntity.get(url).build(), String.class);

    assertThat(response.getBody()).isEqualTo("hello");
  }

  private class AsyncHandler implements HttpHandler {

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      return response.writeWith(
              Flux.just("h", "e", "l", "l", "o")
                      .delayElements(Duration.ofMillis(100))
                      .publishOn(asyncGroup)
                      .collect(DefaultDataBufferFactory.sharedInstance::allocateBuffer,
                              (buffer, str) -> buffer.write(str.getBytes())));
    }
  }

}
