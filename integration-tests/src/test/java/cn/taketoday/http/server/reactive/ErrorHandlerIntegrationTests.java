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

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.net.URI;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.web.client.NoOpResponseErrorHandler;
import cn.taketoday.web.client.ResponseErrorHandler;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import cn.taketoday.web.http.server.reactive.HttpServer;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
@Execution(ExecutionMode.SAME_THREAD)
class ErrorHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  private static final ResponseErrorHandler NO_OP_ERROR_HANDLER = new NoOpResponseErrorHandler();

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

    URI url = URI.create("http://localhost:" + port + "/response-body-error");
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ParameterizedHttpServerTest
  void handlingError(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(NO_OP_ERROR_HANDLER);

    URI url = URI.create("http://localhost:" + port + "/handling-error");
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ParameterizedHttpServerTest
  void emptyPathSegments(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(NO_OP_ERROR_HANDLER);

    URI url = URI.create("http://localhost:" + port + "//");
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    HttpStatus expectedStatus = HttpStatus.OK;
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

}
