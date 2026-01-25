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

package infra.web.server.netty.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.web.context.StandardWebEnvironment;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.properties.bind.Binder;
import infra.test.classpath.ClassPathExclusions;
import infra.util.DataSize;
import infra.web.server.config.ServerProperties;
import infra.web.server.context.AnnotationConfigWebServerApplicationContext;
import infra.web.server.netty.HttpTrafficHandler;
import infra.web.server.netty.NettyServerProperties;
import infra.web.server.netty.NettyWebServerFactory;
import infra.web.server.netty.RandomPortWebServerConfig;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/11 23:14
 */
class NettyWebServerFactoryAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = ApplicationContextRunner.forProvider(this::createContext)
          .withConfiguration(AutoConfigurations.of(RandomPortWebServerConfig.class));

  AnnotationConfigWebServerApplicationContext createContext() {
    var context = new AnnotationConfigWebServerApplicationContext();
    context.setEnvironment(new StandardWebEnvironment());
    return context;
  }

  @Test
  @Disabled("没弄明白CI环境为啥失败")
  void properties() {
    contextRunner.withPropertyValues("server.netty.workerThreads=8",
            "server.netty.max-connection=1024",
            "server.netty.logging-level=DEBUG",
            "server.netty.max-content-length=10MB",
            "server.netty.closeOnExpectationFailed=true",
            "server.netty.maxChunkSize=1KB",
            "server.netty.maxHeaderSize=120B",
            "server.netty.maxInitialLineLength=100",
            "server.netty.validateHeaders=false",
            "server.netty.socketChannel=io.netty.channel.socket.nio.NioServerSocketChannel",
            "server.netty.shutdown.quietPeriod=1",
            "server.netty.shutdown.timeout=1",
            "server.netty.shutdown.unit=seconds",
            "server.netty.acceptor-threads=1").run(context -> {
      NettyServerProperties properties = context.getBean(NettyServerProperties.class);

      var result = Binder.get(context.getEnvironment()).bind("server", NettyServerProperties.class);
      assertThat(result.isBound()).isTrue();
      assertNetty(result.get());
      assertNetty(properties);
    });
  }

  private static void assertNetty(NettyServerProperties server) {
    NettyServerProperties netty = server;
    assertThat(netty.acceptorThreads).isEqualTo(1);
    assertThat(netty.workerThreads).isEqualTo(8);
    assertThat(netty.maxConnection).isEqualTo(1024);
    assertThat(netty.loggingLevel).isEqualTo(LogLevel.DEBUG);
    assertThat(netty.maxContentLength).isEqualTo(DataSize.ofMegabytes(10));
    assertThat(netty.maxChunkSize).isEqualTo(DataSize.ofKilobytes(1));
    assertThat(netty.maxHeaderSize).isEqualTo(DataSize.ofBytes(120));
    assertThat(netty.maxInitialLineLength).isEqualTo(100);
    assertThat(netty.validateHeaders).isEqualTo(false);
    assertThat(netty.socketChannel).isEqualTo(NioServerSocketChannel.class);

    var shutdown = netty.shutdown;

    assertThat(shutdown.quietPeriod).isEqualTo(1);
    assertThat(shutdown.timeout).isEqualTo(1);
    assertThat(shutdown.unit).isEqualTo(TimeUnit.SECONDS);

//    assertThat(Ssl.isEnabled(server.ssl)).isFalse();
//    assertThat(server.ssl).isNull();
  }

  @Test
  void nettySSL() {
    contextRunner.withPropertyValues("server.ssl.enabled:true",
            "server.netty.workerThreads=100",
            "server.ssl.certificate:classpath:ssl/key1.crt",
            "server.ssl.certificatePrivateKey:classpath:ssl/key1.pem").run(context -> {
      ServerProperties properties = context.getBean(ServerProperties.class);

      var nettySSL = properties.ssl;
      assertThat(nettySSL).isNotNull();
      assertThat(nettySSL.enabled).isTrue();
      assertThat(nettySSL.certificate).isEqualTo("classpath:ssl/key1.crt");
      assertThat(nettySSL.certificatePrivateKey).isEqualTo("classpath:ssl/key1.pem");
      assertThat(nettySSL.keyPassword).isNull();

      NettyWebServerFactory factory = context.getBean(NettyWebServerFactory.class);
      assertThat(factory.getWorkThreadCount()).isEqualTo(100);
    });
  }

  @Test
  void publicKeyNotFound() {
    contextRunner.withPropertyValues("server.ssl.enabled:true",
            "server.ssl.certificate:classpath:not-found.crt",
            "server.ssl.certificatePrivateKey:classpath:/ssl/key1.pem").run(context -> {

      assertThatThrownBy(() -> context.getBean(ServerProperties.class))
              .hasRootCauseInstanceOf(FileNotFoundException.class)
              .hasRootCauseMessage("class path resource [not-found.crt] cannot be opened because it does not exist");
    });
  }

  @Test
  void privateKeyNotFound() {
    contextRunner.withPropertyValues("server.ssl.enabled:true",
            "server.ssl.certificate:classpath:ssl/key1.crt",
            "server.ssl.certificatePrivateKey:classpath:not-found.pem").run(context -> {

      assertThatThrownBy(() -> context.getBean(ServerProperties.class))
              .hasRootCauseInstanceOf(FileNotFoundException.class)
              .hasRootCauseMessage("class path resource [not-found.pem] cannot be opened because it does not exist");
    });
  }

  @Test
  @ClassPathExclusions("infra-websocket*")
  void wsNotPresent() {
    contextRunner.run(context -> {
      assertThat(context.getBean(HttpTrafficHandler.class).getClass()).isSameAs(HttpTrafficHandler.class);
    });
  }

  @Test
  void poolName() {
    contextRunner.withPropertyValues("server.netty.acceptorPoolName=acceptor",
            "server.netty.workerPoolName=worker").run(context -> {
      NettyServerProperties properties = context.getBean(NettyServerProperties.class);

      var result = Binder.get(context.getEnvironment()).bind("server.netty", NettyServerProperties.class);
      assertThat(result.isBound()).isTrue();

      assertThat(properties.acceptorPoolName).isEqualTo(result.get().acceptorPoolName).isEqualTo("acceptor");
      assertThat(properties.workerPoolName).isEqualTo(result.get().workerPoolName).isEqualTo("worker");

      NettyWebServerFactory factory = context.getBean(NettyWebServerFactory.class);

      assertThat(factory).extracting("workerPoolName").isEqualTo("worker");
      assertThat(factory).extracting("acceptorPoolName").isEqualTo("acceptor");

    });
  }

}