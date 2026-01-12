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

package infra.web.socket.config;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.context.annotation.Configuration;
import infra.stereotype.Component;
import infra.web.config.annotation.WebMvcConfigurationSupport;
import infra.web.socket.server.RequestUpgradeStrategy;
import infra.web.socket.server.support.DefaultHandshakeHandler;
import infra.web.socket.server.support.WebSocketHandlerMapping;

/**
 * A Configuration that detects implementations of {@link WebSocketConfigurer} in
 * Infra configuration and invokes them in order to configure WebSocket request handling.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
public class WebSocketConfiguration {

  @Component
  public static WebSocketHandlerMapping webSocketHandlerMapping(List<WebSocketConfigurer> configurers,
          @Nullable WebMvcConfigurationSupport support, @Nullable RequestUpgradeStrategy requestUpgradeStrategy) {
    DefaultWebSocketHandlerRegistry registry = new DefaultWebSocketHandlerRegistry();
    if (requestUpgradeStrategy != null) {
      registry.setHandshakeHandler(new DefaultHandshakeHandler(requestUpgradeStrategy));
    }

    for (WebSocketConfigurer configurer : configurers) {
      configurer.registerWebSocketHandlers(registry);
    }

    WebSocketHandlerMapping handlerMapping = registry.getHandlerMapping();
    if (support != null) {
      support.initHandlerMapping(handlerMapping);
    }
    return handlerMapping;
  }

}
