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

import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.Random;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import cn.taketoday.http.server.reactive.bootstrap.HttpServer;
import cn.taketoday.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RandomHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  private static final int REQUEST_SIZE = 4096 * 3;

  private static final int RESPONSE_SIZE = 1024 * 4;

  private final Random rnd = new Random();

  private final RandomHandler handler = new RandomHandler();

  @Override
  protected RandomHandler createHttpHandler() {
    return handler;
  }

  @ParameterizedHttpServerTest
  void random(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    // TODO: fix Reactor support

    RestTemplate restTemplate = new RestTemplate();

    byte[] body = randomBytes();
    RequestEntity<byte[]> request = RequestEntity.post(new URI("http://localhost:" + port)).body(body);
    ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getHeaders().getContentLength()).isEqualTo(RESPONSE_SIZE);
    assertThat(response.getBody().length).isEqualTo(RESPONSE_SIZE);
  }

  private byte[] randomBytes() {
    byte[] buffer = new byte[REQUEST_SIZE];
    rnd.nextBytes(buffer);
    return buffer;
  }

  private class RandomHandler implements HttpHandler {

    static final int CHUNKS = 16;

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      Mono<Integer> requestSizeMono = request.getBody().
              reduce(0, (integer, dataBuffer) -> integer +
                      dataBuffer.readableByteCount()).
              doOnNext(size -> assertThat(size).isEqualTo(REQUEST_SIZE)).
              doOnError(throwable -> assertThat(throwable).isNull());

      response.getHeaders().setContentLength(RESPONSE_SIZE);

      return requestSizeMono.then(response.writeWith(multipleChunks()));
    }

    private Publisher<DataBuffer> multipleChunks() {
      int chunkSize = RESPONSE_SIZE / CHUNKS;
      return Flux.range(1, CHUNKS).map(integer -> randomBuffer(chunkSize));
    }

    private DataBuffer randomBuffer(int size) {
      byte[] bytes = new byte[size];
      rnd.nextBytes(bytes);
      DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer(size);
      buffer.write(bytes);
      return buffer;
    }

  }

}
