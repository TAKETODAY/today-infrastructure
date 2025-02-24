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

package infra.http.server.reactive;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import infra.core.io.buffer.DataBuffer;
import infra.http.RequestEntity;
import infra.http.ResponseEntity;
import infra.web.client.RestTemplate;
import infra.web.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import infra.web.http.server.reactive.HttpServer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Violeta Georgieva
 * @since 4.0
 */
class WriteOnlyHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  private static final int REQUEST_SIZE = 4096 * 3;

  private final Random rnd = new Random();

  private byte[] body;

  @Override
  protected WriteOnlyHandler createHttpHandler() {
    return new WriteOnlyHandler();
  }

  @ParameterizedHttpServerTest
  void writeOnly(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    RestTemplate restTemplate = new RestTemplate();

    this.body = randomBytes();
    RequestEntity<byte[]> request = RequestEntity.post(
            new URI("http://localhost:" + port)).body(
            "".getBytes(StandardCharsets.UTF_8));
    ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

    assertThat(response.getBody()).isEqualTo(body);
  }

  private byte[] randomBytes() {
    byte[] buffer = new byte[REQUEST_SIZE];
    rnd.nextBytes(buffer);
    return buffer;
  }

  class WriteOnlyHandler implements HttpHandler {

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      DataBuffer buffer = response.bufferFactory().allocateBuffer(body.length);
      buffer.write(body);
      return response.writeAndFlushWith(Flux.just(Flux.just(buffer)));
    }
  }

}
