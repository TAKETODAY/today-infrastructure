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

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.net.URI;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import cn.taketoday.http.server.reactive.bootstrap.HttpServer;
import cn.taketoday.http.server.reactive.bootstrap.JettyHttpServer;
import cn.taketoday.web.client.ResponseErrorHandler;
import cn.taketoday.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
@Execution(ExecutionMode.SAME_THREAD)
class ErrorHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  private final ErrorHandler handler = new ErrorHandler();

  @Override
  protected HttpHandler createHttpHandler() {
    return handler;
  }

  @ParameterizedHttpServerTest
  void responseBodyError(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(NO_OP_ERROR_HANDLER);

    URI url = new URI("http://localhost:" + port + "/response-body-error");
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ParameterizedHttpServerTest
  void handlingError(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(NO_OP_ERROR_HANDLER);

    URI url = new URI("http://localhost:" + port + "/handling-error");
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ParameterizedHttpServerTest
    // SPR-15560
  void emptyPathSegments(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(NO_OP_ERROR_HANDLER);

    URI url = new URI("http://localhost:" + port + "//");
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Jetty 10+ rejects empty path segments, see https://github.com/eclipse/jetty.project/issues/6302,
    // but an application can apply CompactPathRule via RewriteHandler:
    // https://www.eclipse.org/jetty/documentation/jetty-11/programming_guide.php

    HttpStatus expectedStatus =
            (httpServer instanceof JettyHttpServer ? HttpStatus.BAD_REQUEST : HttpStatus.OK);

    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
  }

  private static class ErrorHandler implements HttpHandler {

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      Exception error = new UnsupportedOperationException();
      String path = request.getURI().getPath();
      if (path.endsWith("response-body-error")) {
        return response.writeWith(Mono.error(error));
      }
      else if (path.endsWith("handling-error")) {
        return Mono.error(error);
      }
      else {
        return Mono.empty();
      }
    }
  }

  private static final ResponseErrorHandler NO_OP_ERROR_HANDLER = new ResponseErrorHandler() {

    @Override
    public boolean hasError(ClientHttpResponse response) {
      return false;
    }

    @Override
    public void handleError(ClientHttpResponse response) {
    }
  };

}
