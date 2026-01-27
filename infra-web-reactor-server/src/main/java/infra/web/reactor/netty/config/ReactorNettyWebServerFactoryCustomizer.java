/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.reactor.netty.config;

import java.time.Duration;

import infra.app.cloud.CloudPlatform;
import infra.core.Ordered;
import infra.core.env.Environment;
import infra.util.DataSize;
import infra.util.PropertyMapper;
import infra.web.reactor.netty.ReactorNettyReactiveWebServerFactory;
import infra.web.reactor.netty.ReactorServerProperties;
import infra.web.server.Http2;
import infra.web.server.WebServerFactoryCustomizer;
import infra.web.server.config.ServerProperties;
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
public class ReactorNettyWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ReactorNettyReactiveWebServerFactory>, Ordered {

  private final Environment environment;

  private final ServerProperties serverProperties;

  private final ReactorServerProperties reactor;

  public ReactorNettyWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties, ReactorServerProperties reactorProperties) {
    this.environment = environment;
    this.serverProperties = serverProperties;
    this.reactor = reactorProperties;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void customize(ReactorNettyReactiveWebServerFactory factory) {
    factory.setUseForwardHeaders(getOrDeduceUseForwardHeaders());
    PropertyMapper map = PropertyMapper.get();

    map.from(reactor.idleTimeout).to(idleTimeout -> customizeIdleTimeout(factory, idleTimeout));
    map.from(reactor.connectionTimeout).to(connectionTimeout -> customizeConnectionTimeout(factory, connectionTimeout));
    map.from(reactor.maxKeepAliveRequests).to(maxKeepAliveRequests -> customizeMaxKeepAliveRequests(factory, maxKeepAliveRequests));

    if (Http2.isEnabled(serverProperties.http2)) {
      factory.addServerCustomizers(httpServer -> httpServer.http2Settings(builder -> {
        var settings = serverProperties.http2.initialSettings;
        map.from(settings.connectProtocolEnabled).to(builder::connectProtocolEnabled);
        map.from(settings.headerTableSize).to(builder::headerTableSize);
        map.from(settings.initialWindowSize).to(builder::initialWindowSize);
        map.from(settings.maxConcurrentStreams).to(builder::maxConcurrentStreams);
        map.from(settings.maxFrameSize).asInt(DataSize::bytes).to(builder::maxFrameSize);
        map.from(settings.maxHeaderListSize).asInt(DataSize::bytes).to(builder::maxHeaderListSize);
      }));
    }
    customizeRequestDecoder(factory, map);
  }

  private boolean getOrDeduceUseForwardHeaders() {
    if (this.serverProperties.forwardHeadersStrategy == null) {
      CloudPlatform platform = CloudPlatform.getActive(this.environment);
      return platform != null && platform.isUsingForwardHeaders();
    }
    return this.serverProperties.forwardHeadersStrategy.equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
  }

  private void customizeConnectionTimeout(ReactorNettyReactiveWebServerFactory factory, Duration connectionTimeout) {
    factory.addServerCustomizers(httpServer -> httpServer.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectionTimeout.toMillis()));
  }

  @SuppressWarnings("deprecation")
  private void customizeRequestDecoder(ReactorNettyReactiveWebServerFactory factory, PropertyMapper propertyMapper) {
    factory.addServerCustomizers((httpServer) -> httpServer.httpRequestDecoder((httpRequestDecoderSpec) -> {
      propertyMapper.from(this.reactor.maxHeaderSize)
              .to(maxHttpRequestHeader -> httpRequestDecoderSpec.maxHeaderSize((int) maxHttpRequestHeader.toBytes()));
      propertyMapper.from(reactor.maxChunkSize)
              .to(maxChunkSize -> httpRequestDecoderSpec.maxChunkSize((int) maxChunkSize.toBytes()));
      propertyMapper.from(reactor.maxInitialLineLength)
              .to(maxInitialLineLength -> httpRequestDecoderSpec.maxInitialLineLength((int) maxInitialLineLength.toBytes()));
      propertyMapper.from(reactor.h2cMaxContentLength)
              .to(h2cMaxContentLength -> httpRequestDecoderSpec.h2cMaxContentLength((int) h2cMaxContentLength.toBytes()));
      propertyMapper.from(reactor.initialBufferSize)
              .to(initialBufferSize -> httpRequestDecoderSpec.initialBufferSize((int) initialBufferSize.toBytes()));
      propertyMapper.from(reactor.validateHeaders).to(httpRequestDecoderSpec::validateHeaders);
      return httpRequestDecoderSpec;
    }));
  }

  private void customizeIdleTimeout(ReactorNettyReactiveWebServerFactory factory, Duration idleTimeout) {
    factory.addServerCustomizers(httpServer -> httpServer.idleTimeout(idleTimeout));
  }

  private void customizeMaxKeepAliveRequests(ReactorNettyReactiveWebServerFactory factory, int maxKeepAliveRequests) {
    factory.addServerCustomizers(httpServer -> httpServer.maxKeepAliveRequests(maxKeepAliveRequests));
  }

}
