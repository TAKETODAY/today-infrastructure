/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.netty;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.EnableDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyWebServer;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.stereotype.Singleton;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.socket.WebSocketHandlerMapping;
import reactor.core.publisher.Mono;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/13 17:34
 */
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class NettyConfiguration {

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @MissingBean(value = { ReactiveChannelHandler.class, DispatcherHandler.class })
  ReactiveChannelHandler reactiveChannelHandler(ApplicationContext context, NettyRequestConfig contextConfig) {
    return new ReactiveChannelHandler(contextConfig, context);
  }

  /**
   * Default {@link ReactorNettyWebServer} object
   * <p>
   * framework will auto inject properties start with 'server.' or 'server.netty.'
   * </p>
   *
   * @return returns a default {@link ReactorNettyWebServer} object
   */
  @MissingBean
  @EnableDependencyInjection
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyWebServerFactory nettyWebServerFactory(NettyChannelInitializer nettyChannelInitializer) {
    NettyWebServerFactory factory = new NettyWebServerFactory();
    factory.setNettyChannelInitializer(nettyChannelInitializer);
    return factory;
  }

  /**
   * Framework Channel Initializer
   *
   * @param channelHandler ChannelInboundHandler
   */
  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyChannelInitializer nettyChannelInitializer(ReactiveChannelHandler channelHandler) {
    return new NettyChannelInitializer(channelHandler);
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyRequestConfig nettyRequestContextConfig() {
    return new NettyRequestConfig();
  }

  @Component
  HttpHandler httpHandler() {
    return (request, response) -> Mono.empty();
  }

  @DisableAllDependencyInjection
  @ConditionalOnClass(WebSocketHandlerMapping.class)
  @Configuration(proxyBeanMethods = false)
  static class NettyWebSocketConfig {

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @MissingBean(value = { ReactiveChannelHandler.class, DispatcherHandler.class })
    ReactiveChannelHandler webSocketReactiveChannelHandler(ApplicationContext context,
            NettyRequestConfig contextConfig, @Nullable WebSocketHandlerMapping registry) {
      if (registry != null) {
        return new WebSocketReactiveChannelHandler(contextConfig, context);
      }
      return new ReactiveChannelHandler(contextConfig, context);
    }

    @Singleton
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    NettyWebSocketHandlerAdapter webSocketHandlerAdapter() {
      return new NettyWebSocketHandlerAdapter();
    }

  }

}
