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

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import cn.taketoday.http.server.reactive.bootstrap.HttpServer;
import cn.taketoday.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class ServerHttpRequestIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  @Override
  protected CheckRequestHandler createHttpHandler() {
    return new CheckRequestHandler();
  }

  @ParameterizedHttpServerTest
  void checkUri(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    URI url = new URI("http://localhost:" + port + "/foo?param=bar");
    RequestEntity<Void> request = RequestEntity.post(url).build();
    ResponseEntity<Void> response = new RestTemplate().exchange(request, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  static class CheckRequestHandler implements HttpHandler {

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      URI uri = request.getURI();
      assertThat(uri.getScheme()).isEqualTo("http");
      assertThat(uri.getHost()).isNotNull();
      assertThat(uri.getPort()).isNotEqualTo(-1);
      assertThat(request.getRemoteAddress()).isNotNull();
      assertThat(uri.getPath()).isEqualTo("/foo");
      assertThat(uri.getQuery()).isEqualTo("param=bar");
      return Mono.empty();
    }
  }

}
