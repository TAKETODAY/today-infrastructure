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

package infra.http.reactive.server;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.net.URI;

import infra.http.HttpStatus;
import infra.http.RequestEntity;
import infra.http.ResponseEntity;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.reactive.server.HttpHandler;
import infra.http.reactive.server.ServerHttpRequest;
import infra.http.reactive.server.ServerHttpResponse;
import infra.web.client.RestTemplate;
import infra.web.http.server.reactive.HttpServer;
import infra.web.http.server.reactive.ReactorHttpsServer;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTPS-specific integration test for {@link ServerHttpRequest}.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
@Execution(ExecutionMode.SAME_THREAD)
class ServerHttpsRequestIntegrationTests {

  private final HttpServer server = new ReactorHttpsServer();

  private int port;

  private RestTemplate restTemplate;

  @BeforeEach
  void startServer() throws Exception {
    this.server.setHandler(new CheckRequestHandler());
    this.server.afterPropertiesSet();
    this.server.start();

    // Set dynamically chosen port
    this.port = this.server.getPort();

    SSLContextBuilder builder = new SSLContextBuilder();
    builder.loadTrustMaterial(new TrustSelfSignedStrategy());
    SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
            builder.build(), NoopHostnameVerifier.INSTANCE);
    PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(socketFactory)
            .build();
    CloseableHttpClient httpclient = HttpClients.custom().
            setConnectionManager(connectionManager).build();
    HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory(httpclient);
    this.restTemplate = new RestTemplate(requestFactory);
  }

  @AfterEach
  void stopServer() {
    this.server.stop();
  }

  @Test
  void checkUri() throws Exception {
    URI url = URI.create("https://localhost:" + port + "/foo?param=bar");
    RequestEntity<Void> request = RequestEntity.post(url).build();
    ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private static class CheckRequestHandler implements HttpHandler {

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      URI uri = request.getURI();
      assertThat(uri.getScheme()).isEqualTo("https");
      assertThat(uri.getHost()).isNotNull();
      assertThat(uri.getPort()).isNotEqualTo(-1);
      assertThat(request.getRemoteAddress()).isNotNull();
      assertThat(uri.getPath()).isEqualTo("/foo");
      assertThat(uri.getQuery()).isEqualTo("param=bar");
      return Mono.empty();
    }
  }

}
