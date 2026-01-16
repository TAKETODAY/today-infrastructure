package infra.web.server.netty;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.MapConfigurationPropertySource;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/14 20:34
 */
class NettyServerPropertiesTests {

  private final NettyServerProperties properties = new NettyServerProperties();

  @Test
  void nettyWorkThreadCount() {
    assertThat(properties.workerThreads).isNull();

    bind("server.netty.workerThreads", "10");
    assertThat(properties.workerThreads).isEqualTo(10);

    bind("server.netty.worker-threads", "100");
    assertThat(properties.workerThreads).isEqualTo(100);
  }

  @Test
  void nettyBossThreadCount() {
    assertThat(properties.acceptorThreads).isNull();
    bind("server.netty.acceptorThreads", "10");
    assertThat(properties.acceptorThreads).isEqualTo(10);

    bind("server.netty.acceptor-threads", "100");
    assertThat(properties.acceptorThreads).isEqualTo(100);
  }

  @Test
  void nettyLoggingLevel() {
    assertThat(properties.loggingLevel).isNull();

    bind("server.netty.loggingLevel", "INFO");
    assertThat(properties.loggingLevel).isEqualTo(LogLevel.INFO);

    bind("server.netty.logging-level", "DEBUG");
    assertThat(properties.loggingLevel).isEqualTo(LogLevel.DEBUG);
  }

  @Test
  void nettySocketChannel() {
    assertThat(properties.socketChannel).isNull();

    bind("server.netty.socketChannel", "io.netty.channel.socket.nio.NioServerSocketChannel");
    assertThat(properties.socketChannel).isEqualTo(NioServerSocketChannel.class);

    bind("server.netty.socket-channel", "io.netty.channel.socket.nio.NioServerSocketChannel");
    assertThat(properties.socketChannel).isEqualTo(NioServerSocketChannel.class);
  }

  @Test
  void maxConnection() {
    bind("server.netty.maxConnection", "100");
    assertThat(properties.maxConnection).isEqualTo(100);

    bind("server.netty.max-connection", "1000");
    assertThat(properties.maxConnection).isEqualTo(1000);
  }

  private void bind(String name, String value) {
    bind(Collections.singletonMap(name, value));
  }

  private void bind(Map<String, String> map) {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
    new Binder(source).bind("server.netty", Bindable.ofInstance(this.properties));
  }

}