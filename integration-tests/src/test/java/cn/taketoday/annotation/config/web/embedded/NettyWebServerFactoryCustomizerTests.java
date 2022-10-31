/*
 * Copyright 2012-2022 the original author or authors.
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

package cn.taketoday.annotation.config.web.embedded;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyReactiveWebServerFactory;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyServerCustomizer;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.util.DataSize;
import io.netty.channel.ChannelOption;
import reactor.netty.http.server.HttpRequestDecoderSpec;
import reactor.netty.http.server.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link NettyWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Artsiom Yudovin
 * @author Leo Li
 */
@ExtendWith(MockitoExtension.class)
class NettyWebServerFactoryCustomizerTests {

  private MockEnvironment environment;

  private ServerProperties serverProperties;

  private NettyWebServerFactoryCustomizer customizer;

  @Captor
  private ArgumentCaptor<ReactorNettyServerCustomizer> customizerCaptor;

  @BeforeEach
  void setup() {
    this.environment = new MockEnvironment();
    this.serverProperties = new ServerProperties();
    ConfigurationPropertySources.attach(this.environment);
    this.customizer = new NettyWebServerFactoryCustomizer(this.environment, this.serverProperties);
  }

  @Test
  void deduceUseForwardHeaders() {
    this.environment.setProperty("DYNO", "-");
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(true);
  }

  @Test
  void defaultUseForwardHeaders() {
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(false);
  }

  @Test
  void forwardHeadersWhenStrategyIsNativeShouldConfigureValve() {
    this.serverProperties.setForwardHeadersStrategy(ServerProperties.ForwardHeadersStrategy.NATIVE);
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(true);
  }

  @Test
  void forwardHeadersWhenStrategyIsNoneShouldNotConfigureValve() {
    this.environment.setProperty("DYNO", "-");
    this.serverProperties.setForwardHeadersStrategy(ServerProperties.ForwardHeadersStrategy.NONE);
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(false);
  }

  @Test
  void setConnectionTimeout() {
    this.serverProperties.getNetty().setConnectionTimeout(Duration.ofSeconds(1));
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyConnectionTimeout(factory, 1000);
  }

  @Test
  void setIdleTimeout() {
    this.serverProperties.getNetty().setIdleTimeout(Duration.ofSeconds(1));
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyIdleTimeout(factory, Duration.ofSeconds(1));
  }

  @Test
  void setMaxKeepAliveRequests() {
    this.serverProperties.getNetty().setMaxKeepAliveRequests(100);
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyMaxKeepAliveRequests(factory, 100);
  }

  @Test
  void configureHttpRequestDecoder() {
    ServerProperties.ReactorNetty nettyProperties = this.serverProperties.getNetty();
    nettyProperties.setValidateHeaders(false);
    nettyProperties.setInitialBufferSize(DataSize.ofBytes(512));
    nettyProperties.setH2cMaxContentLength(DataSize.ofKilobytes(1));
    setMaxChunkSize(nettyProperties);
    nettyProperties.setMaxInitialLineLength(DataSize.ofKilobytes(32));
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().addServerCustomizers(this.customizerCaptor.capture());
    ReactorNettyServerCustomizer serverCustomizer = this.customizerCaptor.getValue();
    HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
    HttpRequestDecoderSpec decoder = httpServer.configuration().decoder();
    assertThat(decoder.validateHeaders()).isFalse();
    assertThat(decoder.initialBufferSize()).isEqualTo(nettyProperties.getInitialBufferSize().toBytes());
    assertThat(decoder.h2cMaxContentLength()).isEqualTo(nettyProperties.getH2cMaxContentLength().toBytes());
    assertMaxChunkSize(nettyProperties, decoder);
    assertThat(decoder.maxInitialLineLength()).isEqualTo(nettyProperties.getMaxInitialLineLength().toBytes());
  }

  @SuppressWarnings("removal")
  private void setMaxChunkSize(ServerProperties.ReactorNetty nettyProperties) {
    nettyProperties.setMaxChunkSize(DataSize.ofKilobytes(16));
  }

  @SuppressWarnings({ "deprecation", "removal" })
  private void assertMaxChunkSize(ServerProperties.ReactorNetty nettyProperties, HttpRequestDecoderSpec decoder) {
    assertThat(decoder.maxChunkSize()).isEqualTo(nettyProperties.getMaxChunkSize().toBytes());
  }

  private void verifyConnectionTimeout(ReactorNettyReactiveWebServerFactory factory, Integer expected) {
    if (expected == null) {
      then(factory).should(never()).addServerCustomizers(any(ReactorNettyServerCustomizer.class));
      return;
    }
    then(factory).should(times(2)).addServerCustomizers(this.customizerCaptor.capture());
    ReactorNettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
    HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
    Map<ChannelOption<?>, ?> options = httpServer.configuration().options();
    assertThat(options.get(ChannelOption.CONNECT_TIMEOUT_MILLIS)).isEqualTo(expected);
  }

  private void verifyIdleTimeout(ReactorNettyReactiveWebServerFactory factory, Duration expected) {
    if (expected == null) {
      then(factory).should(never()).addServerCustomizers(any(ReactorNettyServerCustomizer.class));
      return;
    }
    then(factory).should(times(2)).addServerCustomizers(this.customizerCaptor.capture());
    ReactorNettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
    HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
    Duration idleTimeout = httpServer.configuration().idleTimeout();
    assertThat(idleTimeout).isEqualTo(expected);
  }

  private void verifyMaxKeepAliveRequests(ReactorNettyReactiveWebServerFactory factory, int expected) {
    then(factory).should(times(2)).addServerCustomizers(this.customizerCaptor.capture());
    ReactorNettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
    HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
    int maxKeepAliveRequests = httpServer.configuration().maxKeepAliveRequests();
    assertThat(maxKeepAliveRequests).isEqualTo(expected);
  }

}
