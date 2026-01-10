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