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

import java.io.File;
import java.net.URI;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.ZeroCopyHttpOutputMessage;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.ReactorHttpServer;
import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.UndertowHttpServer;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Arjen Poutsma
 */
class ZeroCopyIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  private static final Resource springLogoResource = new ClassPathResource("/cn/taketoday/web/spring.png");

  private final ZeroCopyHandler handler = new ZeroCopyHandler();

  @Override
  protected HttpHandler createHttpHandler() {
    return this.handler;
  }

  @ParameterizedHttpServerTest
  void zeroCopy(HttpServer httpServer) throws Exception {
    assumeTrue(httpServer instanceof ReactorHttpServer || httpServer instanceof UndertowHttpServer,
            "Zero-copy does not support Servlet");

    startServer(httpServer);

    URI url = new URI("http://localhost:" + port);
    RequestEntity<?> request = RequestEntity.get(url).build();
    ResponseEntity<byte[]> response = new RestTemplate().exchange(request, byte[].class);

    assertThat(response.hasBody()).isTrue();
    assertThat(response.getHeaders().getContentLength()).isEqualTo(springLogoResource.contentLength());
    assertThat(response.getBody().length).isEqualTo(springLogoResource.contentLength());
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
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
