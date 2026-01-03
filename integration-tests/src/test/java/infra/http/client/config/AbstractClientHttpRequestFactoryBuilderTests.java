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

package infra.http.client.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpResponse;
import infra.test.classpath.resources.WithPackageResources;
import infra.util.StreamUtils;
import infra.web.server.Ssl;
import infra.web.server.Ssl.ClientAuth;
import infra.web.server.WebServer;
import infra.web.server.support.NettyWebServerFactory;
import io.netty.channel.ChannelHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Base class for {@link ClientHttpRequestFactoryBuilder} tests.
 *
 * @param <T> The {@link ClientHttpRequestFactory} type
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
abstract class AbstractClientHttpRequestFactoryBuilderTests<T extends ClientHttpRequestFactory> extends AbstractWebServerSupport {

  protected static final Function<HttpMethod, HttpStatus> ALWAYS_FOUND = (method) -> HttpStatus.FOUND;

  private final Class<T> requestFactoryType;

  private final ClientHttpRequestFactoryBuilder<T> builder;

  AbstractClientHttpRequestFactoryBuilderTests(Class<T> requestFactoryType,
          ClientHttpRequestFactoryBuilder<T> builder) {
    this.requestFactoryType = requestFactoryType;
    this.builder = builder;
  }

  @Test
  void buildReturnsRequestFactoryOfExpectedType() {
    T requestFactory = this.builder.build();
    assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
  }

  @Test
  void buildWhenHasConnectTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withConnectTimeout(Duration.ofSeconds(60));
    T requestFactory = this.builder.build(settings);
    assertThat(connectTimeout(requestFactory)).isEqualTo(Duration.ofSeconds(60).toMillis());
  }

  @Test
  void buildWhenHadReadTimeout() {
    HttpClientSettings settings = HttpClientSettings.defaults().withReadTimeout(Duration.ofSeconds(120));
    T requestFactory = this.builder.build(settings);
    assertThat(readTimeout(requestFactory)).isEqualTo(Duration.ofSeconds(120).toMillis());
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
      ClientHttpRequest insecureRequest = request(builder.build(), uri, httpMethod);
      assertThatExceptionOfType(SSLHandshakeException.class)
              .isThrownBy(() -> insecureRequest.execute().getBody());

      ClientHttpRequestFactory secureRequestFactory = this.builder.build(HttpClientSettings.ofSslBundle(sslBundle()));

      ClientHttpRequest secureRequest = request(secureRequestFactory, uri, httpMethod);
      String secureResponse = StreamUtils.copyToString(secureRequest.execute().getBody(), StandardCharsets.UTF_8);
      assertThat(secureResponse).contains("Received " + httpMethod + " request to /");
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

      ClientHttpRequestFactory requestFactory = this.builder.build(
              HttpClientSettings.ofSslBundle(sslBundle(SslOptions.of(Set.of("TLS_AES_256_GCM_SHA384"), null))));

      ClientHttpRequest secureRequest = request(requestFactory, uri, httpMethod);
      assertThatExceptionOfType(SSLHandshakeException.class)
              .isThrownBy(() -> secureRequest.execute().getBody());
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
          Function<HttpMethod, HttpStatus> expectedStatusForMethod) throws URISyntaxException, IOException {
    HttpStatus expectedStatus = expectedStatusForMethod.apply(httpMethod);
    NettyWebServerFactory webServerFactory = createWebServerFactory();
    WebServer webServer = webServerFactory.getWebServer();
    try {
      webServer.start();
      int port = webServer.getPort();
      URI uri = new URI("http://localhost:%s".formatted(port) + "/redirect");
      ClientHttpRequestFactory requestFactory = this.builder.build(settings);
      ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
      ClientHttpResponse response = request.execute();
      assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
      if (expectedStatus == HttpStatus.OK) {
        assertThat(response.getBody()).asString(StandardCharsets.UTF_8).contains("request to /redirected");
      }
    }
    finally {
      shutdownGracefully(webServer);
    }
  }

  private ClientHttpRequest request(ClientHttpRequestFactory factory, URI uri, String method) throws IOException {
    return factory.createRequest(uri, HttpMethod.valueOf(method));
  }

  private Ssl ssl(String... ciphers) {
    Ssl ssl = new Ssl();
    ssl.enabled = true;
    ssl.clientAuth = ClientAuth.NEED;
    ssl.keyPassword = "password";
    ssl.keyStore = "classpath:test.jks";
    ssl.trustStore = "classpath:test.jks";
    if (ciphers.length > 0) {
      ssl.ciphers = ciphers;
    }
    return ssl;
  }

  @Override
  protected ChannelHandler createChannelHandler() {
    return new RedirectHandler();
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

  protected abstract long connectTimeout(T requestFactory);

  protected abstract long readTimeout(T requestFactory);

}
