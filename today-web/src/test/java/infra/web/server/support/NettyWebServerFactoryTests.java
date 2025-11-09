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

package infra.web.server.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.util.DataSize;
import infra.web.server.ServerProperties.Netty;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.logging.LogLevel;
import io.netty.util.NetUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:05
 */
class NettyWebServerFactoryTests {

  @Test
  void shouldCreateNettyWebServerFactoryWithDefaults() {
    // when
    NettyWebServerFactory factory = new NettyWebServerFactory();

    // then
    assertThat(factory).isNotNull();
    assertThat(factory.getWorkThreadCount()).isEqualTo(4);
    assertThat(factory.getAcceptorThreadCount()).isEqualTo(2);
    assertThat(factory).extracting("maxConnection").isEqualTo(NetUtil.SOMAXCONN);
  }

  @Test
  void shouldSetAndGetWorkerThreadCount() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();

    // when
    factory.setWorkerThreadCount(8);

    // then
    assertThat(factory.getWorkThreadCount()).isEqualTo(8);
  }

  @Test
  void shouldSetAndGetAcceptorThreadCount() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();

    // when
    factory.setAcceptorThreadCount(4);

    // then
    assertThat(factory.getAcceptorThreadCount()).isEqualTo(4);
  }

  @Test
  void shouldSetAndGetMaxConnection() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();

    // when
    factory.setMaxConnection(1000);

    // then
    assertThat(factory).extracting("maxConnection").isEqualTo(1000);
  }

  @Test
  void shouldSetAndGetLoggingLevel() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();

    // when
    factory.setLoggingLevel(LogLevel.INFO);

    // then
    assertThat(factory.getLoggingLevel()).isEqualTo(LogLevel.INFO);
  }

  @Test
  void shouldApplyNettyConfiguration() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();
    Netty netty = new Netty();
    netty.loggingLevel = LogLevel.DEBUG;
    netty.acceptorThreads = 3;
    netty.workerThreads = 6;
    netty.maxConnection = 2000;
    netty.workerPoolName = "custom-worker";
    netty.acceptorPoolName = "custom-acceptor";

    // when
    factory.applyFrom(netty);

    // then
    assertThat(factory.getLoggingLevel()).isEqualTo(LogLevel.DEBUG);
    assertThat(factory.getAcceptorThreadCount()).isEqualTo(3);
    assertThat(factory.getWorkThreadCount()).isEqualTo(6);
    assertThat(factory).extracting("maxConnection").isEqualTo(2000);
  }

  @Test
  void shouldCreateHttpDecoderConfig() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();
    Netty netty = new Netty();
    netty.initialBufferSize = DataSize.ofBytes(1024);
    netty.maxChunkSize = DataSize.ofKilobytes(8);
    netty.maxHeaderSize = DataSize.ofKilobytes(8);
    netty.maxInitialLineLength = 4096;

    // when
    HttpDecoderConfig config = factory.createHttpDecoderConfig(netty);

    // then
    assertThat(config).isNotNull();
  }

  @Test
  void shouldSetAndGetEventLoopGroups() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();
    EventLoopGroup workerGroup = mock(EventLoopGroup.class);
    EventLoopGroup acceptorGroup = mock(EventLoopGroup.class);

    // when
    factory.setWorkerGroup(workerGroup);
    factory.setAcceptorGroup(acceptorGroup);

    // then
    assertThat(factory.getWorkerGroup()).isEqualTo(workerGroup);
    assertThat(factory.getAcceptorGroup()).isEqualTo(acceptorGroup);
  }

  @Test
  void shouldSetAndGetSocketChannel() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();
    Class<? extends ServerSocketChannel> socketChannel = NioServerSocketChannel.class;

    // when
    factory.setSocketChannel(socketChannel);

    // then
    assertThat(factory.getSocketChannel()).isEqualTo(socketChannel);
  }

  @Test
  void shouldSetAndGetChannelConfigurer() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();
    ChannelConfigurer configurer = mock(ChannelConfigurer.class);

    // when
    factory.setChannelConfigurer(configurer);

    // then
    assertThat(factory).extracting("channelConfigurer").isEqualTo(configurer);
  }

  @Test
  void shouldSetAndGetBootstrapCustomizers() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();
    List<ServerBootstrapCustomizer> customizers = List.of(mock(ServerBootstrapCustomizer.class));

    // when
    factory.setBootstrapCustomizers(customizers);

    // then
    assertThat(factory).extracting("bootstrapCustomizers").isEqualTo(customizers);
  }

  @Test
  void shouldSetAndGetThreadPoolNames() {
    // given
    NettyWebServerFactory factory = new NettyWebServerFactory();
    String workerPoolName = "test-workers";
    String acceptorPoolName = "test-acceptor";

    // when
    factory.setWorkerPoolName(workerPoolName);
    factory.setAcceptorPoolName(acceptorPoolName);

    // then
    assertThat(factory).extracting("workerPoolName").isEqualTo(workerPoolName);
    assertThat(factory).extracting("acceptorPoolName").isEqualTo(acceptorPoolName);
  }

}