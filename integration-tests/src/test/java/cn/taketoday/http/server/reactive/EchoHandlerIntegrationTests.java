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

package cn.taketoday.http.server.reactive;

import java.net.URI;
import java.util.Random;

import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import cn.taketoday.web.http.server.reactive.HttpServer;
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
