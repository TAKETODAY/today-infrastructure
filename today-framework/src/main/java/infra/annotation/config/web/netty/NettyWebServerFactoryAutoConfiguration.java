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

package infra.annotation.config.web.netty;

import java.util.List;

import infra.annotation.ConditionalOnWebApplication;
import infra.annotation.ConditionalOnWebApplication.Type;
import infra.annotation.config.web.ErrorMvcAutoConfiguration;
import infra.annotation.config.web.WebMvcProperties;
import infra.beans.factory.config.BeanDefinition;
import infra.context.ApplicationContext;
import infra.context.annotation.Lazy;
import infra.context.annotation.Role;
import infra.context.annotation.config.AutoConfigureOrder;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.ApplicationTemp;
import infra.core.Ordered;
import infra.core.ssl.SslBundles;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.StringUtils;
import infra.web.server.ChannelWebServerFactory;
import infra.web.server.ServerProperties;
import infra.web.server.ServerProperties.Netty.Multipart;
import infra.web.server.Ssl;
import infra.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import infra.web.server.error.SendErrorHandler;
import infra.web.server.support.ChannelConfigurer;
import infra.web.server.support.NettyChannelHandler;
import infra.web.server.support.NettyRequestConfig;
import infra.web.server.support.NettyWebServerFactory;
import infra.web.server.support.ServerBootstrapCustomizer;
import infra.web.socket.server.support.WsNettyChannelHandler;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;

import static infra.web.server.ChannelWebServerFactory.CHANNEL_HANDLER_BEAN_NAME;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a netty web server.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 17:39
 */
@Lazy
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = Type.NETTY)
@EnableConfigurationProperties(ServerProperties.class)
@DisableDIAutoConfiguration(after = ErrorMvcAutoConfiguration.class)
public class NettyWebServerFactoryAutoConfiguration {

  private NettyWebServerFactoryAutoConfiguration() {
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static WebServerFactoryCustomizerBeanPostProcessor webServerFactoryCustomizerBeanPostProcessor() {
    return new WebServerFactoryCustomizerBeanPostProcessor();
  }

  @Component(CHANNEL_HANDLER_BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(name = CHANNEL_HANDLER_BEAN_NAME)
  public static NettyChannelHandler nettyChannelHandler(ApplicationContext context,
          WebMvcProperties webMvcProperties, NettyRequestConfig requestConfig) {
    NettyChannelHandler handler = createChannelHandler(context, requestConfig, context.getClassLoader());
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
  public static ChannelWebServerFactory nettyWebServerFactory(ServerProperties serverProperties,
          @Nullable ChannelConfigurer channelConfigurer, @Nullable SslBundles sslBundles,
          @Nullable List<ServerBootstrapCustomizer> customizers, @Nullable ApplicationTemp applicationTemp) {
    NettyWebServerFactory factory = new NettyWebServerFactory();

    serverProperties.applyTo(factory, sslBundles, applicationTemp);

    factory.applyFrom(serverProperties.netty);
    factory.setBootstrapCustomizers(customizers);
    factory.setChannelConfigurer(channelConfigurer);
    return factory;
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static NettyRequestConfig nettyRequestConfig(ServerProperties server, SendErrorHandler sendErrorHandler) {
    var multipart = server.netty.multipart;
    var factory = createHttpDataFactory(multipart);
    if (multipart.maxFieldSize != null) {
      factory.setMaxLimit(multipart.maxFieldSize.toBytes());
    }
    if (StringUtils.hasText(multipart.baseDir)) {
      factory.setBaseDir(multipart.baseDir);
    }
    factory.setDeleteOnExit(multipart.deleteOnExit);
    return NettyRequestConfig.forBuilder(Ssl.isEnabled(server.ssl))
            .httpDataFactory(factory)
            .headersFactory(DefaultHttpHeadersFactory.headersFactory()
                    .withValidation(server.netty.validateHeaders))
            .sendErrorHandler(sendErrorHandler)
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

  private static NettyChannelHandler createChannelHandler(ApplicationContext context,
          NettyRequestConfig requestConfig, @Nullable ClassLoader classLoader) {
    if (ClassUtils.isPresent("infra.web.socket.server.support.WsNettyChannelHandler", classLoader)) {
      return Ws.createChannelHandler(context, requestConfig);
    }
    return new NettyChannelHandler(requestConfig, context);
  }

  static class Ws {
    private static NettyChannelHandler createChannelHandler(ApplicationContext context, NettyRequestConfig requestConfig) {
      return new WsNettyChannelHandler(requestConfig, context);
    }
  }

}
