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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import infra.context.properties.source.ConfigurationPropertySources;
import infra.mock.env.MockEnvironment;
import infra.util.DataSize;
import infra.web.reactor.netty.ReactorNettyReactiveWebServerFactory;
import infra.web.reactor.netty.ReactorNettyServerCustomizer;
import infra.web.reactor.netty.ReactorServerProperties;
import infra.web.server.config.ServerProperties;
import io.netty.channel.ChannelOption;
import reactor.netty.http.Http2SettingsSpec;
import reactor.netty.http.server.HttpRequestDecoderSpec;
import reactor.netty.http.server.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link ReactorNettyWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Artsiom Yudovin
 * @author Leo Li
 */
@ExtendWith(MockitoExtension.class)
class ReactorNettyWebServerFactoryCustomizerTests {

  private MockEnvironment environment;

  private ServerProperties serverProperties;

  private ReactorServerProperties reactorProperties;

  private ReactorNettyWebServerFactoryCustomizer customizer;

  @Captor
  private ArgumentCaptor<ReactorNettyServerCustomizer> customizerCaptor;

  @BeforeEach
  void setup() {
    this.environment = new MockEnvironment();
    this.serverProperties = new ServerProperties();
    reactorProperties = new ReactorServerProperties();
    ConfigurationPropertySources.attach(this.environment);
    this.customizer = new ReactorNettyWebServerFactoryCustomizer(this.environment, this.serverProperties, reactorProperties);
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
    this.serverProperties.forwardHeadersStrategy = (ServerProperties.ForwardHeadersStrategy.NATIVE);
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(true);
  }

  @Test
  void forwardHeadersWhenStrategyIsNoneShouldNotConfigureValve() {
    this.environment.setProperty("DYNO", "-");
    this.serverProperties.forwardHeadersStrategy = (ServerProperties.ForwardHeadersStrategy.NONE);
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(false);
  }

  @Test
  void setConnectionTimeout() {
    reactorProperties.connectionTimeout = (Duration.ofSeconds(1));
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyConnectionTimeout(factory, 1000);
  }

  @Test
  void setIdleTimeout() {
    reactorProperties.idleTimeout = (Duration.ofSeconds(1));
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyIdleTimeout(factory, Duration.ofSeconds(1));
  }

  @Test
  void setMaxKeepAliveRequests() {
    reactorProperties.maxKeepAliveRequests = (100);
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyMaxKeepAliveRequests(factory, 100);
  }

  @Test
  void setHttp2MaxRequestHeaderSize() {
    DataSize headerSize = DataSize.ofKilobytes(24);
    serverProperties.http2.setEnabled(true);
    serverProperties.http2.initialSettings.maxHeaderListSize = headerSize;
    reactorProperties.maxHeaderSize = DataSize.ofKilobytes(25);
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyHttp2MaxHeaderSize(factory, headerSize.toBytes());
  }

  @Test
  void configureHttpRequestDecoder() {
    ReactorServerProperties nettyProperties = reactorProperties;
    reactorProperties.maxHeaderSize = (DataSize.ofKilobytes(24));
    nettyProperties.validateHeaders = (false);
    nettyProperties.initialBufferSize = (DataSize.ofBytes(512));
    nettyProperties.h2cMaxContentLength = (DataSize.ofKilobytes(1));
    nettyProperties.maxInitialLineLength = (DataSize.ofKilobytes(32));
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().addServerCustomizers(this.customizerCaptor.capture());
    ReactorNettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
    HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
    HttpRequestDecoderSpec decoder = httpServer.configuration().decoder();
    assertThat(decoder.validateHeaders()).isFalse();
    assertThat(decoder.maxHeaderSize()).isEqualTo(reactorProperties.maxHeaderSize.toBytes());
    assertThat(decoder.initialBufferSize()).isEqualTo(nettyProperties.initialBufferSize.toBytes());
    assertThat(decoder.h2cMaxContentLength()).isEqualTo(nettyProperties.h2cMaxContentLength.toBytes());
    assertThat(decoder.maxInitialLineLength()).isEqualTo(nettyProperties.maxInitialLineLength.toBytes());
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

  private void verifyHttp2MaxHeaderSize(ReactorNettyReactiveWebServerFactory factory, long expected) {
    then(factory).should(times(2)).addServerCustomizers(this.customizerCaptor.capture());
    ReactorNettyServerCustomizer serverCustomizer = this.customizerCaptor.getAllValues().get(0);
    HttpServer httpServer = serverCustomizer.apply(HttpServer.create());
    Http2SettingsSpec decoder = httpServer.configuration().http2SettingsSpec();
    assertThat(decoder.maxHeaderListSize()).isEqualTo(expected);
  }

}
