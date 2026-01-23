/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.socket;

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

import infra.context.Lifecycle;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.server.netty.NettyRequestUpgradeStrategy;
import infra.web.socket.client.WebSocketClient;
import infra.web.socket.client.support.NettyWebSocketClient;
import infra.web.socket.server.RequestUpgradeStrategy;
import infra.web.socket.server.support.DefaultHandshakeHandler;

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

  protected AnnotationConfigApplicationContext ctx;

  protected void setup(WebSocketTestServer server, WebSocketClient webSocketClient, TestInfo testInfo) throws Exception {
    this.server = server;
    this.webSocketClient = webSocketClient;

    logger.info("Setting up '%s', client=%s, server=%s".formatted(testInfo.getTestMethod().get().getName(),
            webSocketClient.getClass().getSimpleName(), server.getClass().getSimpleName()));

    this.ctx = new AnnotationConfigApplicationContext();
    this.ctx.register(getAnnotatedConfigClasses());

    ctx.register(ParameterResolvingRegistry.class);
    ctx.register(ReturnValueHandlerManager.class);

    this.ctx.register(upgradeStrategyConfigTypes.get(this.server.getClass()));

    if (this.webSocketClient instanceof Lifecycle lifecycle) {
      lifecycle.start();
    }

    this.server.setup(ctx);

    this.ctx.refresh();

    this.server.start(ctx);
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
      this.ctx.close();
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
      return new NettyRequestUpgradeStrategy();
    }

  }

}
