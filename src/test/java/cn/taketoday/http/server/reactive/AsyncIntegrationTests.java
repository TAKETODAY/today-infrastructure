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
import java.time.Duration;

import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import cn.taketoday.http.server.reactive.bootstrap.HttpServer;
import cn.taketoday.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Maldini
 * @since 5.0
 */
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
