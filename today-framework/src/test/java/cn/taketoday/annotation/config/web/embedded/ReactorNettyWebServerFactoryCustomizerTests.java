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
import cn.taketoday.web.server.reactive.support.ReactorNettyReactiveWebServerFactory;
import cn.taketoday.web.server.reactive.support.ReactorNettyServerCustomizer;
import cn.taketoday.web.server.Http2;
import cn.taketoday.web.server.ServerProperties;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.util.DataSize;
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

  private ReactorNettyWebServerFactoryCustomizer customizer;

  @Captor
  private ArgumentCaptor<ReactorNettyServerCustomizer> customizerCaptor;

  @BeforeEach
  void setup() {
    this.environment = new MockEnvironment();
    this.serverProperties = new ServerProperties();
    ConfigurationPropertySources.attach(this.environment);
    this.customizer = new ReactorNettyWebServerFactoryCustomizer(this.environment, this.serverProperties);
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
    this.serverProperties.reactorNetty.connectionTimeout = (Duration.ofSeconds(1));
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyConnectionTimeout(factory, 1000);
  }

  @Test
  void setIdleTimeout() {
    this.serverProperties.reactorNetty.idleTimeout = (Duration.ofSeconds(1));
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyIdleTimeout(factory, Duration.ofSeconds(1));
  }

  @Test
  void setMaxKeepAliveRequests() {
    this.serverProperties.reactorNetty.maxKeepAliveRequests = (100);
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyMaxKeepAliveRequests(factory, 100);
  }

  @Test
  void setHttp2MaxRequestHeaderSize() {
    DataSize headerSize = DataSize.ofKilobytes(24);
    serverProperties.http2 = new Http2();
    this.serverProperties.http2.setEnabled(true);
    this.serverProperties.maxHttpRequestHeaderSize = (headerSize);
    ReactorNettyReactiveWebServerFactory factory = mock(ReactorNettyReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    verifyHttp2MaxHeaderSize(factory, headerSize.toBytes());
  }

  @Test
  void configureHttpRequestDecoder() {
    ServerProperties.ReactorNetty nettyProperties = this.serverProperties.reactorNetty;
    this.serverProperties.maxHttpRequestHeaderSize = (DataSize.ofKilobytes(24));
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
    assertThat(decoder.maxHeaderSize()).isEqualTo(this.serverProperties.maxHttpRequestHeaderSize.toBytes());
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
