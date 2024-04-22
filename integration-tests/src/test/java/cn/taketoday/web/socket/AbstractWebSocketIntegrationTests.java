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

package cn.taketoday.web.socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import cn.taketoday.context.Lifecycle;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.method.AnnotationHandlerFactory;
import cn.taketoday.web.servlet.support.AnnotationConfigWebApplicationContext;
import cn.taketoday.web.socket.client.WebSocketClient;
import cn.taketoday.web.socket.client.standard.StandardWebSocketClient;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.web.socket.server.jetty.JettyRequestUpgradeStrategy;
import cn.taketoday.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import cn.taketoday.web.socket.server.standard.UndertowRequestUpgradeStrategy;
import cn.taketoday.web.socket.server.support.DefaultHandshakeHandler;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/7 21:35
 */
public abstract class AbstractWebSocketIntegrationTests {

  private static final Map<Class<?>, Class<?>> upgradeStrategyConfigTypes = Map.of(
          JettyWebSocketTestServer.class, JettyUpgradeStrategyConfig.class, //
          TomcatWebSocketTestServer.class, TomcatUpgradeStrategyConfig.class, //
          UndertowTestServer.class, UndertowUpgradeStrategyConfig.class);

  static Stream<Arguments> argumentsFactory() {
    return Stream.of(
            arguments(named("Tomcat", new TomcatWebSocketTestServer()), named("Standard", new StandardWebSocketClient())),
            arguments(named("Undertow", new UndertowTestServer()), named("Standard", new StandardWebSocketClient()))
    );
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] server = {0}, client = {1}")
  @MethodSource("argumentsFactory")
  protected @interface ParameterizedWebSocketTest {
  }

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected WebSocketTestServer server;

  protected WebSocketClient webSocketClient;

  protected AnnotationConfigWebApplicationContext wac;

  protected void setup(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
    this.server = server;
    this.webSocketClient = webSocketClient;

    logger.debug("Setting up '" + testInfo.getTestMethod().get().getName() + "', client=" +
            this.webSocketClient.getClass().getSimpleName() + ", server=" +
            this.server.getClass().getSimpleName());

    this.wac = new AnnotationConfigWebApplicationContext();
    this.wac.register(getAnnotatedConfigClasses());
    wac.register(AnnotationHandlerFactory.class);
    wac.register(ParameterResolvingRegistry.class);
    wac.register(ReturnValueHandlerManager.class);

    this.wac.register(upgradeStrategyConfigTypes.get(this.server.getClass()));

    if (this.webSocketClient instanceof Lifecycle) {
      ((Lifecycle) this.webSocketClient).start();
    }

    this.server.setup();
    this.server.deployConfig(this.wac);

    this.wac.setServletContext(this.server.getServletContext());
    this.wac.refresh();

    this.server.start();
  }

  protected abstract Class<?>[] getAnnotatedConfigClasses();

  @AfterEach
  void teardown() throws Exception {
    try {
      if (this.webSocketClient instanceof Lifecycle) {
        ((Lifecycle) this.webSocketClient).stop();
      }
    }
    catch (Throwable t) {
      logger.error("Failed to stop WebSocket client", t);
    }
    try {
      this.server.undeployConfig();
    }
    catch (Throwable t) {
      logger.error("Failed to undeploy application config", t);
    }
    try {
      this.server.stop();
    }
    catch (Throwable t) {
      logger.error("Failed to stop server", t);
    }
    try {
      this.wac.close();
    }
    catch (Throwable t) {
      logger.error("Failed to close WebApplicationContext", t);
    }
  }

  protected String getWsBaseUrl() {
    return "ws://localhost:" + this.server.getPort();
  }

  protected CompletableFuture<WebSocketSession> execute(WebSocketHandler clientHandler, String endpointPath) {
    return this.webSocketClient.execute(clientHandler, getWsBaseUrl() + endpointPath);
  }

  static abstract class AbstractRequestUpgradeStrategyConfig {

    @Bean
    public DefaultHandshakeHandler handshakeHandler() {
      return new DefaultHandshakeHandler(requestUpgradeStrategy());
    }

    public abstract RequestUpgradeStrategy requestUpgradeStrategy();
  }

  @Configuration(proxyBeanMethods = false)
  static class JettyUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

    @Override
    @Bean
    public RequestUpgradeStrategy requestUpgradeStrategy() {
      return new JettyRequestUpgradeStrategy();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class TomcatUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

    @Override
    @Bean
    public RequestUpgradeStrategy requestUpgradeStrategy() {
      return new TomcatRequestUpgradeStrategy();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class UndertowUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

    @Override
    @Bean
    public RequestUpgradeStrategy requestUpgradeStrategy() {
      return new UndertowRequestUpgradeStrategy();
    }
  }

}
