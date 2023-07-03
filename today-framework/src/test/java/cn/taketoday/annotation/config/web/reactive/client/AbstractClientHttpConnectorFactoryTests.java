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

package cn.taketoday.annotation.config.web.reactive.client;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.jks.JksSslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreDetails;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.Ssl.ClientAuth;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.web.reactive.function.client.WebClient;
import cn.taketoday.web.reactive.function.client.WebClientRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Abstract base class for {@link ClientHttpConnectorFactory} tests.
 *
 * @author Phillip Webb
 */
abstract class AbstractClientHttpConnectorFactoryTests {

  @Test
  void insecureConnection() {
    TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
    WebServer webServer = webServerFactory.getWebServer();
    try {
      webServer.start();
      int port = webServer.getPort();
      String url = "http://localhost:%s".formatted(port);
      WebClient insecureWebClient = WebClient.builder()
              .clientConnector(getFactory().createClientHttpConnector())
              .build();
      String insecureBody = insecureWebClient.get()
              .uri(url)
              .exchangeToMono((response) -> response.bodyToMono(String.class))
              .block();
      assertThat(insecureBody).contains("HTTP Status 404 – Not Found");
    }
    finally {
      webServer.stop();
    }
  }

  @Test
  void secureConnection() throws Exception {
    TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
    Ssl ssl = new Ssl();
    ssl.setClientAuth(ClientAuth.NEED);
    ssl.setKeyPassword("password");
    ssl.setKeyStore("classpath:test.jks");
    ssl.setTrustStore("classpath:test.jks");
    webServerFactory.setSsl(ssl);
    WebServer webServer = webServerFactory.getWebServer();
    try {
      webServer.start();
      int port = webServer.getPort();
      String url = "https://localhost:%s".formatted(port);
      WebClient insecureWebClient = WebClient.builder()
              .clientConnector(getFactory().createClientHttpConnector())
              .build();
      assertThatExceptionOfType(WebClientRequestException.class).isThrownBy(() -> insecureWebClient.get()
              .uri(url)
              .exchangeToMono((response) -> response.bodyToMono(String.class))
              .block());
      JksSslStoreDetails storeDetails = JksSslStoreDetails.forLocation("classpath:test.jks");
      JksSslStoreBundle stores = new JksSslStoreBundle(storeDetails, storeDetails);
      SslBundle sslBundle = SslBundle.of(stores, SslBundleKey.of("password"));
      WebClient secureWebClient = WebClient.builder()
              .clientConnector(getFactory().createClientHttpConnector(sslBundle))
              .build();
      String secureBody = secureWebClient.get()
              .uri(url)
              .exchangeToMono((response) -> response.bodyToMono(String.class))
              .block();
      assertThat(secureBody).contains("HTTP Status 404 – Not Found");
    }
    finally {
      webServer.stop();
    }
  }

  protected abstract ClientHttpConnectorFactory<?> getFactory();

}
