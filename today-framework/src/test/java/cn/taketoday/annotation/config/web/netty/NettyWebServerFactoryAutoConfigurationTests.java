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

package cn.taketoday.annotation.config.web.netty;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import cn.taketoday.annotation.config.web.RandomPortWebServerConfig;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.web.server.context.AnnotationConfigWebServerApplicationContext;
import cn.taketoday.framework.web.netty.NettyWebServerFactory;
import cn.taketoday.framework.web.netty.StandardNettyWebEnvironment;
import cn.taketoday.web.server.ServerProperties;
import cn.taketoday.web.server.Ssl;
import cn.taketoday.util.DataSize;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 21:03
 */
class NettyWebServerFactoryAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = ApplicationContextRunner.forProvider(this::createContext)
          .withConfiguration(AutoConfigurations.of(RandomPortWebServerConfig.class));

  AnnotationConfigWebServerApplicationContext createContext() {
    var context = new AnnotationConfigWebServerApplicationContext();
    context.setEnvironment(new StandardNettyWebEnvironment());
    return context;
  }

  @Test
  void properties() {
    contextRunner.withPropertyValues("server.netty.workerThreads=8",
            "server.netty.max-connection=1024",
            "server.netty.logging-level=DEBUG",
            "server.netty.max-content-length=10MB",
            "server.netty.closeOnExpectationFailed=true",
            "server.netty.maxChunkSize=1KB",
            "server.netty.maxHeaderSize=120",
            "server.netty.maxInitialLineLength=100",
            "server.netty.validateHeaders=false",
            "server.netty.socketChannel=io.netty.channel.socket.nio.NioServerSocketChannel",
            "server.netty.shutdown.quietPeriod=1",
            "server.netty.shutdown.timeout=1",
            "server.netty.shutdown.unit=seconds",
            "server.netty.acceptor-threads=1").run(context -> {
      ServerProperties properties = context.getBean(ServerProperties.class);

      var result = Binder.get(context.getEnvironment()).bind("server", ServerProperties.class);
      assertThat(result.isBound()).isTrue();
      assertNetty(result.get());
      assertNetty(properties);
    });
  }

  private static void assertNetty(ServerProperties server) {
    ServerProperties.Netty netty = server.netty;
    assertThat(netty.acceptorThreads).isEqualTo(1);
    assertThat(netty.workerThreads).isEqualTo(8);
    assertThat(netty.maxConnection).isEqualTo(1024);
    assertThat(netty.loggingLevel).isEqualTo(LogLevel.DEBUG);
    assertThat(netty.maxContentLength).isEqualTo(DataSize.ofMegabytes(10));
    assertThat(netty.closeOnExpectationFailed).isEqualTo(true);
    assertThat(netty.maxChunkSize).isEqualTo(DataSize.ofKilobytes(1));
    assertThat(netty.maxHeaderSize).isEqualTo(120);
    assertThat(netty.maxInitialLineLength).isEqualTo(100);
    assertThat(netty.validateHeaders).isEqualTo(false);
    assertThat(netty.socketChannel).isEqualTo(NioServerSocketChannel.class);

    var shutdown = netty.shutdown;

    assertThat(shutdown.quietPeriod).isEqualTo(1);
    assertThat(shutdown.timeout).isEqualTo(1);
    assertThat(shutdown.unit).isEqualTo(TimeUnit.SECONDS);

    assertThat(Ssl.isEnabled(server.ssl)).isFalse();
    assertThat(server.ssl).isNull();
  }

  @Test
  void nettySSL() {
    contextRunner.withPropertyValues("server.ssl.enabled:true",
            "server.netty.workerThreads=100",
            "server.ssl.certificate:classpath:cn/taketoday/annotation/config/ssl/key1.crt",
            "server.ssl.certificatePrivateKey:classpath:cn/taketoday/annotation/config/ssl/key1.pem").run(context -> {
      ServerProperties properties = context.getBean(ServerProperties.class);

      var nettySSL = properties.ssl;
      assertThat(nettySSL).isNotNull();
      assertThat(nettySSL.enabled).isTrue();
      assertThat(nettySSL.certificate).isEqualTo("classpath:cn/taketoday/annotation/config/ssl/key1.crt");
      assertThat(nettySSL.certificatePrivateKey).isEqualTo("classpath:cn/taketoday/annotation/config/ssl/key1.pem");
      assertThat(nettySSL.keyPassword).isNull();

      NettyWebServerFactory factory = context.getBean(NettyWebServerFactory.class);
      assertThat(factory.getWorkThreadCount()).isEqualTo(100);
    });
  }

  @Test
  void publicKeyNotFound() {
    contextRunner.withPropertyValues("server.ssl.enabled:true",
            "server.ssl.certificate:classpath:not-found.crt",
            "server.ssl.certificatePrivateKey:classpath:/cn/taketoday/annotation/config/ssl/key1.pem").run(context -> {

      assertThatThrownBy(() -> context.getBean(ServerProperties.class))
              .hasRootCauseInstanceOf(FileNotFoundException.class)
              .hasRootCauseMessage("class path resource [not-found.crt] cannot be resolved to URL because it does not exist");
    });
  }

  @Test
  void privateKeyNotFound() {
    contextRunner.withPropertyValues("server.ssl.enabled:true",
            "server.ssl.certificate:classpath:cn/taketoday/annotation/config/ssl/key1.crt",
            "server.ssl.certificatePrivateKey:classpath:not-found.pem").run(context -> {

      assertThatThrownBy(() -> context.getBean(ServerProperties.class))
              .hasRootCauseInstanceOf(FileNotFoundException.class)
              .hasRootCauseMessage("class path resource [not-found.pem] cannot be resolved to URL because it does not exist");
    });
  }

}