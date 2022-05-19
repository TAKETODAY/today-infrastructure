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

package cn.taketoday.framework.web.embedded.config;

import java.time.Duration;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.framework.web.embedded.netty.NettyReactiveWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.util.PropertyMapper;
import io.netty.channel.ChannelOption;

/**
 * Customization for Netty-specific features.
 *
 * @author Brian Clozel
 * @author Chentao Qu
 * @author Artsiom Yudovin
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class NettyWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory>, Ordered {

  private final Environment environment;

  private final ServerProperties serverProperties;

  public NettyWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
    this.environment = environment;
    this.serverProperties = serverProperties;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void customize(NettyReactiveWebServerFactory factory) {
    factory.setUseForwardHeaders(getOrDeduceUseForwardHeaders());
    PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
    ServerProperties.Netty nettyProperties = serverProperties.getNetty();

    propertyMapper.from(nettyProperties::getIdleTimeout).whenNonNull().to(idleTimeout -> customizeIdleTimeout(factory, idleTimeout));
    propertyMapper.from(nettyProperties::getConnectionTimeout).whenNonNull().to(connectionTimeout -> customizeConnectionTimeout(factory, connectionTimeout));
    propertyMapper.from(nettyProperties::getMaxKeepAliveRequests).to(maxKeepAliveRequests -> customizeMaxKeepAliveRequests(factory, maxKeepAliveRequests));
    customizeRequestDecoder(factory, propertyMapper);
  }

  private boolean getOrDeduceUseForwardHeaders() {
    if (this.serverProperties.getForwardHeadersStrategy() == null) {
      CloudPlatform platform = CloudPlatform.getActive(this.environment);
      return platform != null && platform.isUsingForwardHeaders();
    }
    return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
  }

  private void customizeConnectionTimeout(NettyReactiveWebServerFactory factory, Duration connectionTimeout) {
    factory.addServerCustomizers(httpServer -> httpServer.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectionTimeout.toMillis()));
  }

  private void customizeRequestDecoder(NettyReactiveWebServerFactory factory, PropertyMapper propertyMapper) {
    factory.addServerCustomizers((httpServer) -> httpServer.httpRequestDecoder((httpRequestDecoderSpec) -> {
      propertyMapper.from(this.serverProperties.getMaxHttpHeaderSize())
              .whenNonNull()
              .to(maxHttpRequestHeader -> httpRequestDecoderSpec.maxHeaderSize((int) maxHttpRequestHeader.toBytes()));
      ServerProperties.Netty nettyProperties = this.serverProperties.getNetty();
      propertyMapper.from(nettyProperties.getMaxChunkSize())
              .whenNonNull()
              .to(maxChunkSize -> httpRequestDecoderSpec.maxChunkSize((int) maxChunkSize.toBytes()));
      propertyMapper.from(nettyProperties.getMaxInitialLineLength())
              .whenNonNull()
              .to(maxInitialLineLength -> httpRequestDecoderSpec.maxInitialLineLength((int) maxInitialLineLength.toBytes()));
      propertyMapper.from(nettyProperties.getH2cMaxContentLength())
              .whenNonNull()
              .to(h2cMaxContentLength -> httpRequestDecoderSpec.h2cMaxContentLength((int) h2cMaxContentLength.toBytes()));
      propertyMapper.from(nettyProperties.getInitialBufferSize())
              .whenNonNull()
              .to(initialBufferSize -> httpRequestDecoderSpec.initialBufferSize((int) initialBufferSize.toBytes()));
      propertyMapper.from(nettyProperties.isValidateHeaders()).whenNonNull().to(httpRequestDecoderSpec::validateHeaders);
      return httpRequestDecoderSpec;
    }));
  }

  private void customizeIdleTimeout(NettyReactiveWebServerFactory factory, Duration idleTimeout) {
    factory.addServerCustomizers(httpServer -> httpServer.idleTimeout(idleTimeout));
  }

  private void customizeMaxKeepAliveRequests(NettyReactiveWebServerFactory factory, int maxKeepAliveRequests) {
    factory.addServerCustomizers(httpServer -> httpServer.maxKeepAliveRequests(maxKeepAliveRequests));
  }

}
