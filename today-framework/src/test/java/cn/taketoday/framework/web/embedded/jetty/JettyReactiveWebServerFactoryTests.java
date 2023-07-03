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

package cn.taketoday.framework.web.embedded.jetty;

import org.awaitility.Awaitility;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.net.ConnectException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Arrays;

import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.http.client.reactive.JettyResourceFactory;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyReactiveWebServerFactory} and {@link JettyWebServer}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
class JettyReactiveWebServerFactoryTests extends AbstractReactiveWebServerFactoryTests {

  @Override
  protected JettyReactiveWebServerFactory getFactory() {
    return new JettyReactiveWebServerFactory(0);
  }

  @Test
  @Override
  @Disabled("Jetty 11 does not support User-Agent-based compression")
  protected void noCompressionForUserAgent() {

  }

  @Test
  void setNullServerCustomizersShouldThrowException() {
    JettyReactiveWebServerFactory factory = getFactory();
    assertThatIllegalArgumentException().isThrownBy(() -> factory.setServerCustomizers(null))
            .withMessageContaining("Customizers must not be null");
  }

  @Test
  void addNullServerCustomizersShouldThrowException() {
    JettyReactiveWebServerFactory factory = getFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.addServerCustomizers((JettyServerCustomizer[]) null))
            .withMessageContaining("Customizers must not be null");
  }

  @Test
  void jettyCustomizersShouldBeInvoked() {
    HttpHandler handler = mock(HttpHandler.class);
    JettyReactiveWebServerFactory factory = getFactory();
    JettyServerCustomizer[] configurations = new JettyServerCustomizer[4];
    Arrays.setAll(configurations, (i) -> mock(JettyServerCustomizer.class));
    factory.setServerCustomizers(Arrays.asList(configurations[0], configurations[1]));
    factory.addServerCustomizers(configurations[2], configurations[3]);
    this.webServer = factory.getWebServer(handler);
    InOrder ordered = inOrder((Object[]) configurations);
    for (JettyServerCustomizer configuration : configurations) {
      ordered.verify(configuration).customize(any(Server.class));
    }
  }

  @Test
  void specificIPAddressNotReverseResolved() throws Exception {
    JettyReactiveWebServerFactory factory = getFactory();
    InetAddress localhost = InetAddress.getLocalHost();
    factory.setAddress(InetAddress.getByAddress(localhost.getAddress()));
    this.webServer = factory.getWebServer(mock(HttpHandler.class));
    this.webServer.start();
    Connector connector = ((JettyWebServer) this.webServer).getServer().getConnectors()[0];
    assertThat(((ServerConnector) connector).getHost()).isEqualTo(localhost.getHostAddress());
  }

  @Test
  void useForwardedHeaders() {
    JettyReactiveWebServerFactory factory = getFactory();
    factory.setUseForwardHeaders(true);
    assertForwardHeaderIsUsed(factory);
  }

  @Test
  void useServerResources() throws Exception {
    JettyResourceFactory resourceFactory = new JettyResourceFactory();
    resourceFactory.afterPropertiesSet();
    JettyReactiveWebServerFactory factory = getFactory();
    factory.setResourceFactory(resourceFactory);
    JettyWebServer webServer = (JettyWebServer) factory.getWebServer(new EchoHandler());
    webServer.start();
    Connector connector = webServer.getServer().getConnectors()[0];
    assertThat(connector.getByteBufferPool()).isEqualTo(resourceFactory.getByteBufferPool());
    assertThat(connector.getExecutor()).isEqualTo(resourceFactory.getExecutor());
    assertThat(connector.getScheduler()).isEqualTo(resourceFactory.getScheduler());
  }

  @Test
  void whenServerIsShuttingDownGracefullyThenNewConnectionsCannotBeMade() {
    JettyReactiveWebServerFactory factory = getFactory();
    factory.setShutdown(Shutdown.GRACEFUL);
    BlockingHandler blockingHandler = new BlockingHandler();
    this.webServer = factory.getWebServer(blockingHandler);
    this.webServer.start();
    WebClient webClient = getWebClient(this.webServer.getPort()).build();
    this.webServer.shutDownGracefully((result) -> {
    });
    Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> {
      blockingHandler.stopBlocking();
      try {
        webClient.get().retrieve().toBodilessEntity().block();
        return false;
      }
      catch (RuntimeException ex) {
        return ex.getCause() instanceof ConnectException;
      }
    });
    this.webServer.stop();
  }

  @Test
  void shouldApplyMaxConnections() {
    JettyReactiveWebServerFactory factory = getFactory();
    factory.setMaxConnections(1);
    this.webServer = factory.getWebServer(new EchoHandler());
    Server server = ((JettyWebServer) this.webServer).getServer();
    ConnectionLimit connectionLimit = server.getBean(ConnectionLimit.class);
    assertThat(connectionLimit).isNotNull();
    assertThat(connectionLimit.getMaxConnections()).isOne();
  }

  //  @Override
  protected String startedLogMessage() {
    return ((JettyWebServer) this.webServer).getStartedLogMessage();
  }

}
