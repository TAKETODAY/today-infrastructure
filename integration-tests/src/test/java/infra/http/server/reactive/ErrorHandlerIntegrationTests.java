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

package infra.http.server.reactive;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.net.URI;

import infra.http.HttpStatus;
import infra.http.ResponseEntity;
import infra.web.client.NoOpResponseErrorHandler;
import infra.web.client.ResponseErrorHandler;
import infra.web.client.RestTemplate;
import infra.web.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import infra.web.http.server.reactive.HttpServer;
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
