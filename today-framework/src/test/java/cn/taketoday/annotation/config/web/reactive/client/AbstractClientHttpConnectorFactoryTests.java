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

package cn.taketoday.annotation.config.web.reactive.client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.annotation.config.web.RandomPortWebServerConfig;
import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.jks.JksSslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreDetails;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.web.server.context.ConfigurableWebServerApplicationContext;
import cn.taketoday.web.server.context.WebServerApplicationContext;
import cn.taketoday.web.server.WebServer;
import cn.taketoday.web.reactive.function.client.WebClient;
import cn.taketoday.web.reactive.function.client.WebClientRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Abstract base class for {@link ClientHttpConnectorFactory} tests.
 *
 * @author Phillip Webb
 */
@Disabled
abstract class AbstractClientHttpConnectorFactoryTests {

  @Test
  @Disabled
  void insecureConnection() {
    try (ConfigurableApplicationContext context = Application.forBuilder(RandomPortWebServerConfig.class,
                    WebMvcAutoConfiguration.class)
            .type(ApplicationType.NETTY_WEB)
            .run()) {
      WebServer webServer = context.unwrap(ConfigurableWebServerApplicationContext.class).getWebServer();
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
  }

  @Test
  @Disabled
  void secureConnection() throws Exception {
    try (ConfigurableApplicationContext context = Application.forBuilder(
                    RandomPortWebServerConfig.class, WebMvcAutoConfiguration.class)
            .type(ApplicationType.NETTY_WEB)
            .run("--server.netty.ssl.enabled=false",
                    "--server.netty.ssl.privateKey=classpath:test-key.pem",
                    "--server.netty.ssl.keyPassword=password",
                    "--server.netty.ssl.publicKey=classpath:test-key.pem")) {
      WebServer webServer = context.unwrap(WebServerApplicationContext.class).getWebServer();
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
//              .clientConnector(getFactory().createClientHttpConnector(sslBundle))
              .clientConnector(getFactory().createClientHttpConnector())
              .build();
      String secureBody = secureWebClient.get()
              .uri(url)
              .exchangeToMono((response) -> response.bodyToMono(String.class))
              .block();
      assertThat(secureBody).contains("HTTP Status 404 – Not Found");
    }
  }

  protected abstract ClientHttpConnectorFactory<?> getFactory();

}
