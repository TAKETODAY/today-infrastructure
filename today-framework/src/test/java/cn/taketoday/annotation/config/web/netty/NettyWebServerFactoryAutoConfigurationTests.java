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

import java.util.concurrent.TimeUnit;

import cn.taketoday.annotation.config.web.ErrorMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.web.context.AnnotationConfigWebServerApplicationContext;
import cn.taketoday.framework.web.netty.NettyChannelInitializer;
import cn.taketoday.framework.web.netty.NettyWebServerFactory;
import cn.taketoday.framework.web.netty.SSLNettyChannelInitializer;
import cn.taketoday.framework.web.netty.StandardNettyWebEnvironment;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactory;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.DataSize;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.handler.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 21:03
 */
class NettyWebServerFactoryAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = ApplicationContextRunner.forProvider(this::createContext)
          .withConfiguration(AutoConfigurations.of(
                  NettyWebServerFactoryAutoConfiguration.class, WebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class))
          .withUserConfiguration(WebServerConfiguration.class);

  AnnotationConfigWebServerApplicationContext createContext() {
    var context = new AnnotationConfigWebServerApplicationContext();
    context.setEnvironment(new StandardNettyWebEnvironment());
    return context;
  }

  @Test
  void webServerFactory() {
    contextRunner.run(context -> {
      NettyWebServerFactory factory = context.getBean(NettyWebServerFactory.class);
      assertThat(factory.getWorkThreadCount()).isEqualTo(100);
    });
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
            "server.netty.socketChannel=io.netty.channel.epoll.EpollServerSocketChannel",
            "server.netty.shutdown.quietPeriod=2",
            "server.netty.shutdown.timeout=20",
            "server.netty.shutdown.unit=minutes",
            "server.netty.acceptor-threads=1").run(context -> {
      ServerProperties properties = context.getBean(ServerProperties.class);

      var result = Binder.get(context.getEnvironment()).bind("server", ServerProperties.class);
      assertThat(result.isBound()).isTrue();
      assertNetty(result.get().netty);
      assertNetty(properties.netty);
    });
  }

  private static void assertNetty(ServerProperties.Netty netty) {
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
    assertThat(netty.socketChannel).isEqualTo(EpollServerSocketChannel.class);

    var shutdown = netty.shutdown;

    assertThat(shutdown.quietPeriod).isEqualTo(2);
    assertThat(shutdown.timeout).isEqualTo(20);
    assertThat(shutdown.unit).isEqualTo(TimeUnit.MINUTES);

    var nettySSL = netty.ssl;
    assertThat(nettySSL.enabled).isFalse();
    assertThat(nettySSL.publicKey).isNull();
    assertThat(nettySSL.privateKey).isNull();
    assertThat(nettySSL.keyPassword).isNull();
  }

  @Test
  void nettySSL() {
    contextRunner.withPropertyValues("server.netty.ssl.enabled:true",
            "server.netty.ssl.public-key:classpath:/cn/taketoday/annotation/config/ssl/key1.crt",
            "server.netty.ssl.private-key:classpath:/cn/taketoday/annotation/config/ssl/key1.pem").run(context -> {
      ServerProperties properties = context.getBean(ServerProperties.class);

      var nettySSL = properties.netty.ssl;
      assertThat(nettySSL.enabled).isTrue();
      assertThat(nettySSL.publicKey).isEqualTo("classpath:/cn/taketoday/annotation/config/ssl/key1.crt");
      assertThat(nettySSL.privateKey).isEqualTo("classpath:/cn/taketoday/annotation/config/ssl/key1.pem");
      assertThat(nettySSL.keyPassword).isNull();

      NettyWebServerFactory factory = context.getBean(NettyWebServerFactory.class);
      assertThat(factory.getWorkThreadCount()).isEqualTo(100);
      assertThat(factory.getNettyChannelInitializer()).isInstanceOf(SSLNettyChannelInitializer.class);
    });
  }

  @Test
  void publicKeyNotFound() {
    contextRunner.withPropertyValues("server.netty.ssl.enabled:true",
            "server.netty.ssl.public-key:classpath:not-found.crt",
            "server.netty.ssl.private-key:classpath:/cn/taketoday/annotation/config/ssl/key1.pem").run(context -> {

      assertThatThrownBy(() -> context.getBean(ServerProperties.class))
              .hasRootCauseInstanceOf(IllegalStateException.class)
              .hasRootCauseMessage("publicKey not found");
    });
  }

  @Test
  void privateKeyNotFound() {
    contextRunner.withPropertyValues("server.netty.ssl.enabled:true",
            "server.netty.ssl.public-key:classpath:cn/taketoday/annotation/config/ssl/key1.crt",
            "server.netty.ssl.private-key:classpath:not-found.pem").run(context -> {

      assertThatThrownBy(() -> context.getBean(ServerProperties.class))
              .hasRootCauseInstanceOf(IllegalStateException.class)
              .hasRootCauseMessage("privateKey not found");
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class WebServerConfiguration {

    @Component
    static WebServerFactory webServerFactory(NettyChannelInitializer nettyChannelInitializer) {
      NettyWebServerFactory factory = new NettyWebServerFactory();
      factory.setWorkerThreadCount(100);
      factory.setPort(0);
      factory.setNettyChannelInitializer(nettyChannelInitializer);
      return factory;
    }

  }
}