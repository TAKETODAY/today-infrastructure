/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.socket.config;

import java.util.List;

import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.util.DataSize;
import infra.web.config.WebMvcConfigurationSupport;
import infra.web.socket.server.RequestUpgradeStrategy;
import infra.web.socket.server.support.DefaultHandshakeHandler;
import infra.web.socket.server.support.NettyRequestUpgradeStrategy;
import infra.web.socket.server.support.WebSocketHandlerMapping;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;

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

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(io.netty.handler.codec.http.HttpMethod.class)
  public static class NettyWebSocket {

    @Component
    @ConditionalOnMissingBean
    public static RequestUpgradeStrategy nettyRequestUpgradeStrategy(WebSocketDecoderConfig decoderConfig) {
      return new NettyRequestUpgradeStrategy(decoderConfig);
    }

    @Component
    @ConditionalOnMissingBean
    public static WebSocketDecoderConfig webSocketDecoderConfig() {
      return WebSocketDecoderConfig.newBuilder()
              .maxFramePayloadLength(DataSize.ofKilobytes(512).bytes().intValue())
              .expectMaskedFrames(true)
              .allowMaskMismatch(false)
              .allowExtensions(false)
              .closeOnProtocolViolation(true)
              .withUTF8Validator(true)
              .build();
    }

  }

}
