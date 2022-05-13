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

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.EnableDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.properties.Props;
import cn.taketoday.framework.web.embedded.netty.NettyReactiveWebServerFactory;
import cn.taketoday.lang.Singleton;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.socket.WebSocketHandlerRegistry;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/13 17:34
 */
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class NettyConfiguration {

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @MissingBean(value = ReactiveChannelHandler.class)
  ReactiveChannelHandler reactiveChannelHandler(
          WebApplicationContext context,
          NettyDispatcher nettyDispatcher,
          NettyRequestConfig contextConfig,
          @Autowired(required = false) WebSocketHandlerRegistry registry) {
    if (registry != null) {
      return new WebSocketReactiveChannelHandler(nettyDispatcher, contextConfig, context);
    }
    return new ReactiveChannelHandler(nettyDispatcher, contextConfig, context);
  }

  @Singleton
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyWebSocketHandlerAdapter webSocketHandlerAdapter() {
    return new NettyWebSocketHandlerAdapter();
  }

  @MissingBean(value = DispatcherHandler.class)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  DispatcherHandler dispatcherHandler(WebApplicationContext webApplicationContext) {
    return new DispatcherHandler(webApplicationContext);
  }

  /**
   * Default {@link NettyWebServer} object
   * <p>
   * framework will auto inject properties start with 'server.' or 'server.netty.'
   * </p>
   *
   * @return returns a default {@link NettyWebServer} object
   */
  @MissingBean
  @EnableDependencyInjection
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Props("server.netty")
  NettyReactiveWebServerFactory nettyWebServer() {
    return new NettyReactiveWebServerFactory();
  }

  /**
   * Framework Channel Initializer
   *
   * @param channelHandler ChannelInboundHandler
   */
  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyServerInitializer nettyServerInitializer(ReactiveChannelHandler channelHandler) {
    return new NettyServerInitializer(channelHandler);
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  NettyRequestConfig nettyRequestContextConfig() {
    return new NettyRequestConfig();
  }

}
