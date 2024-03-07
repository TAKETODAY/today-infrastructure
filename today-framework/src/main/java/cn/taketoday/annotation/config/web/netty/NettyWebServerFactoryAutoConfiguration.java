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

import java.io.IOException;
import java.io.InputStream;
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
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.properties.ConfigurationProperties;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
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
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.web.multipart.MultipartConfig;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.ssl.SslContextBuilder;

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
    handler.setThrowExceptionIfNoHandlerFound(webMvcProperties.isThrowExceptionIfNoHandlerFound());
    handler.setEnableLoggingRequestDetails(webMvcProperties.isLogRequestDetails());
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

    factory.applyFrom(serverProperties.getNetty());
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
  static NettyChannelInitializer nettyChannelInitializer(ServerProperties properties,
          NettyChannelHandler channelHandler, ResourceLoader resourceLoader) {
    var netty = properties.getNetty();
    var initializer = createChannelInitializer(resourceLoader, channelHandler, netty);
    initializer.setCloseOnExpectationFailed(netty.isCloseOnExpectationFailed());
    initializer.setMaxContentLength(netty.getMaxContentLength().toBytesInt());

    HttpDecoderConfig httpDecoderConfig = new HttpDecoderConfig()
            .setMaxInitialLineLength(netty.getMaxInitialLineLength())
            .setMaxHeaderSize(netty.getMaxHeaderSize())
            .setMaxChunkSize(netty.getMaxChunkSize().toBytesInt())
            .setValidateHeaders(netty.isValidateHeaders());
    initializer.setHttpDecoderConfig(httpDecoderConfig);
    return initializer;
  }

  private static NettyChannelInitializer createChannelInitializer(ResourceLoader resourceLoader,
          NettyChannelHandler channelHandler, ServerProperties.Netty netty) {
    var nettySSL = netty.getSsl();
    if (nettySSL.isEnabled()) {
      Resource privateKeyResource = resourceLoader.getResource(nettySSL.getPrivateKey());
      Resource publicKeyResource = resourceLoader.getResource(nettySSL.getPublicKey());

      Assert.state(publicKeyResource.exists(), "publicKey not found");
      Assert.state(privateKeyResource.exists(), "privateKey not found");

      try (InputStream publicKeyStream = publicKeyResource.getInputStream();
              InputStream privateKeyStream = privateKeyResource.getInputStream()) {
        return new SSLNettyChannelInitializer(channelHandler,
                SslContextBuilder.forServer(publicKeyStream, privateKeyStream, nettySSL.getKeyPassword()).build());
      }
      catch (IOException e) {
        throw new IllegalStateException("publicKey or publicKey resource I/O error", e);
      }
    }

    return new NettyChannelInitializer(channelHandler);
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static NettyRequestConfig nettyRequestConfig(ServerProperties properties,
          MultipartConfig multipartConfig, SendErrorHandler sendErrorHandler) {
    String location = multipartConfig.getLocation();
    var factory = new DefaultHttpDataFactory(location != null);
    if (multipartConfig.getMaxRequestSize() != null) {
      factory.setMaxLimit(multipartConfig.getMaxRequestSize().toBytes());
    }
    if (location != null) {
      factory.setBaseDir(location);
    }
    return new NettyRequestConfig(factory, sendErrorHandler, properties.getNetty().getSsl().isEnabled());
  }

  @Component
  @ConditionalOnProperty(prefix = "server.multipart", name = "enabled", matchIfMissing = true)
  @ConfigurationProperties(prefix = "server.multipart", ignoreUnknownFields = false)
  static MultipartConfig multipartConfig() {
    return new MultipartConfig();
  }

}
