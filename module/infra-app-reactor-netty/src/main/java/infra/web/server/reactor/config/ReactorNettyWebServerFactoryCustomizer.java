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

package infra.web.server.reactor.config;

import java.time.Duration;

import infra.app.cloud.CloudPlatform;
import infra.core.Ordered;
import infra.core.env.Environment;
import infra.util.PropertyMapper;
import infra.web.server.Http2;
import infra.web.server.ServerProperties;
import infra.web.server.WebServerFactoryCustomizer;
import infra.web.server.reactor.ReactorNettyReactiveWebServerFactory;
import infra.web.server.reactor.ReactorServerProperties;
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
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();

    map.from(reactor.idleTimeout).whenNonNull().to(idleTimeout -> customizeIdleTimeout(factory, idleTimeout));
    map.from(reactor.connectionTimeout).whenNonNull().to(connectionTimeout -> customizeConnectionTimeout(factory, connectionTimeout));
    map.from(reactor.maxKeepAliveRequests).to(maxKeepAliveRequests -> customizeMaxKeepAliveRequests(factory, maxKeepAliveRequests));

    if (Http2.isEnabled(serverProperties.http2)) {
      map.from(reactor.maxHeaderSize)
              .whenNonNull()
              .to(size -> customizeHttp2MaxHeaderSize(factory, size.toBytes()));
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
              .whenNonNull()
              .to(maxHttpRequestHeader -> httpRequestDecoderSpec.maxHeaderSize((int) maxHttpRequestHeader.toBytes()));
      propertyMapper.from(reactor.maxChunkSize)
              .whenNonNull()
              .to(maxChunkSize -> httpRequestDecoderSpec.maxChunkSize((int) maxChunkSize.toBytes()));
      propertyMapper.from(reactor.maxInitialLineLength)
              .whenNonNull()
              .to(maxInitialLineLength -> httpRequestDecoderSpec.maxInitialLineLength((int) maxInitialLineLength.toBytes()));
      propertyMapper.from(reactor.h2cMaxContentLength)
              .whenNonNull()
              .to(h2cMaxContentLength -> httpRequestDecoderSpec.h2cMaxContentLength((int) h2cMaxContentLength.toBytes()));
      propertyMapper.from(reactor.initialBufferSize)
              .whenNonNull()
              .to(initialBufferSize -> httpRequestDecoderSpec.initialBufferSize((int) initialBufferSize.toBytes()));
      propertyMapper.from(reactor.validateHeaders).whenNonNull().to(httpRequestDecoderSpec::validateHeaders);
      return httpRequestDecoderSpec;
    }));
  }

  private void customizeIdleTimeout(ReactorNettyReactiveWebServerFactory factory, Duration idleTimeout) {
    factory.addServerCustomizers(httpServer -> httpServer.idleTimeout(idleTimeout));
  }

  private void customizeMaxKeepAliveRequests(ReactorNettyReactiveWebServerFactory factory, int maxKeepAliveRequests) {
    factory.addServerCustomizers(httpServer -> httpServer.maxKeepAliveRequests(maxKeepAliveRequests));
  }

  private void customizeHttp2MaxHeaderSize(ReactorNettyReactiveWebServerFactory factory, long size) {
    factory.addServerCustomizers(
            ((httpServer) -> httpServer.http2Settings(settings -> settings.maxHeaderListSize(size))));
  }

}
