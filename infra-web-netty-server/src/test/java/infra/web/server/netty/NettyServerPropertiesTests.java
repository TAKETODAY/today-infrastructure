package infra.web.server.netty;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.MapConfigurationPropertySource;
import infra.util.DataSize;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.util.ResourceLeakDetector;

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

  @Test
  void workerPoolName() {
    assertThat(properties.workerPoolName).isNull();

    bind("server.netty.worker-pool-name", "worker");
    assertThat(properties.workerPoolName).isEqualTo("worker");
  }

  @Test
  void acceptorPoolName() {
    assertThat(properties.acceptorPoolName).isNull();

    bind("server.netty.acceptor-pool-name", "acceptor");
    assertThat(properties.acceptorPoolName).isEqualTo("acceptor");
  }

  @Test
  void maxContentLength() {
    assertThat(properties.maxContentLength.toBytes()).isEqualTo(DataSize.ofMegabytes(100).toBytes());

    bind("server.netty.max-content-length", "1MB");
    assertThat(properties.maxContentLength.toBytes()).isEqualTo(DataSize.ofMegabytes(1).toBytes());
  }

  @Test
  void maxChunkSize() {
    bind("server.netty.max-chunk-size", "16KB");
    assertThat(properties.maxChunkSize.toBytes()).isEqualTo(DataSize.ofBytes(16384).toBytes());
  }

  @Test
  void initialBufferSize() {
    assertThat(properties.initialBufferSize.toBytes()).isEqualTo(DataSize.ofBytes(128).toBytes());

    bind("server.netty.initial-buffer-size", "256B");
    assertThat(properties.initialBufferSize.toBytes()).isEqualTo(DataSize.ofBytes(256).toBytes());
  }

  @Test
  void maxHeaderSize() {
    bind("server.netty.max-header-size", "16KB");
    assertThat(properties.maxHeaderSize.toBytes()).isEqualTo(DataSize.ofBytes(16384).toBytes());
  }

  @Test
  void maxInitialLineLength() {
    assertThat(properties.maxInitialLineLength).isEqualTo(4096);

    bind("server.netty.max-initial-line-length", "8192");
    assertThat(properties.maxInitialLineLength).isEqualTo(8192);
  }

  @Test
  void validateHeaders() {
    assertThat(properties.validateHeaders).isTrue();

    bind("server.netty.validate-headers", "false");
    assertThat(properties.validateHeaders).isFalse();
  }

  @Test
  void chunkedSupported() {
    assertThat(properties.chunkedSupported).isTrue();

    bind("server.netty.chunked-supported", "false");
    assertThat(properties.chunkedSupported).isFalse();
  }

  @Test
  void allowPartialChunks() {
    assertThat(properties.allowPartialChunks).isTrue();

    bind("server.netty.allow-partial-chunks", "false");
    assertThat(properties.allowPartialChunks).isFalse();
  }

  @Test
  void allowDuplicateContentLengths() {
    assertThat(properties.allowDuplicateContentLengths).isFalse();

    bind("server.netty.allow-duplicate-content-lengths", "true");
    assertThat(properties.allowDuplicateContentLengths).isTrue();
  }

  @Test
  void writerAutoFlush() {
    assertThat(properties.writerAutoFlush).isFalse();

    bind("server.netty.writer-auto-flush", "true");
    assertThat(properties.writerAutoFlush).isTrue();
  }

  @Test
  void dataReceivedQueueCapacity() {
    assertThat(properties.dataReceivedQueueCapacity).isEqualTo(256);

    bind("server.netty.data-received-queue-capacity", "512");
    assertThat(properties.dataReceivedQueueCapacity).isEqualTo(512);
  }

  @Test
  void autoRead() {
    assertThat(properties.autoRead).isTrue();

    bind("server.netty.auto-read", "false");
    assertThat(properties.autoRead).isFalse();
  }

  @Test
  void leakDetection() {
    assertThat(properties.leakDetection).isEqualTo(ResourceLeakDetector.Level.DISABLED);

    bind("server.netty.leak-detection", "PARANOID");
    assertThat(properties.leakDetection).isEqualTo(ResourceLeakDetector.Level.PARANOID);
  }

  @Test
  void connectionTimeout() {
    assertThat(properties.connectionTimeout).isNull();

    bind("server.netty.connection-timeout", "30s");
    assertThat(properties.connectionTimeout).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  void autoAckPingFrame() {
    assertThat(properties.autoAckPingFrame).isTrue();

    bind("server.netty.auto-ack-ping-frame", "false");
    assertThat(properties.autoAckPingFrame).isFalse();
  }

  @Test
  void autoAckSettingsFrame() {
    assertThat(properties.autoAckSettingsFrame).isTrue();

    bind("server.netty.auto-ack-settings-frame", "false");
    assertThat(properties.autoAckSettingsFrame).isFalse();
  }

  @Test
  void http2GracefulShutdownTimeout() {
    assertThat(properties.http2GracefulShutdownTimeout).isEqualTo(Duration.ofSeconds(30));

    bind("server.netty.http2-graceful-shutdown-timeout", "29s");
    assertThat(properties.http2GracefulShutdownTimeout).isEqualTo(Duration.ofSeconds(29));
  }

  @Test
  void httpRequestExecutor() {
    NettyServerProperties.HttpRequestExecutor executor = properties.httpRequestExecutor;
    assertThat(executor.coreSize).isEqualTo(8);
    assertThat(executor.maxSize).isEqualTo(Integer.MAX_VALUE);
    assertThat(executor.queueCapacity).isEqualTo(Integer.MAX_VALUE);
    assertThat(executor.allowCoreThreadTimeOut).isTrue();
    assertThat(executor.keepAlive).isEqualTo(Duration.ofSeconds(60));
    assertThat(executor.threadNamePrefix).isEqualTo("task-");

    bind(Map.of(
            "server.netty.http-request-executor.core-size", "4",
            "server.netty.http-request-executor.max-size", "64",
            "server.netty.http-request-executor.queue-capacity", "1024",
            "server.netty.http-request-executor.allow-core-thread-time-out", "false",
            "server.netty.http-request-executor.keep-alive", "30s",
            "server.netty.http-request-executor.thread-name-prefix", "http-"
    ));
    assertThat(executor.coreSize).isEqualTo(4);
    assertThat(executor.maxSize).isEqualTo(64);
    assertThat(executor.queueCapacity).isEqualTo(1024);
    assertThat(executor.allowCoreThreadTimeOut).isFalse();
    assertThat(executor.keepAlive).isEqualTo(Duration.ofSeconds(30));
    assertThat(executor.threadNamePrefix).isEqualTo("http-");
  }

  @Test
  void shutdown() {
    NettyServerProperties.Shutdown shutdown = properties.shutdown;
    assertThat(shutdown.quietPeriod).isEqualTo(1);
    assertThat(shutdown.timeout).isEqualTo(10);
    assertThat(shutdown.unit).isEqualTo(TimeUnit.SECONDS);

    bind(Map.of(
            "server.netty.shutdown.quiet-period", "3",
            "server.netty.shutdown.timeout", "5",
            "server.netty.shutdown.unit", "MINUTES"
    ));
    assertThat(shutdown.quietPeriod).isEqualTo(3);
    assertThat(shutdown.timeout).isEqualTo(5);
    assertThat(shutdown.unit).isEqualTo(TimeUnit.MINUTES);
  }

  private void bind(String name, String value) {
    bind(Collections.singletonMap(name, value));
  }

  private void bind(Map<String, String> map) {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
    new Binder(source).bind("server.netty", Bindable.ofInstance(this.properties));
  }

}