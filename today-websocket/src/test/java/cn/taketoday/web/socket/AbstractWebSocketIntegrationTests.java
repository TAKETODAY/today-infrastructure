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
import java.util.stream.Stream;

import cn.taketoday.context.Lifecycle;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.method.AnnotationHandlerFactory;
import cn.taketoday.web.socket.client.WebSocketClient;
import cn.taketoday.web.socket.client.support.NettyWebSocketClient;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import cn.taketoday.web.socket.server.support.DefaultHandshakeHandler;
import cn.taketoday.web.socket.server.support.NettyRequestUpgradeStrategy;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/7 21:35
 */
public abstract class AbstractWebSocketIntegrationTests {

  private static final Map<Class<?>, Class<?>> upgradeStrategyConfigTypes = Map.of(
          NettyTestServer.class, NettyUpgradeStrategyConfig.class);

  public static Stream<Arguments> argumentsFactory() {
    return Stream.of(arguments(
            named("Netty", new NettyTestServer()), named("Netty Client", new NettyWebSocketClient())
    ));
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

  protected AnnotationConfigApplicationContext wac;

  protected void setup(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
    this.server = server;
    this.webSocketClient = webSocketClient;

    logger.info("Setting up '%s', client=%s, server=%s".formatted(testInfo.getTestMethod().get().getName(),
            webSocketClient.getClass().getSimpleName(), server.getClass().getSimpleName()));

    this.wac = new AnnotationConfigApplicationContext();
    this.wac.register(getAnnotatedConfigClasses());

    wac.register(AnnotationHandlerFactory.class);
    wac.register(ParameterResolvingRegistry.class);
    wac.register(ReturnValueHandlerManager.class);

    this.wac.register(upgradeStrategyConfigTypes.get(this.server.getClass()));

    if (this.webSocketClient instanceof Lifecycle lifecycle) {
      lifecycle.start();
    }

    this.server.setup(wac);

    this.wac.refresh();

    this.server.start();
    logger.info("Setup complete.");
  }

  protected abstract Class<?>[] getAnnotatedConfigClasses();

  @AfterEach
  void teardown() throws Exception {
    logger.info("Tearing down '{}'.", server.getClass().getSimpleName());
    try {
      if (this.webSocketClient instanceof Lifecycle) {
        ((Lifecycle) this.webSocketClient).stop();
      }
    }
    catch (Throwable t) {
      logger.error("Failed to stop WebSocket client", t);
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

  static abstract class AbstractRequestUpgradeStrategyConfig {

    @Bean
    public DefaultHandshakeHandler handshakeHandler() {
      return new DefaultHandshakeHandler(requestUpgradeStrategy());
    }

    public abstract RequestUpgradeStrategy requestUpgradeStrategy();
  }

  @Configuration
  static class NettyUpgradeStrategyConfig extends AbstractRequestUpgradeStrategyConfig {

    @Override
    @Bean
    public RequestUpgradeStrategy requestUpgradeStrategy() {
      return new NettyRequestUpgradeStrategy(null);
    }
  }

}
