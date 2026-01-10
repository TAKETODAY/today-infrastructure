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

import java.io.File;
import java.net.URI;

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.MediaType;
import infra.http.RequestEntity;
import infra.http.ResponseEntity;
import infra.http.ZeroCopyHttpOutputMessage;
import infra.web.client.RestTemplate;
import infra.web.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import infra.web.http.server.reactive.HttpServer;
import infra.web.http.server.reactive.ReactorHttpServer;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Arjen Poutsma
 */
class ZeroCopyIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  private static final Resource springLogoResource = new ClassPathResource("/infra/web/spring.png");

  private final ZeroCopyHandler handler = new ZeroCopyHandler();

  @Override
  protected HttpHandler createHttpHandler() {
    return this.handler;
  }

  @ParameterizedHttpServerTest
  void zeroCopy(HttpServer httpServer) throws Exception {
    assumeTrue(httpServer instanceof ReactorHttpServer, "Zero-copy does not support Servlet");

    startServer(httpServer);

    URI url = new URI("http://localhost:" + port);
    RequestEntity<?> request = RequestEntity.get(url).build();
    ResponseEntity<byte[]> response = new RestTemplate().exchange(request, byte[].class);

    assertThat(response.hasBody()).isTrue();
    assertThat(response.headers().getContentLength()).isEqualTo(springLogoResource.contentLength());
    assertThat(response.getBody().length).isEqualTo(springLogoResource.contentLength());
    assertThat(response.headers().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
  }

  private static class ZeroCopyHandler implements HttpHandler {

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      try {
        ZeroCopyHttpOutputMessage zeroCopyResponse = (ZeroCopyHttpOutputMessage) response;
        File logoFile = springLogoResource.getFile();
        zeroCopyResponse.getHeaders().setContentType(MediaType.IMAGE_PNG);
        zeroCopyResponse.getHeaders().setContentLength(logoFile.length());
        return zeroCopyResponse.writeWith(logoFile);
      }
      catch (Throwable ex) {
        return Mono.error(ex);
      }
    }
  }

}
