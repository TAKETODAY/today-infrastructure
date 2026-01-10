/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.annotation.config.web;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.MapConfigurationPropertySource;
import infra.util.DataSize;
import infra.web.server.ServerProperties;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import reactor.netty.http.HttpDecoderSpec;
import reactor.netty.http.server.HttpRequestDecoderSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerProperties}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Quinten De Swaef
 * @author Venil Noronha
 * @author Andrew McGhie
 * @author HaiTao Zhang
 * @author Rafiullah Hamedy
 * @author Chris Bono
 * @author Parviz Rozikov
 */
class ServerPropertiesTests {

  private final ServerProperties properties = new ServerProperties();

  @Test
  void testAddressBinding() throws Exception {
    bind("server.address", "127.0.0.1");
    assertThat(this.properties.address).isEqualTo(InetAddress.getByName("127.0.0.1"));
  }

  @Test
  void testPortBinding() {
    bind("server.port", "9000");
    assertThat(this.properties.port.intValue()).isEqualTo(9000);
  }

  @Test
  void testCustomizeHeaderSize() {
    bind("server.reactor-netty.max-header-size", "1MB");
    assertThat(this.properties.reactorNetty.maxHeaderSize).isEqualTo(DataSize.ofMegabytes(1));
  }

  @Test
  void testCustomizeHeaderSizeUseBytesByDefault() {
    bind("server.reactor-netty.max-header-size", "1024");
    assertThat(this.properties.reactorNetty.maxHeaderSize).isEqualTo(DataSize.ofKilobytes(1));
  }

  @Test
  void testCustomizeMaxHttpRequestHeaderSize() {
    bind("server.reactor-netty.max-header-size", "1MB");
    assertThat(this.properties.reactorNetty.maxHeaderSize).isEqualTo(DataSize.ofMegabytes(1));
  }

  @Test
  void testCustomizeMaxHttpRequestHeaderSizeUseBytesByDefault() {
    bind("server.reactor-netty.max-header-size", "1024");
    assertThat(this.properties.reactorNetty.maxHeaderSize).isEqualTo(DataSize.ofKilobytes(1));
  }

  @Test
  void testCustomizeNettyIdleTimeout() {
    bind("server.reactor-netty.idle-timeout", "10s");
    assertThat(this.properties.reactorNetty.idleTimeout).isEqualTo(Duration.ofSeconds(10));
  }

  @Test
  void testCustomizeNettyMaxKeepAliveRequests() {
    bind("server.reactor-netty.max-keep-alive-requests", "100");
    assertThat(this.properties.reactorNetty.maxKeepAliveRequests).isEqualTo(100);
  }

  @Test
  void nettyMaxChunkSizeMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.maxChunkSize.toBytes())
            .isEqualTo(HttpDecoderSpec.DEFAULT_MAX_CHUNK_SIZE);
  }

  @Test
  void nettyMaxInitialLineLengthMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.maxInitialLineLength.toBytes())
            .isEqualTo(HttpDecoderSpec.DEFAULT_MAX_INITIAL_LINE_LENGTH);
  }

  @Test
  void nettyValidateHeadersMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.validateHeaders).isEqualTo(HttpDecoderSpec.DEFAULT_VALIDATE_HEADERS);
  }

  @Test
  void nettyH2cMaxContentLengthMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.h2cMaxContentLength.toBytes())
            .isEqualTo(HttpRequestDecoderSpec.DEFAULT_H2C_MAX_CONTENT_LENGTH);
  }

  @Test
  void nettyInitialBufferSizeMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.initialBufferSize.toBytes())
            .isEqualTo(HttpDecoderSpec.DEFAULT_INITIAL_BUFFER_SIZE);
  }

  @Test
  void nettyWorkThreadCount() {
    assertThat(this.properties.netty.workerThreads).isNull();

    bind("server.netty.workerThreads", "10");
    assertThat(this.properties.netty.workerThreads).isEqualTo(10);

    bind("server.netty.worker-threads", "100");
    assertThat(this.properties.netty.workerThreads).isEqualTo(100);
  }

  @Test
  void nettyBossThreadCount() {
    assertThat(this.properties.netty.acceptorThreads).isNull();
    bind("server.netty.acceptorThreads", "10");
    assertThat(this.properties.netty.acceptorThreads).isEqualTo(10);

    bind("server.netty.acceptor-threads", "100");
    assertThat(this.properties.netty.acceptorThreads).isEqualTo(100);
  }

  @Test
  void nettyLoggingLevel() {
    assertThat(this.properties.netty.loggingLevel).isNull();

    bind("server.netty.loggingLevel", "INFO");
    assertThat(this.properties.netty.loggingLevel).isEqualTo(LogLevel.INFO);

    bind("server.netty.logging-level", "DEBUG");
    assertThat(this.properties.netty.loggingLevel).isEqualTo(LogLevel.DEBUG);
  }

  @Test
  void nettySocketChannel() {
    assertThat(this.properties.netty.socketChannel).isNull();

    bind("server.netty.socketChannel", "io.netty.channel.socket.nio.NioServerSocketChannel");
    assertThat(this.properties.netty.socketChannel).isEqualTo(NioServerSocketChannel.class);

    bind("server.netty.socket-channel", "io.netty.channel.socket.nio.NioServerSocketChannel");
    assertThat(this.properties.netty.socketChannel).isEqualTo(NioServerSocketChannel.class);
  }

  @Test
  void maxConnection() {
    bind("server.netty.maxConnection", "100");
    assertThat(this.properties.netty.maxConnection).isEqualTo(100);

    bind("server.netty.max-connection", "1000");
    assertThat(this.properties.netty.maxConnection).isEqualTo(1000);
  }

  private void bind(String name, String value) {
    bind(Collections.singletonMap(name, value));
  }

  private void bind(Map<String, String> map) {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
    new Binder(source).bind("server", Bindable.ofInstance(this.properties));
  }

}
