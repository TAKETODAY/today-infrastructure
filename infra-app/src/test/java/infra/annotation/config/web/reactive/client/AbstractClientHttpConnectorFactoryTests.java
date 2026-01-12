/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.annotation.config.web.reactive.client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import infra.annotation.config.web.RandomPortWebServerConfig;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.app.Application;
import infra.app.ApplicationType;
import infra.context.ConfigurableApplicationContext;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundleKey;
import infra.core.ssl.jks.JksSslStoreBundle;
import infra.core.ssl.jks.JksSslStoreDetails;
import infra.web.client.reactive.WebClient;
import infra.web.client.reactive.WebClientRequestException;
import infra.web.server.WebServer;
import infra.web.server.context.ConfigurableWebServerApplicationContext;
import infra.web.server.context.WebServerApplicationContext;

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
    try (ConfigurableApplicationContext context = Application.forBuilder(RandomPortWebServerConfig.class, WebMvcAutoConfiguration.class)
            .type(ApplicationType.WEB)
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
            .type(ApplicationType.WEB)
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
