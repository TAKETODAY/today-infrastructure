/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.server.reactive;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.HttpServer;
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
