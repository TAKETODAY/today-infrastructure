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

package cn.taketoday.annotation.config.web;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.util.DataSize;
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
    bind("server.max-http-request-header-size", "1MB");
    assertThat(this.properties.maxHttpRequestHeaderSize).isEqualTo(DataSize.ofMegabytes(1));
  }

  @Test
  void testCustomizeHeaderSizeUseBytesByDefault() {
    bind("server.max-http-request-header-size", "1024");
    assertThat(this.properties.maxHttpRequestHeaderSize).isEqualTo(DataSize.ofKilobytes(1));
  }

  @Test
  void testCustomizeMaxHttpRequestHeaderSize() {
    bind("server.max-http-request-header-size", "1MB");
    assertThat(this.properties.maxHttpRequestHeaderSize).isEqualTo(DataSize.ofMegabytes(1));
  }

  @Test
  void testCustomizeMaxHttpRequestHeaderSizeUseBytesByDefault() {
    bind("server.max-http-request-header-size", "1024");
    assertThat(this.properties.maxHttpRequestHeaderSize).isEqualTo(DataSize.ofKilobytes(1));
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
