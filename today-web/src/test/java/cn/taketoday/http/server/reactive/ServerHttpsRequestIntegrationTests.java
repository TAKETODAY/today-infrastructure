/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.ReactorHttpsServer;
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
