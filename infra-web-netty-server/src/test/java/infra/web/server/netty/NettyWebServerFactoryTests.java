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

package infra.web.server.netty;

import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import infra.util.DataSize;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.logging.LoggingHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.logging.LogLevel;
import io.netty.util.NetUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:05
 */
class NettyWebServerFactoryTests {

  @Test
  void bootstrapAndChildPipelineAreCustomizedBeforeServingConnections() throws Exception {
    var factory = nioFactory();
    var parent = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    var worker = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    try {
      factory.setAcceptorGroup(parent);
      factory.setWorkerGroup(worker);
      factory.setSocketChannel(NioServerSocketChannel.class);
      factory.setPort(0);
      factory.setConnectionTimeout(Duration.ofSeconds(2));
      factory.setLoggingLevel(LogLevel.DEBUG);
      factory.setHttpTrafficHandler(new ChannelInboundHandlerAdapter());
      var captured = new AtomicReference<ServerBootstrap>();
      factory.setBootstrapCustomizers(List.of(bootstrap -> {
        captured.set(bootstrap);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, false);
      }));
      var configurer = mock(ChannelConfigurer.class);
      factory.setChannelConfigurer(configurer);

      var server = factory.createWebServer();
      var bootstrap = captured.get().config();
      assertThat(bootstrap.group()).isSameAs(parent);
      assertThat(bootstrap.childGroup()).isSameAs(worker);
      assertThat(bootstrap.options()).containsEntry(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
      assertThat(bootstrap.childOptions()).containsEntry(ChannelOption.TCP_NODELAY, false);
      assertThat(bootstrap.handler()).isInstanceOfSatisfying(LoggingHandler.class,
              handler -> assertThat(handler.level()).isEqualTo(LogLevel.DEBUG));

      server.start();
      try (Socket socket = new Socket("localhost", server.getPort())) {
        var child = org.mockito.ArgumentCaptor.forClass(Channel.class);
        var order = inOrder(configurer);
        order.verify(configurer, timeout(5000)).initChannel(child.capture());
        order.verify(configurer, timeout(5000)).postInitChannel(child.getValue());
        assertThat(child.getValue().pipeline().get(HttpChannelInitializer.HttpCodec)).isNotNull();
      }
    }
    finally {
      parent.shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
      worker.shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
    }
  }

  @Test
  void http2CodecUsesConfiguredInitialSettingsForEachConnection() {
    var factory = new NettyWebServerFactory();
    var http2 = factory.getHttp2();
    http2.setEnabled(true);
    var settings = http2.initialSettings;
    settings.maxFrameSize = DataSize.ofKilobytes(32);
    settings.maxHeaderListSize = null;
    settings.initialWindowSize = 100_000;
    settings.maxConcurrentStreams = 5;
    settings.headerTableSize = 1024;
    settings.connectProtocolEnabled = true;
    var initializer = (HttpChannelInitializer) factory.createChannelInitializer(new NettyServerProperties(),
            new ChannelInboundHandlerAdapter());
    Http2FrameCodec first = initializer.createHttp2FrameCodec();
    Http2FrameCodec second = initializer.createHttp2FrameCodec();
    var firstChannel = new EmbeddedChannel(first);
    var secondChannel = new EmbeddedChannel(second);
    try {
      assertThat(second).isNotSameAs(first);
      for (var codec : List.of(first, second)) {
        var sent = codec.encoder().pollSentSettings();
        assertThat(sent).isNotNull();
        assertThat(sent.maxFrameSize()).isEqualTo(32768);
        assertThat(sent.initialWindowSize()).isEqualTo(100_000);
        assertThat(sent.maxConcurrentStreams()).isEqualTo(5L);
        assertThat(sent.headerTableSize()).isEqualTo(1024L);
        assertThat(sent.connectProtocolEnabled()).isTrue();
      }
    }
    finally {
      firstChannel.finishAndReleaseAll();
      secondChannel.finishAndReleaseAll();
    }
  }

  private static NettyWebServerFactory nioFactory() {
    return new NettyWebServerFactory() {
      @Override
      protected boolean epollIsAvailable() {
        return false;
      }

      @Override
      protected boolean kQueueIsAvailable() {
        return false;
      }
    };
  }

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
    NettyServerProperties netty = new NettyServerProperties();
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
    NettyServerProperties netty = new NettyServerProperties();
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

  @Test
  void setHttp2FrameCodecFactory() {
    NettyWebServerFactory factory = new NettyWebServerFactory();
    assertThat(factory).extracting("http2FrameCodecFactory").isNotNull();

    Object http2FrameCodecFactory = Extractors.byName("http2FrameCodecFactory").apply(factory);
    factory.setHttp2FrameCodecFactory(() -> null);

    assertThat(factory).extracting("http2FrameCodecFactory").isNotNull()
            .isNotEqualTo(http2FrameCodecFactory);

    factory.setHttp2FrameCodecFactory(null);

    assertThat(factory).extracting("http2FrameCodecFactory").isNotNull()
            .isInstanceOf(http2FrameCodecFactory.getClass());
  }

}
