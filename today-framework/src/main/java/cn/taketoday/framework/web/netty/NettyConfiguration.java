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

import cn.taketoday.annotation.config.web.WebMvcProperties;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.EnableDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyWebServer;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.multipart.MultipartConfig;
import cn.taketoday.web.socket.server.support.WebSocketHandlerMapping;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
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
  @MissingBean(value = { NettyChannelHandler.class, DispatcherHandler.class })
  NettyChannelHandler nettyChannelHandler(ApplicationContext context,
          WebMvcProperties webMvcProperties, NettyRequestConfig contextConfig) {
    NettyChannelHandler handler = new NettyChannelHandler(contextConfig, context);
    handler.setThrowExceptionIfNoHandlerFound(webMvcProperties.isThrowExceptionIfNoHandlerFound());
    handler.setEnableLoggingRequestDetails(webMvcProperties.isLogRequestDetails());
    return handler;
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
  NettyChannelInitializer nettyChannelInitializer(NettyChannelHandler channelHandler) {
    return new NettyChannelInitializer(channelHandler);
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyRequestConfig nettyRequestConfig(MultipartConfig multipartConfig) {
    String location = multipartConfig.getLocation();
    DefaultHttpDataFactory factory = new DefaultHttpDataFactory(location != null);
    factory.setMaxLimit(multipartConfig.getMaxRequestSize().toBytes());
    if (location != null) {
      factory.setBaseDir(location);
    }
    return new NettyRequestConfig(factory);
  }

  @Component
  @ConditionalOnProperty(prefix = "server.multipart", name = "enabled", matchIfMissing = true)
  @ConfigurationProperties(prefix = "server.multipart", ignoreUnknownFields = false)
  static MultipartConfig multipartConfig() {
    return new MultipartConfig();
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
    @MissingBean(value = { NettyChannelHandler.class, DispatcherHandler.class })
    NettyChannelHandler webSocketReactiveChannelHandler(ApplicationContext context,
            NettyRequestConfig contextConfig, @Nullable WebSocketHandlerMapping registry) {
      if (registry != null) {
        return new WebSocketNettyChannelHandler(contextConfig, context);
      }
      return new NettyChannelHandler(contextConfig, context);
    }

    @Component
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    NettyRequestUpgradeStrategy nettyRequestUpgradeStrategy() {
      return new NettyRequestUpgradeStrategy();
    }

  }

}
