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

package cn.taketoday.annotation.config.web.netty;

import java.util.List;

import cn.taketoday.annotation.config.web.ErrorMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.WebMvcProperties;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.annotation.config.AutoConfigureOrder;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication.Type;
import cn.taketoday.web.server.ChannelWebServerFactory;
import cn.taketoday.web.server.ServerProperties;
import cn.taketoday.web.server.ServerProperties.Netty.Multipart;
import cn.taketoday.web.server.Ssl;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.framework.web.netty.NettyChannelHandler;
import cn.taketoday.framework.web.netty.NettyRequestConfig;
import cn.taketoday.framework.web.netty.NettyWebServerFactory;
import cn.taketoday.web.server.error.SendErrorHandler;
import cn.taketoday.framework.web.netty.ServerBootstrapCustomizer;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a netty web server.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 17:39
 */
@Lazy
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = Type.NETTY)
@EnableConfigurationProperties(ServerProperties.class)
@DisableDIAutoConfiguration(after = ErrorMvcAutoConfiguration.class)
public class NettyWebServerFactoryAutoConfiguration {

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @MissingBean(value = { NettyChannelHandler.class, DispatcherHandler.class })
  static NettyChannelHandler nettyChannelHandler(ApplicationContext context,
          WebMvcProperties webMvcProperties, NettyRequestConfig contextConfig) {
    NettyChannelHandler handler = new NettyChannelHandler(contextConfig, context);
    handler.setThrowExceptionIfNoHandlerFound(webMvcProperties.throwExceptionIfNoHandlerFound);
    handler.setEnableLoggingRequestDetails(webMvcProperties.logRequestDetails);
    return handler;
  }

  /**
   * Netty Server
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static ChannelWebServerFactory nettyWebServerFactory(ServerProperties serverProperties, @Nullable SslBundles sslBundles,
          @Nullable List<ServerBootstrapCustomizer> customizers, @Nullable ApplicationTemp applicationTemp) {
    NettyWebServerFactory factory = new NettyWebServerFactory();

    serverProperties.applyTo(factory, sslBundles, applicationTemp);

    factory.applyFrom(serverProperties.netty);
    factory.setBootstrapCustomizers(customizers);
    return factory;
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static NettyRequestConfig nettyRequestConfig(ServerProperties server, SendErrorHandler sendErrorHandler) {
    var multipart = server.netty.multipart;
    var factory = createHttpDataFactory(multipart);
    if (multipart.maxFieldSize != null) {
      factory.setMaxLimit(multipart.maxFieldSize.toBytes());
    }
    if (StringUtils.hasText(multipart.baseDir)) {
      factory.setBaseDir(multipart.baseDir);
    }
    factory.setDeleteOnExit(multipart.deleteOnExit);
    return NettyRequestConfig.forBuilder()
            .httpDataFactory(factory)
            .sendErrorHandler(sendErrorHandler)
            .secure(Ssl.isEnabled(server.ssl))
            .build();
  }

  private static DefaultHttpDataFactory createHttpDataFactory(Multipart multipart) {
    if (multipart.mixedMode) {
      if (multipart.fieldSizeThreshold != null) {
        return new DefaultHttpDataFactory(multipart.fieldSizeThreshold.toBytes(), multipart.charset);
      }
      return new DefaultHttpDataFactory();
    }
    else {
      return new DefaultHttpDataFactory(StringUtils.hasText(multipart.baseDir), multipart.charset);
    }
  }

}
