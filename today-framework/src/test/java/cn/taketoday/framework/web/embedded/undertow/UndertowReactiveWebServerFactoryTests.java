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

package cn.taketoday.framework.web.embedded.undertow;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;

import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.web.reactive.function.BodyInserters;
import cn.taketoday.web.reactive.function.client.WebClient;
import cn.taketoday.web.reactive.function.client.WebClientResponseException.ServiceUnavailable;
import io.undertow.Undertow;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UndertowReactiveWebServerFactory} and {@link UndertowWebServer}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
class UndertowReactiveWebServerFactoryTests extends AbstractReactiveWebServerFactoryTests {

  @TempDir
  File tempDir;

  @Override
  protected UndertowReactiveWebServerFactory getFactory() {
    return new UndertowReactiveWebServerFactory(0);
  }

  @Test
  void setNullBuilderCustomizersShouldThrowException() {
    UndertowReactiveWebServerFactory factory = getFactory();
    assertThatIllegalArgumentException().isThrownBy(() -> factory.setBuilderCustomizers(null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void addNullBuilderCustomizersShouldThrowException() {
    UndertowReactiveWebServerFactory factory = getFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.addBuilderCustomizers((UndertowBuilderCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void builderCustomizersShouldBeInvoked() {
    UndertowReactiveWebServerFactory factory = getFactory();
    HttpHandler handler = mock(HttpHandler.class);
    UndertowBuilderCustomizer[] customizers = new UndertowBuilderCustomizer[4];
    Arrays.setAll(customizers, (i) -> mock(UndertowBuilderCustomizer.class));
    factory.setBuilderCustomizers(Arrays.asList(customizers[0], customizers[1]));
    factory.addBuilderCustomizers(customizers[2], customizers[3]);
    this.webServer = factory.getWebServer(handler);
    InOrder ordered = inOrder((Object[]) customizers);
    for (UndertowBuilderCustomizer customizer : customizers) {
      ordered.verify(customizer).customize(any(Undertow.Builder.class));
    }
  }

  @Test
  void useForwardedHeaders() {
    UndertowReactiveWebServerFactory factory = getFactory();
    factory.setUseForwardHeaders(true);
    assertForwardHeaderIsUsed(factory);
  }

  @Test
  void accessLogCanBeEnabled() {
    testAccessLog(null, null, "access_log.log");
  }

  @Test
  void accessLogCanBeCustomized() {
    testAccessLog("my_access.", "logz", "my_access.logz");
  }

  @Test
  void whenServerIsShuttingDownGracefullyThenNewConnectionsAreRejectedWithServiceUnavailable() {
    UndertowReactiveWebServerFactory factory = getFactory();
    factory.setShutdown(Shutdown.GRACEFUL);
    BlockingHandler blockingHandler = new BlockingHandler();
    this.webServer = factory.getWebServer(blockingHandler);
    this.webServer.start();
    this.webServer.shutDownGracefully((result) -> {
    });
    WebClient webClient = getWebClient(this.webServer.getPort()).build();
    Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> {
      blockingHandler.stopBlocking();
      try {
        webClient.get().retrieve().toBodilessEntity().block();
        return false;
      }
      catch (RuntimeException ex) {
        return ex instanceof ServiceUnavailable;
      }
    });
    this.webServer.stop();
  }

  private void testAccessLog(String prefix, String suffix, String expectedFile) {
    UndertowReactiveWebServerFactory factory = getFactory();
    factory.setAccessLogEnabled(true);
    factory.setAccessLogPrefix(prefix);
    factory.setAccessLogSuffix(suffix);
    File accessLogDirectory = this.tempDir;
    factory.setAccessLogDirectory(accessLogDirectory);
    assertThat(accessLogDirectory).isEmptyDirectory();
    this.webServer = factory.getWebServer(new EchoHandler());
    this.webServer.start();
    WebClient client = getWebClient(this.webServer.getPort()).build();
    Mono<String> result = client.post()
            .uri("/test")
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue("Hello World"))
            .retrieve()
            .bodyToMono(String.class);
    assertThat(result.block(Duration.ofSeconds(30))).isEqualTo("Hello World");
    File accessLog = new File(accessLogDirectory, expectedFile);
    awaitFile(accessLog);
    assertThat(accessLogDirectory.listFiles()).contains(accessLog);
  }

  private void awaitFile(File file) {
    Awaitility.waitAtMost(Duration.ofSeconds(10)).until(file::exists, is(true));
  }

  @Override
  protected String startedLogMessage() {
    return ((UndertowWebServer) this.webServer).getStartLogMessage();
  }

  @Override
  protected void addConnector(int port, AbstractReactiveWebServerFactory factory) {
    ((UndertowReactiveWebServerFactory) factory)
            .addBuilderCustomizers((builder) -> builder.addHttpListener(port, "0.0.0.0"));
  }

}
