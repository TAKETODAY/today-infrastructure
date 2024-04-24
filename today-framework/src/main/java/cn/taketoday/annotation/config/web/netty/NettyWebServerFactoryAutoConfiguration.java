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
import cn.taketoday.framework.web.netty.NettyChannelHandler;
import cn.taketoday.framework.web.netty.NettyChannelInitializer;
import cn.taketoday.framework.web.netty.NettyRequestConfig;
import cn.taketoday.framework.web.netty.NettyWebServerFactory;
import cn.taketoday.framework.web.netty.SSLNettyChannelInitializer;
import cn.taketoday.framework.web.netty.SendErrorHandler;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.DispatcherHandler;
import io.netty.handler.codec.http.HttpDecoderConfig;
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
  static WebServerFactory nettyWebServerFactory(ServerProperties serverProperties,
          NettyChannelInitializer nettyChannelInitializer, @Nullable SslBundles sslBundles,
          @Nullable List<ServerBootstrapCustomizer> customizers, @Nullable ApplicationTemp applicationTemp) {
    NettyWebServerFactory factory = new NettyWebServerFactory();
    factory.setNettyChannelInitializer(nettyChannelInitializer);

    serverProperties.applyTo(factory, sslBundles, applicationTemp);

    factory.applyFrom(serverProperties.netty);
    factory.setBootstrapCustomizers(customizers);
    return factory;
  }

  /**
   * Infra netty channel initializer
   *
   * @param channelHandler ChannelInboundHandler
   */
  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static NettyChannelInitializer nettyChannelInitializer(ServerProperties properties, NettyChannelHandler channelHandler) {
    var netty = properties.netty;
    var initializer = createChannelInitializer(channelHandler, properties);
    initializer.setCloseOnExpectationFailed(netty.closeOnExpectationFailed);
    initializer.setMaxContentLength(netty.maxContentLength.toBytesInt());

    HttpDecoderConfig httpDecoderConfig = new HttpDecoderConfig()
            .setInitialBufferSize(netty.initialBufferSize.toBytesInt())
            .setMaxChunkSize(netty.maxChunkSize.toBytesInt())
            .setMaxHeaderSize(netty.maxHeaderSize)
            .setValidateHeaders(netty.validateHeaders)
            .setChunkedSupported(netty.chunkedSupported)
            .setAllowPartialChunks(netty.allowPartialChunks)
            .setMaxInitialLineLength(netty.maxInitialLineLength)
            .setAllowDuplicateContentLengths(netty.allowDuplicateContentLengths);
    initializer.setHttpDecoderConfig(httpDecoderConfig);
    return initializer;
  }

  private static NettyChannelInitializer createChannelInitializer(NettyChannelHandler channelHandler, ServerProperties server) {
    if (server.ssl.enabled) {
      return new SSLNettyChannelInitializer(channelHandler, NettySSLBuilder.createSslContext(server.http2, server.ssl));
    }
    return new NettyChannelInitializer(channelHandler);
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static NettyRequestConfig nettyRequestConfig(ServerProperties server, SendErrorHandler sendErrorHandler) {
    var multipart = server.multipart;
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
            .secure(server.ssl.enabled)
            .build();
  }

  private static DefaultHttpDataFactory createHttpDataFactory(ServerProperties.Multipart multipart) {
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
