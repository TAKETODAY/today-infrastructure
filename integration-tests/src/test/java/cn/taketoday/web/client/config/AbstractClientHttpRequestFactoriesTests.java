/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.client.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.net.ssl.SSLHandshakeException;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.jks.JksSslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreDetails;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.Ssl.ClientAuth;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.test.web.servlet.DirtiesUrlFactories;
import cn.taketoday.util.StreamUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Base classes for testing of {@link ClientHttpRequestFactories} with different HTTP
 * clients on the classpath.
 *
 * @param <T> the {@link ClientHttpRequestFactory} to be produced
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/1 23:10
 */
@DirtiesUrlFactories
abstract class AbstractClientHttpRequestFactoriesTests<T extends ClientHttpRequestFactory> {

  private final Class<T> requestFactoryType;

  protected AbstractClientHttpRequestFactoriesTests(Class<T> requestFactoryType) {
    this.requestFactoryType = requestFactoryType;
  }

  @Test
  void getReturnsRequestFactoryOfExpectedType() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
            .get(ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
  }

  @Test
  void getOfGeneralTypeReturnsRequestFactoryOfExpectedType() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(ClientHttpRequestFactory.class,
            ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
  }

  @Test
  void getOfSpecificTypeReturnsRequestFactoryOfExpectedType() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(this.requestFactoryType,
            ClientHttpRequestFactorySettings.DEFAULTS);
    assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getReturnsRequestFactoryWithConfiguredConnectTimeout() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
            .get(ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofSeconds(60)));
    assertThat(connectTimeout((T) requestFactory)).isEqualTo(Duration.ofSeconds(60).toMillis());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getReturnsRequestFactoryWithConfiguredReadTimeout() {
    ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
            .get(ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofSeconds(120)));
    assertThat(readTimeout((T) requestFactory)).isEqualTo(Duration.ofSeconds(120).toMillis());
  }

  @ParameterizedTest
  @CsvSource({ "GET", "POST" })
  void connectWithSslBundle(String httpMethod) throws Exception {
    TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
    Ssl ssl = new Ssl();
    ssl.setClientAuth(ClientAuth.NEED);
    ssl.setKeyPassword("password");
    ssl.setKeyStore("classpath:test.jks");
    ssl.setTrustStore("classpath:test.jks");
    webServerFactory.setSsl(ssl);
    WebServer webServer = webServerFactory.getWebServer(context -> context.addServlet("test", TestServlet.class)
            .addMapping("/"));
    try {
      webServer.start();
      int port = webServer.getPort();
      URI uri = new URI("https://localhost:%s".formatted(port));
      ClientHttpRequestFactory insecureRequestFactory = ClientHttpRequestFactories
              .get(ClientHttpRequestFactorySettings.DEFAULTS);
      ClientHttpRequest insecureRequest = insecureRequestFactory.createRequest(uri, HttpMethod.GET);
      assertThatExceptionOfType(SSLHandshakeException.class)
              .isThrownBy(() -> insecureRequest.execute().getBody());
      JksSslStoreDetails storeDetails = JksSslStoreDetails.forLocation("classpath:test.jks");
      JksSslStoreBundle stores = new JksSslStoreBundle(storeDetails, storeDetails);
      SslBundle sslBundle = SslBundle.of(stores, SslBundleKey.of("password"));
      ClientHttpRequestFactory secureRequestFactory = ClientHttpRequestFactories
              .get(ClientHttpRequestFactorySettings.DEFAULTS.withSslBundle(sslBundle));
      ClientHttpRequest secureRequest = secureRequestFactory.createRequest(uri, HttpMethod.valueOf(httpMethod));
      String secureResponse = StreamUtils.copyToString(secureRequest.execute().getBody(), StandardCharsets.UTF_8);
      assertThat(secureResponse).contains("Received " + httpMethod + " request to /");
    }
    finally {
      webServer.stop();
    }
  }

  protected abstract long connectTimeout(T requestFactory);

  protected abstract long readTimeout(T requestFactory);

  public static class TestServlet extends HttpServlet {

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
      res.getWriter().println("Received " + req.getMethod() + " request to " + req.getRequestURI());
    }

  }

}

