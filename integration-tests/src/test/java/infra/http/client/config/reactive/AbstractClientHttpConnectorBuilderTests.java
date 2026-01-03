/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.client.config.reactive;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Set;
import java.util.function.Function;

import javax.net.ssl.SSLHandshakeException;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundleKey;
import infra.core.ssl.SslOptions;
import infra.core.ssl.jks.JksSslStoreBundle;
import infra.core.ssl.jks.JksSslStoreDetails;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.client.config.AbstractWebServerSupport;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.HttpRedirects;
import infra.http.client.config.RedirectHandler;
import infra.http.client.reactive.ClientHttpConnector;
import infra.test.classpath.resources.WithPackageResources;
import infra.web.client.reactive.ClientRequest;
import infra.web.client.reactive.ClientResponse;
import infra.web.client.reactive.ExchangeFunctions;
import infra.web.client.reactive.WebClientRequestException;
import infra.web.server.Ssl;
import infra.web.server.WebServer;
import infra.web.server.support.NettyWebServerFactory;
import io.netty.channel.ChannelHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Base class for {@link ClientHttpConnectorBuilder} tests.
 *
 * @param <T> The {@link ClientHttpConnector} type
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
abstract class AbstractClientHttpConnectorBuilderTests<T extends ClientHttpConnector> extends AbstractWebServerSupport {

  private static final Function<HttpMethod, HttpStatus> ALWAYS_FOUND = (method) -> HttpStatus.FOUND;

  private final Class<T> connectorType;

  private final ClientHttpConnectorBuilder<T> builder;

  AbstractClientHttpConnectorBuilderTests(Class<T> connectorType, ClientHttpConnectorBuilder<T> builder) {
    this.connectorType = connectorType;
    this.builder = builder;
  }

  @Test
  void buildReturnsConnectorOfExpectedType() {
    T connector = this.builder.build();
    assertThat(connector).isInstanceOf(this.connectorType);
  }

  @Test
  void buildWhenHasConnectTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(60));
    T connector = this.builder.build(settings);
    assertThat(connectTimeout(connector)).isEqualTo(Duration.ofSeconds(60).toMillis());
  }

  @Test
  void buildWhenHadReadTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofSeconds(120));
    T connector = this.builder.build(settings);
    assertThat(readTimeout(connector)).isEqualTo(Duration.ofSeconds(120).toMillis());
  }

  @ParameterizedTest
  @WithPackageResources("test.jks")
  @ValueSource(strings = { "GET", "POST" })
  void connectWithSslBundle(String httpMethod) throws Exception {
    NettyWebServerFactory webServerFactory = createWebServerFactory();
    webServerFactory.setSsl(ssl());
    WebServer webServer = webServerFactory.getWebServer();
    try {
      webServer.start();
      int port = webServer.getPort();
      URI uri = new URI("https://localhost:%s".formatted(port));
      ClientHttpConnector insecureConnector = this.builder.build();
      ClientRequest insecureRequest = createRequest(httpMethod, uri);
      assertThatExceptionOfType(WebClientRequestException.class)
              .isThrownBy(() -> getResponse(insecureConnector, insecureRequest))
              .withCauseInstanceOf(SSLHandshakeException.class);
      ClientHttpConnector secureConnector = this.builder.build(HttpClientSettings.ofSslBundle(sslBundle()));
      ClientRequest secureRequest = createRequest(httpMethod, uri);
      ClientResponse secureResponse = getResponse(secureConnector, secureRequest);
      assertThat(secureResponse.bodyToMono(String.class).block())
              .contains("Received " + httpMethod + " request to /");
    }
    finally {
      shutdownGracefully(webServer);
    }
  }

  @ParameterizedTest
  @WithPackageResources("test.jks")
  @ValueSource(strings = { "GET", "POST" })
  void connectWithSslBundleAndOptionsMismatch(String httpMethod) throws Exception {
    NettyWebServerFactory webServerFactory = createWebServerFactory();
    webServerFactory.setSsl(ssl("TLS_AES_128_GCM_SHA256"));
    WebServer webServer = webServerFactory.getWebServer();
    try {
      webServer.start();
      int port = webServer.getPort();
      URI uri = new URI("https://localhost:%s".formatted(port));
      ClientHttpConnector secureConnector = this.builder.build(
              HttpClientSettings.ofSslBundle(sslBundle(SslOptions.of(Set.of("TLS_AES_256_GCM_SHA384"), null))));
      ClientRequest secureRequest = createRequest(httpMethod, uri);
      assertThatExceptionOfType(WebClientRequestException.class)
              .isThrownBy(() -> getResponse(secureConnector, secureRequest))
              .withCauseInstanceOf(SSLHandshakeException.class);
    }
    finally {
      shutdownGracefully(webServer);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
  void redirectDefault(String httpMethod) throws Exception {
    testRedirect(null, HttpMethod.valueOf(httpMethod), this::getExpectedRedirect);
  }

  @ParameterizedTest
  @ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
  void redirectFollow(String httpMethod) throws Exception {
    HttpClientSettings settings = HttpClientSettings.defaults().withRedirects(HttpRedirects.FOLLOW);
    testRedirect(settings, HttpMethod.valueOf(httpMethod), this::getExpectedRedirect);
  }

  @ParameterizedTest
  @ValueSource(strings = { "GET", "POST", "PUT", "PATCH", "DELETE" })
  void redirectDontFollow(String httpMethod) throws Exception {
    HttpClientSettings settings = HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);
    testRedirect(settings, HttpMethod.valueOf(httpMethod), ALWAYS_FOUND);
  }

  protected final void testRedirect(@Nullable HttpClientSettings settings, HttpMethod httpMethod,
          Function<HttpMethod, HttpStatus> expectedStatusForMethod) throws URISyntaxException {
    HttpStatus expectedStatus = expectedStatusForMethod.apply(httpMethod);
    NettyWebServerFactory webServerFactory = createWebServerFactory();
    WebServer webServer = webServerFactory.getWebServer();
    try {
      webServer.start();
      int port = webServer.getPort();
      URI uri = new URI("http://localhost:%s".formatted(port) + "/redirect");
      ClientHttpConnector connector = this.builder.build(settings);
      ClientRequest request = createRequest(httpMethod, uri);
      ClientResponse response = getResponse(connector, request);
      assertThat(response.statusCode()).isEqualTo(expectedStatus);
      if (expectedStatus == HttpStatus.OK) {
        assertThat(response.bodyToMono(String.class).block()).contains("request to /redirected");
      }
    }
    finally {
      shutdownGracefully(webServer);
    }
  }

  private ClientRequest createRequest(String httpMethod, URI uri) {
    return createRequest(HttpMethod.valueOf(httpMethod), uri);
  }

  private ClientRequest createRequest(HttpMethod httpMethod, URI uri) {
    return ClientRequest.create(httpMethod, uri).build();
  }

  private ClientResponse getResponse(ClientHttpConnector connector, ClientRequest request) {
    ClientResponse response = ExchangeFunctions.create(connector).exchange(request).block();
    assertThat(response).isNotNull();
    return response;
  }

  private Ssl ssl(String... ciphers) {
    Ssl ssl = new Ssl();
    ssl.clientAuth = (Ssl.ClientAuth.NEED);
    ssl.keyPassword = ("password");
    ssl.keyStore = ("classpath:test.jks");
    ssl.trustStore = ("classpath:test.jks");
    if (ciphers.length > 0) {
      ssl.ciphers = (ciphers);
    }
    return ssl;
  }

  protected final SslBundle sslBundle() {
    return sslBundle(SslOptions.NONE);
  }

  protected final SslBundle sslBundle(SslOptions sslOptions) {
    JksSslStoreDetails storeDetails = JksSslStoreDetails.forLocation("classpath:test.jks");
    JksSslStoreBundle stores = new JksSslStoreBundle(storeDetails, storeDetails);
    return SslBundle.of(stores, SslBundleKey.of("password"), sslOptions);
  }

  protected HttpStatus getExpectedRedirect(HttpMethod httpMethod) {
    return HttpStatus.OK;
  }

  @Override
  protected ChannelHandler createChannelHandler() {
    return new RedirectHandler();
  }

  protected abstract long connectTimeout(T connector);

  protected abstract long readTimeout(T connector);

}
