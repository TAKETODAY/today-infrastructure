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

package cn.taketoday.annotation.config.web.embedded;

import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.ConnectionFactory;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Function;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.framework.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.jetty.JettyWebServer;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.ServerProperties.Jetty;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.test.context.support.TestPropertySourceUtils;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.test.web.servlet.DirtiesUrlFactories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author HaiTao Zhang
 */
@DirtiesUrlFactories
class JettyWebServerFactoryCustomizerTests {

  private MockEnvironment environment;

  private ServerProperties serverProperties;

  private JettyWebServerFactoryCustomizer customizer;

  @BeforeEach
  void setup() {
    this.environment = new MockEnvironment();
    this.serverProperties = new ServerProperties();
    ConfigurationPropertySources.attach(this.environment);
    this.customizer = new JettyWebServerFactoryCustomizer(this.environment, this.serverProperties);
  }

  @Test
  void deduceUseForwardHeaders() {
    this.environment.setProperty("DYNO", "-");
    ConfigurableJettyWebServerFactory factory = mock(ConfigurableJettyWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(true);
  }

  @Test
  void defaultUseForwardHeaders() {
    ConfigurableJettyWebServerFactory factory = mock(ConfigurableJettyWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(false);
  }

  @Test
  void forwardHeadersWhenStrategyIsNativeShouldConfigureValve() {
    this.serverProperties.forwardHeadersStrategy = (ServerProperties.ForwardHeadersStrategy.NATIVE);
    ConfigurableJettyWebServerFactory factory = mock(ConfigurableJettyWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(true);
  }

  @Test
  void forwardHeadersWhenStrategyIsNoneShouldNotConfigureValve() {
    this.environment.setProperty("DYNO", "-");
    this.serverProperties.forwardHeadersStrategy = (ServerProperties.ForwardHeadersStrategy.NONE);
    ConfigurableJettyWebServerFactory factory = mock(ConfigurableJettyWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(false);
  }

  @Test
  void accessLogCanBeCustomized() throws IOException {
    File logFile = ApplicationTemp.instance.createFile(
            null, "jetty_log", ".log").toFile();
    bind("server.jetty.accesslog.enabled=true", "server.jetty.accesslog.format=extended_ncsa",
            "server.jetty.accesslog.filename=" + logFile.getAbsolutePath().replace("\\", "\\\\"),
            "server.jetty.accesslog.file-date-format=yyyy-MM-dd", "server.jetty.accesslog.retention-period=42",
            "server.jetty.accesslog.append=true", "server.jetty.accesslog.ignore-paths=/a/path,/b/path");
    JettyWebServer server = customizeAndGetServer();
    CustomRequestLog requestLog = getRequestLog(server);
    assertThat(requestLog.getFormatString()).isEqualTo(CustomRequestLog.EXTENDED_NCSA_FORMAT);
    assertThat(requestLog.getIgnorePaths()).hasSize(2);
    assertThat(requestLog.getIgnorePaths()).containsExactly("/a/path", "/b/path");
    RequestLogWriter logWriter = getLogWriter(requestLog);
    assertThat(logWriter.getFileName()).isEqualTo(logFile.getAbsolutePath());
    assertThat(logWriter.getFilenameDateFormat()).isEqualTo("yyyy-MM-dd");
    assertThat(logWriter.getRetainDays()).isEqualTo(42);
    assertThat(logWriter.isAppend()).isTrue();
  }

  @Test
  void accessLogCanBeEnabled() {
    bind("server.jetty.accesslog.enabled=true");
    JettyWebServer server = customizeAndGetServer();
    CustomRequestLog requestLog = getRequestLog(server);
    assertThat(requestLog.getFormatString()).isEqualTo(CustomRequestLog.NCSA_FORMAT);
    assertThat(requestLog.getIgnorePaths()).isNull();
    RequestLogWriter logWriter = getLogWriter(requestLog);
    assertThat(logWriter.getFileName()).isNull();
    assertThat(logWriter.isAppend()).isFalse();
  }

  @Test
  void threadPoolMatchesJettyDefaults() {
    ThreadPool defaultThreadPool = new Server(0).getThreadPool();
    ThreadPool configuredThreadPool = customizeAndGetServer().getServer().getThreadPool();
    assertThat(defaultThreadPool).isInstanceOf(QueuedThreadPool.class);
    assertThat(configuredThreadPool).isInstanceOf(QueuedThreadPool.class);
    QueuedThreadPool defaultQueuedThreadPool = (QueuedThreadPool) defaultThreadPool;
    QueuedThreadPool configuredQueuedThreadPool = (QueuedThreadPool) configuredThreadPool;
    assertThat(configuredQueuedThreadPool.getMinThreads()).isEqualTo(defaultQueuedThreadPool.getMinThreads());
    assertThat(configuredQueuedThreadPool.getMaxThreads()).isEqualTo(defaultQueuedThreadPool.getMaxThreads());
    assertThat(configuredQueuedThreadPool.getIdleTimeout()).isEqualTo(defaultQueuedThreadPool.getIdleTimeout());
    BlockingQueue<?> defaultQueue = getQueue(defaultThreadPool);
    BlockingQueue<?> configuredQueue = getQueue(configuredThreadPool);
    assertThat(defaultQueue).isInstanceOf(BlockingArrayQueue.class);
    assertThat(configuredQueue).isInstanceOf(BlockingArrayQueue.class);
    assertThat(((BlockingArrayQueue<?>) defaultQueue).getMaxCapacity())
            .isEqualTo(((BlockingArrayQueue<?>) configuredQueue).getMaxCapacity());
  }

  @Test
  void threadPoolMaxThreadsCanBeCustomized() {
    bind("server.jetty.threads.max=100");
    JettyWebServer server = customizeAndGetServer();
    QueuedThreadPool threadPool = (QueuedThreadPool) server.getServer().getThreadPool();
    assertThat(threadPool.getMaxThreads()).isEqualTo(100);
  }

  @Test
  void threadPoolMinThreadsCanBeCustomized() {
    bind("server.jetty.threads.min=100");
    JettyWebServer server = customizeAndGetServer();
    QueuedThreadPool threadPool = (QueuedThreadPool) server.getServer().getThreadPool();
    assertThat(threadPool.getMinThreads()).isEqualTo(100);
  }

  @Test
  void threadPoolIdleTimeoutCanBeCustomized() {
    bind("server.jetty.threads.idle-timeout=100s");
    JettyWebServer server = customizeAndGetServer();
    QueuedThreadPool threadPool = (QueuedThreadPool) server.getServer().getThreadPool();
    assertThat(threadPool.getIdleTimeout()).isEqualTo(100000);
  }

  @Test
  void threadPoolWithMaxQueueCapacityEqualToZeroCreateSynchronousQueue() {
    bind("server.jetty.threads.max-queue-capacity=0");
    JettyWebServer server = customizeAndGetServer();
    ThreadPool threadPool = server.getServer().getThreadPool();
    BlockingQueue<?> queue = getQueue(threadPool);
    assertThat(queue).isInstanceOf(SynchronousQueue.class);
    assertDefaultThreadPoolSettings(threadPool);
  }

  @Test
  void threadPoolWithMaxQueueCapacityEqualToZeroCustomizesThreadPool() {
    bind("server.jetty.threads.max-queue-capacity=0", "server.jetty.threads.min=100",
            "server.jetty.threads.max=100", "server.jetty.threads.idle-timeout=6s");
    JettyWebServer server = customizeAndGetServer();
    QueuedThreadPool threadPool = (QueuedThreadPool) server.getServer().getThreadPool();
    assertThat(threadPool.getMinThreads()).isEqualTo(100);
    assertThat(threadPool.getMaxThreads()).isEqualTo(100);
    assertThat(threadPool.getIdleTimeout()).isEqualTo(Duration.ofSeconds(6).toMillis());
  }

  @Test
  void threadPoolWithMaxQueueCapacityPositiveCreateBlockingArrayQueue() {
    bind("server.jetty.threads.max-queue-capacity=1234");
    JettyWebServer server = customizeAndGetServer();
    ThreadPool threadPool = server.getServer().getThreadPool();
    BlockingQueue<?> queue = getQueue(threadPool);
    assertThat(queue).isInstanceOf(BlockingArrayQueue.class);
    assertThat(((BlockingArrayQueue<?>) queue).getMaxCapacity()).isEqualTo(1234);
    assertDefaultThreadPoolSettings(threadPool);
  }

  @Test
  void threadPoolWithMaxQueueCapacityPositiveCustomizesThreadPool() {
    bind("server.jetty.threads.max-queue-capacity=1234", "server.jetty.threads.min=10",
            "server.jetty.threads.max=150", "server.jetty.threads.idle-timeout=3s");
    JettyWebServer server = customizeAndGetServer();
    QueuedThreadPool threadPool = (QueuedThreadPool) server.getServer().getThreadPool();
    assertThat(threadPool.getMinThreads()).isEqualTo(10);
    assertThat(threadPool.getMaxThreads()).isEqualTo(150);
    assertThat(threadPool.getIdleTimeout()).isEqualTo(Duration.ofSeconds(3).toMillis());
  }

  private void assertDefaultThreadPoolSettings(ThreadPool threadPool) {
    assertThat(threadPool).isInstanceOf(QueuedThreadPool.class);
    QueuedThreadPool queuedThreadPool = (QueuedThreadPool) threadPool;
    Jetty defaultProperties = new Jetty();
    assertThat(queuedThreadPool.getMinThreads()).isEqualTo(defaultProperties.threads.min);
    assertThat(queuedThreadPool.getMaxThreads()).isEqualTo(defaultProperties.threads.max);
    assertThat(queuedThreadPool.getIdleTimeout())
            .isEqualTo(defaultProperties.threads.idleTimeout.toMillis());
  }

  private CustomRequestLog getRequestLog(JettyWebServer server) {
    RequestLog requestLog = server.getServer().getRequestLog();
    assertThat(requestLog).isInstanceOf(CustomRequestLog.class);
    return (CustomRequestLog) requestLog;
  }

  private RequestLogWriter getLogWriter(CustomRequestLog requestLog) {
    RequestLog.Writer writer = requestLog.getWriter();
    assertThat(writer).isInstanceOf(RequestLogWriter.class);
    return (RequestLogWriter) requestLog.getWriter();
  }

  @Test
  void setUseForwardHeaders() {
    this.serverProperties.forwardHeadersStrategy = (ServerProperties.ForwardHeadersStrategy.NATIVE);
    ConfigurableJettyWebServerFactory factory = mock(ConfigurableJettyWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setUseForwardHeaders(true);
  }

  @Test
  void customizeMaxHttpHeaderSize() {
    bind("server.max-http-request-header-size=2048");
    JettyWebServer server = customizeAndGetServer();
    List<Integer> requestHeaderSizes = getRequestHeaderSizes(server);
    assertThat(requestHeaderSizes).containsOnly(2048);
  }

  @Test
  void customMaxHttpHeaderSizeIgnoredIfNegative() {
    bind("server.max-http-request-header-size=-1");
    JettyWebServer server = customizeAndGetServer();
    List<Integer> requestHeaderSizes = getRequestHeaderSizes(server);
    assertThat(requestHeaderSizes).containsOnly(8192);
  }

  @Test
  void customMaxHttpHeaderSizeIgnoredIfZero() {
    bind("server.max-http-request-header-size=0");
    JettyWebServer server = customizeAndGetServer();
    List<Integer> requestHeaderSizes = getRequestHeaderSizes(server);
    assertThat(requestHeaderSizes).containsOnly(8192);
  }

  @Test
  void customizeMaxRequestHttpHeaderSize() {
    bind("server.max-http-request-header-size=2048");
    JettyWebServer server = customizeAndGetServer();
    List<Integer> requestHeaderSizes = getRequestHeaderSizes(server);
    assertThat(requestHeaderSizes).containsOnly(2048);
  }

  @Test
  void customMaxHttpRequestHeaderSizeIgnoredIfNegative() {
    bind("server.max-http-request-header-size=-1");
    JettyWebServer server = customizeAndGetServer();
    List<Integer> requestHeaderSizes = getRequestHeaderSizes(server);
    assertThat(requestHeaderSizes).containsOnly(8192);
  }

  @Test
  void customMaxHttpRequestHeaderSizeIgnoredIfZero() {
    bind("server.max-http-request-header-size=0");
    JettyWebServer server = customizeAndGetServer();
    List<Integer> requestHeaderSizes = getRequestHeaderSizes(server);
    assertThat(requestHeaderSizes).containsOnly(8192);
  }

  @Test
  void defaultMaxHttpResponseHeaderSize() {
    JettyWebServer server = customizeAndGetServer();
    List<Integer> responseHeaderSizes = getResponseHeaderSizes(server);
    assertThat(responseHeaderSizes).containsOnly(8192);
  }

  @Test
  void customizeMaxHttpResponseHeaderSize() {
    bind("server.jetty.max-http-response-header-size=2KB");
    JettyWebServer server = customizeAndGetServer();
    List<Integer> responseHeaderSizes = getResponseHeaderSizes(server);
    assertThat(responseHeaderSizes).containsOnly(2048);
  }

  @Test
  void customMaxHttpResponseHeaderSizeIgnoredIfNegative() {
    bind("server.jetty.max-http-response-header-size=-1");
    JettyWebServer server = customizeAndGetServer();
    List<Integer> responseHeaderSizes = getResponseHeaderSizes(server);
    assertThat(responseHeaderSizes).containsOnly(8192);
  }

  @Test
  void customMaxHttpResponseHeaderSizeIgnoredIfZero() {
    bind("server.jetty.max-http-response-header-size=0");
    JettyWebServer server = customizeAndGetServer();
    List<Integer> responseHeaderSizes = getResponseHeaderSizes(server);
    assertThat(responseHeaderSizes).containsOnly(8192);
  }

  @Test
  void customIdleTimeout() {
    bind("server.jetty.connection-idle-timeout=60s");
    JettyWebServer server = customizeAndGetServer();
    List<Long> timeouts = connectorsIdleTimeouts(server);
    assertThat(timeouts).containsOnly(60000L);
  }

  private List<Long> connectorsIdleTimeouts(JettyWebServer server) {
    // Start (and directly stop) server to have connectors available
    server.start();
    server.stop();
    return Arrays.stream(server.getServer().getConnectors())
            .filter((connector) -> connector instanceof AbstractConnector).map(Connector::getIdleTimeout).toList();
  }

  private List<Integer> getRequestHeaderSizes(JettyWebServer server) {
    return getHeaderSizes(server, HttpConfiguration::getRequestHeaderSize);
  }

  private List<Integer> getResponseHeaderSizes(JettyWebServer server) {
    return getHeaderSizes(server, HttpConfiguration::getResponseHeaderSize);
  }

  private List<Integer> getHeaderSizes(JettyWebServer server, Function<HttpConfiguration, Integer> provider) {
    List<Integer> requestHeaderSizes = new ArrayList<>();
    // Start (and directly stop) server to have connectors available
    server.start();
    server.stop();
    Connector[] connectors = server.getServer().getConnectors();
    for (Connector connector : connectors) {
      connector.getConnectionFactories()
              .stream()
              .filter(factory -> factory instanceof ConnectionFactory)
              .forEach(cf -> {
                ConnectionFactory factory = (ConnectionFactory) cf;
                HttpConfiguration configuration = factory.getHttpConfiguration();
                requestHeaderSizes.add(provider.apply(configuration));
              });
    }
    return requestHeaderSizes;
  }

  private BlockingQueue<?> getQueue(ThreadPool threadPool) {
    return ReflectionTestUtils.invokeMethod(threadPool, "getQueue");
  }

  private void bind(String... inlinedProperties) {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, inlinedProperties);
    new Binder(ConfigurationPropertySources.get(this.environment)).bind("server",
            Bindable.ofInstance(this.serverProperties));
  }

  private JettyWebServer customizeAndGetServer() {
    JettyServletWebServerFactory factory = customizeAndGetFactory();
    return (JettyWebServer) factory.getWebServer();
  }

  private JettyServletWebServerFactory customizeAndGetFactory() {
    JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
    this.customizer.customize(factory);
    return factory;
  }

}
