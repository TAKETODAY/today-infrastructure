/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.web.netty;

import cn.taketoday.annotation.config.web.WebMvcProperties;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.framework.web.netty.FastRequestThreadLocal;
import cn.taketoday.framework.web.netty.NettyChannelHandler;
import cn.taketoday.framework.web.netty.NettyChannelInitializer;
import cn.taketoday.framework.web.netty.NettyRequestConfig;
import cn.taketoday.framework.web.netty.NettyWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactory;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.PropertyMapper;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.multipart.MultipartConfig;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a netty web server.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 17:39
 */
@AutoConfiguration
@DisableAllDependencyInjection
@ConditionalOnWebApplication(type = Type.NETTY)
@EnableConfigurationProperties(ServerProperties.class)
public class NettyWebServerFactoryAutoConfiguration {

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @MissingBean(value = { NettyChannelHandler.class, DispatcherHandler.class })
  static NettyChannelHandler nettyChannelHandler(ApplicationContext context,
          WebMvcProperties webMvcProperties, NettyRequestConfig contextConfig) {
    NettyChannelHandler handler = new NettyChannelHandler(contextConfig, context);
    handler.setThrowExceptionIfNoHandlerFound(webMvcProperties.isThrowExceptionIfNoHandlerFound());
    handler.setEnableLoggingRequestDetails(webMvcProperties.isLogRequestDetails());
    return handler;
  }

  /**
   * Netty Server
   */
  @Component
  @ConditionalOnMissingBean
  static WebServerFactory nettyWebServerFactory(ServerProperties serverProperties,
          NettyChannelInitializer nettyChannelInitializer) {
    NettyWebServerFactory factory = new NettyWebServerFactory();
    factory.setNettyChannelInitializer(nettyChannelInitializer);

    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(serverProperties::getSsl).to(factory::setSsl);
    map.from(serverProperties::getPort).to(factory::setPort);
    map.from(serverProperties::getHttp2).to(factory::setHttp2);
    map.from(serverProperties::getAddress).to(factory::setAddress);
    map.from(serverProperties.getShutdown()).to(factory::setShutdown);
    map.from(serverProperties::getCompression).to(factory::setCompression);

    ServerProperties.Netty netty = serverProperties.getNetty();

    map.from(netty::getLoggingLevel).to(factory::setLoggingLevel);
    map.from(netty::getSocketChannel).to(factory::setSocketChannel);
    map.from(netty::getBossThreadCount).to(factory::setBossThreadCount);
    map.from(netty::getWorkThreadCount).to(factory::setWorkThreadCount);

    // replace context holder
    if (netty.isFastThreadLocal()) {
      RequestContextHolder.replaceContextHolder(new FastRequestThreadLocal());
    }

    return factory;
  }

  /**
   * Framework Channel Initializer
   *
   * @param channelHandler ChannelInboundHandler
   */
  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static NettyChannelInitializer nettyChannelInitializer(NettyChannelHandler channelHandler) {
    return new NettyChannelInitializer(channelHandler);
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static NettyRequestConfig nettyRequestConfig(MultipartConfig multipartConfig) {
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

}
