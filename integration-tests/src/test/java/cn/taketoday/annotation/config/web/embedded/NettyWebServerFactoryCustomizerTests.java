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
