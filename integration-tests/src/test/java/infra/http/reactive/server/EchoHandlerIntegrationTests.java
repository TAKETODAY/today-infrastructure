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

import java.net.URI;
import java.util.Random;

import infra.http.RequestEntity;
import infra.http.ResponseEntity;
import infra.http.reactive.server.HttpHandler;
import infra.http.reactive.server.ServerHttpRequest;
import infra.http.reactive.server.ServerHttpResponse;
import infra.web.client.RestTemplate;
import infra.web.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import infra.web.http.server.reactive.HttpServer;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class EchoHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  private static final int REQUEST_SIZE = 4096 * 3;

  private final Random rnd = new Random();

  @Override
  protected EchoHandler createHttpHandler() {
    return new EchoHandler();
  }

  @ParameterizedHttpServerTest
  public void echo(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    RestTemplate restTemplate = new RestTemplate();

    byte[] body = randomBytes();
    RequestEntity<byte[]> request = RequestEntity.post(new URI("http://localhost:" + port)).body(body);
    ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

    assertThat(response.getBody()).isEqualTo(body);
  }

  private byte[] randomBytes() {
    byte[] buffer = new byte[REQUEST_SIZE];
    rnd.nextBytes(buffer);
    return buffer;
  }

  /**
   * @author Arjen Poutsma
   */
  public static class EchoHandler implements HttpHandler {

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      return response.writeWith(request.getBody());
    }
  }

}
