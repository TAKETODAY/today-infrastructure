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

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.jetty.JettyWebServer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.ServerProperties.Tomcat.Accesslog;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.test.web.servlet.DirtiesUrlFactories;
import cn.taketoday.util.DataSize;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.undertow.UndertowOptions;
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
@DirtiesUrlFactories
class ServerPropertiesTests {

  private final ServerProperties properties = new ServerProperties();

  @Test
  void testAddressBinding() throws Exception {
    bind("server.address", "127.0.0.1");
    assertThat(this.properties.getAddress()).isEqualTo(InetAddress.getByName("127.0.0.1"));
  }

  @Test
  void testPortBinding() {
    bind("server.port", "9000");
    assertThat(this.properties.getPort().intValue()).isEqualTo(9000);
  }

  @Test
  void testServerHeaderDefault() {
    assertThat(this.properties.getServerHeader()).isNull();
  }

  @Test
  void testServerHeader() {
    bind("server.server-header", "Custom Server");
    assertThat(this.properties.getServerHeader()).isEqualTo("Custom Server");
  }

  @Test
  void testTomcatBinding() {
    Map<String, String> map = new HashMap<>();
    map.put("server.tomcat.accesslog.conditionIf", "foo");
    map.put("server.tomcat.accesslog.conditionUnless", "bar");
    map.put("server.tomcat.accesslog.pattern", "%h %t '%r' %s %b");
    map.put("server.tomcat.accesslog.prefix", "foo");
    map.put("server.tomcat.accesslog.suffix", "-bar.log");
    map.put("server.tomcat.accesslog.encoding", "UTF-8");
    map.put("server.tomcat.accesslog.locale", "en-AU");
    map.put("server.tomcat.accesslog.checkExists", "true");
    map.put("server.tomcat.accesslog.rotate", "false");
    map.put("server.tomcat.accesslog.rename-on-rotate", "true");
    map.put("server.tomcat.accesslog.ipv6Canonical", "true");
    map.put("server.tomcat.accesslog.request-attributes-enabled", "true");
    map.put("server.tomcat.remoteip.protocol-header", "X-Forwarded-Protocol");
    map.put("server.tomcat.remoteip.remote-ip-header", "Remote-Ip");
    map.put("server.tomcat.remoteip.internal-proxies", "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    map.put("server.tomcat.remoteip.trusted-proxies", "proxy1|proxy2|proxy3");
    map.put("server.tomcat.reject-illegal-header", "false");
    map.put("server.tomcat.background-processor-delay", "10");
    map.put("server.tomcat.relaxed-path-chars", "|,<");
    map.put("server.tomcat.relaxed-query-chars", "^  ,  | ");
    map.put("server.tomcat.use-relative-redirects", "true");
    bind(map);
    ServerProperties.Tomcat tomcat = this.properties.tomcat;
    Accesslog accesslog = tomcat.getAccesslog();
    assertThat(accesslog.getConditionIf()).isEqualTo("foo");
    assertThat(accesslog.getConditionUnless()).isEqualTo("bar");
    assertThat(accesslog.getPattern()).isEqualTo("%h %t '%r' %s %b");
    assertThat(accesslog.getPrefix()).isEqualTo("foo");
    assertThat(accesslog.getSuffix()).isEqualTo("-bar.log");
    assertThat(accesslog.getEncoding()).isEqualTo("UTF-8");
    assertThat(accesslog.getLocale()).isEqualTo("en-AU");
    assertThat(accesslog.isCheckExists()).isEqualTo(true);
    assertThat(accesslog.isRotate()).isFalse();
    assertThat(accesslog.isRenameOnRotate()).isTrue();
    assertThat(accesslog.isIpv6Canonical()).isTrue();
    assertThat(accesslog.isRequestAttributesEnabled()).isTrue();
    assertThat(tomcat.getRemoteip().getRemoteIpHeader()).isEqualTo("Remote-Ip");
    assertThat(tomcat.getRemoteip().getProtocolHeader()).isEqualTo("X-Forwarded-Protocol");
    assertThat(tomcat.getRemoteip().getInternalProxies()).isEqualTo("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    assertThat(tomcat.getRemoteip().getTrustedProxies()).isEqualTo("proxy1|proxy2|proxy3");
    assertThat(tomcat.isRejectIllegalHeader()).isFalse();
    assertThat(tomcat.getBackgroundProcessorDelay()).hasSeconds(10);
    assertThat(tomcat.getRelaxedPathChars()).containsExactly('|', '<');
    assertThat(tomcat.getRelaxedQueryChars()).containsExactly('^', '|');
    assertThat(tomcat.isUseRelativeRedirects()).isTrue();
  }

  @Test
  void testTrailingSlashOfContextPathIsRemoved() {
    bind("server.servlet.context-path", "/foo/");
    assertThat(this.properties.servlet.getContextPath()).isEqualTo("/foo");
  }

  @Test
  void testSlashOfContextPathIsDefaultValue() {
    bind("server.servlet.context-path", "/");
    assertThat(this.properties.servlet.getContextPath()).isEqualTo("");
  }

  @Test
  void testContextPathWithLeadingWhitespace() {
    bind("server.servlet.context-path", " /assets");
    assertThat(this.properties.servlet.getContextPath()).isEqualTo("/assets");
  }

  @Test
  void testContextPathWithTrailingWhitespace() {
    bind("server.servlet.context-path", "/assets/copy/ ");
    assertThat(this.properties.servlet.getContextPath()).isEqualTo("/assets/copy");
  }

  @Test
  void testContextPathWithLeadingAndTrailingWhitespace() {
    bind("server.servlet.context-path", " /assets ");
    assertThat(this.properties.servlet.getContextPath()).isEqualTo("/assets");
  }

  @Test
  void testContextPathWithLeadingAndTrailingWhitespaceAndContextWithSpace() {
    bind("server.servlet.context-path", "  /assets /copy/    ");
    assertThat(this.properties.servlet.getContextPath()).isEqualTo("/assets /copy");
  }

  @Test
  void testCustomizeUriEncoding() {
    bind("server.tomcat.uri-encoding", "US-ASCII");
    assertThat(this.properties.tomcat.getUriEncoding()).isEqualTo(StandardCharsets.US_ASCII);
  }

  @Test
  void testCustomizeHeaderSize() {
    bind("server.max-http-request-header-size", "1MB");
    assertThat(this.properties.getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofMegabytes(1));
  }

  @Test
  void testCustomizeHeaderSizeUseBytesByDefault() {
    bind("server.max-http-request-header-size", "1024");
    assertThat(this.properties.getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(1));
  }

  @Test
  void testCustomizeMaxHttpRequestHeaderSize() {
    bind("server.max-http-request-header-size", "1MB");
    assertThat(this.properties.getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofMegabytes(1));
  }

  @Test
  void testCustomizeMaxHttpRequestHeaderSizeUseBytesByDefault() {
    bind("server.max-http-request-header-size", "1024");
    assertThat(this.properties.getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(1));
  }

  @Test
  void testCustomizeTomcatMaxThreads() {
    bind("server.tomcat.threads.max", "10");
    assertThat(this.properties.tomcat.getThreads().getMax()).isEqualTo(10);
  }

  @Test
  void testCustomizeTomcatKeepAliveTimeout() {
    bind("server.tomcat.keep-alive-timeout", "30s");
    assertThat(this.properties.tomcat.getKeepAliveTimeout()).hasSeconds(30);
  }

  @Test
  void testCustomizeTomcatKeepAliveTimeoutWithInfinite() {
    bind("server.tomcat.keep-alive-timeout", "-1");
    assertThat(this.properties.tomcat.getKeepAliveTimeout()).hasMillis(-1);
  }

  @Test
  void customizeMaxKeepAliveRequests() {
    bind("server.tomcat.max-keep-alive-requests", "200");
    assertThat(this.properties.tomcat.getMaxKeepAliveRequests()).isEqualTo(200);
  }

  @Test
  void customizeMaxKeepAliveRequestsWithInfinite() {
    bind("server.tomcat.max-keep-alive-requests", "-1");
    assertThat(this.properties.tomcat.getMaxKeepAliveRequests()).isEqualTo(-1);
  }

  @Test
  void testCustomizeTomcatMinSpareThreads() {
    bind("server.tomcat.threads.min-spare", "10");
    assertThat(this.properties.tomcat.getThreads().getMinSpare()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettyAcceptors() {
    bind("server.jetty.threads.acceptors", "10");
    assertThat(this.properties.jetty.getThreads().getAcceptors()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettySelectors() {
    bind("server.jetty.threads.selectors", "10");
    assertThat(this.properties.jetty.getThreads().getSelectors()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettyMaxThreads() {
    bind("server.jetty.threads.max", "10");
    assertThat(this.properties.jetty.getThreads().getMax()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettyMinThreads() {
    bind("server.jetty.threads.min", "10");
    assertThat(this.properties.jetty.getThreads().getMin()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettyIdleTimeout() {
    bind("server.jetty.threads.idle-timeout", "10s");
    assertThat(this.properties.jetty.getThreads().getIdleTimeout()).isEqualTo(Duration.ofSeconds(10));
  }

  @Test
  void testCustomizeJettyMaxQueueCapacity() {
    bind("server.jetty.threads.max-queue-capacity", "5150");
    assertThat(this.properties.jetty.getThreads().getMaxQueueCapacity()).isEqualTo(5150);
  }

  @Test
  void testCustomizeUndertowServerOption() {
    bind("server.undertow.options.server.ALWAYS_SET_KEEP_ALIVE", "true");
    assertThat(this.properties.undertow.getOptions().getServer()).containsEntry("ALWAYS_SET_KEEP_ALIVE",
            "true");
  }

  @Test
  void testCustomizeUndertowSocketOption() {
    bind("server.undertow.options.socket.ALWAYS_SET_KEEP_ALIVE", "true");
    assertThat(this.properties.undertow.getOptions().getSocket()).containsEntry("ALWAYS_SET_KEEP_ALIVE",
            "true");
  }

  @Test
  void testCustomizeUndertowIoThreads() {
    bind("server.undertow.threads.io", "4");
    assertThat(this.properties.undertow.getThreads().getIo()).isEqualTo(4);
  }

  @Test
  void testCustomizeUndertowWorkerThreads() {
    bind("server.undertow.threads.worker", "10");
    assertThat(this.properties.undertow.getThreads().getWorker()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettyAccessLog() {
    Map<String, String> map = new HashMap<>();
    map.put("server.jetty.accesslog.enabled", "true");
    map.put("server.jetty.accesslog.filename", "foo.txt");
    map.put("server.jetty.accesslog.file-date-format", "yyyymmdd");
    map.put("server.jetty.accesslog.retention-period", "4");
    map.put("server.jetty.accesslog.append", "true");
    map.put("server.jetty.accesslog.custom-format", "{client}a - %u %t \"%r\" %s %O");
    map.put("server.jetty.accesslog.ignore-paths", "/a/path,/b/path");
    bind(map);
    ServerProperties.Jetty jetty = this.properties.jetty;
    assertThat(jetty.getAccesslog().isEnabled()).isTrue();
    assertThat(jetty.getAccesslog().getFilename()).isEqualTo("foo.txt");
    assertThat(jetty.getAccesslog().getFileDateFormat()).isEqualTo("yyyymmdd");
    assertThat(jetty.getAccesslog().getRetentionPeriod()).isEqualTo(4);
    assertThat(jetty.getAccesslog().isAppend()).isTrue();
    assertThat(jetty.getAccesslog().getCustomFormat()).isEqualTo("{client}a - %u %t \"%r\" %s %O");
    assertThat(jetty.getAccesslog().getIgnorePaths()).containsExactly("/a/path", "/b/path");
  }

  @Test
  void testCustomizeNettyIdleTimeout() {
    bind("server.reactor-netty.idle-timeout", "10s");
    assertThat(this.properties.reactorNetty.getIdleTimeout()).isEqualTo(Duration.ofSeconds(10));
  }

  @Test
  void testCustomizeNettyMaxKeepAliveRequests() {
    bind("server.reactor-netty.max-keep-alive-requests", "100");
    assertThat(this.properties.reactorNetty.getMaxKeepAliveRequests()).isEqualTo(100);
  }

  @Test
  void tomcatAcceptCountMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.tomcat.getAcceptCount()).isEqualTo(getDefaultProtocol().getAcceptCount());
  }

  @Test
  void tomcatProcessorCacheMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.tomcat.getProcessorCache()).isEqualTo(getDefaultProtocol().getProcessorCache());
  }

  @Test
  void tomcatMaxConnectionsMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.tomcat.getMaxConnections()).isEqualTo(getDefaultProtocol().getMaxConnections());
  }

  @Test
  void tomcatMaxThreadsMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.tomcat.getThreads().getMax()).isEqualTo(getDefaultProtocol().getMaxThreads());
  }

  @Test
  void tomcatMinSpareThreadsMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.tomcat.getThreads().getMinSpare())
            .isEqualTo(getDefaultProtocol().getMinSpareThreads());
  }

  @Test
  void tomcatMaxHttpPostSizeMatchesConnectorDefault() {
    assertThat(this.properties.tomcat.getMaxHttpFormPostSize().toBytes())
            .isEqualTo(getDefaultConnector().getMaxPostSize());
  }

  @Test
  void tomcatBackgroundProcessorDelayMatchesEngineDefault() {
    assertThat(this.properties.tomcat.getBackgroundProcessorDelay())
            .hasSeconds((new StandardEngine().getBackgroundProcessorDelay()));
  }

  @Test
  void tomcatMaxHttpFormPostSizeMatchesConnectorDefault() {
    assertThat(this.properties.tomcat.getMaxHttpFormPostSize().toBytes())
            .isEqualTo(getDefaultConnector().getMaxPostSize());
  }

  @Test
  void tomcatUriEncodingMatchesConnectorDefault() {
    assertThat(this.properties.tomcat.getUriEncoding().name())
            .isEqualTo(getDefaultConnector().getURIEncoding());
  }

  @Test
  void tomcatRedirectContextRootMatchesDefault() {
    assertThat(this.properties.tomcat.getRedirectContextRoot())
            .isEqualTo(new StandardContext().getMapperContextRootRedirectEnabled());
  }

  @Test
  void tomcatAccessLogRenameOnRotateMatchesDefault() {
    assertThat(this.properties.tomcat.getAccesslog().isRenameOnRotate())
            .isEqualTo(new AccessLogValve().isRenameOnRotate());
  }

  @Test
  void tomcatAccessLogRequestAttributesEnabledMatchesDefault() {
    assertThat(this.properties.tomcat.getAccesslog().isRequestAttributesEnabled())
            .isEqualTo(new AccessLogValve().getRequestAttributesEnabled());
  }

  @Test
  void tomcatInternalProxiesMatchesDefault() {
    assertThat(this.properties.tomcat.getRemoteip().getInternalProxies())
            .isEqualTo(new RemoteIpValve().getInternalProxies());
  }

  @Test
  void tomcatRejectIllegalHeaderMatchesProtocolDefault() throws Exception {
    assertThat(getDefaultProtocol()).hasFieldOrPropertyWithValue("rejectIllegalHeader",
            this.properties.tomcat.isRejectIllegalHeader());
  }

  @Test
  void tomcatUseRelativeRedirectsDefaultsToFalse() {
    assertThat(this.properties.tomcat.isUseRelativeRedirects()).isFalse();
  }

  @Test
  void tomcatMaxKeepAliveRequestsDefault() throws Exception {
    AbstractEndpoint<?, ?> endpoint = (AbstractEndpoint<?, ?>) ReflectionTestUtils.getField(getDefaultProtocol(),
            "endpoint");
    int defaultMaxKeepAliveRequests = (int) ReflectionTestUtils.getField(endpoint, "maxKeepAliveRequests");
    assertThat(this.properties.tomcat.getMaxKeepAliveRequests()).isEqualTo(defaultMaxKeepAliveRequests);
  }

  @Test
  void jettyThreadPoolPropertyDefaultsShouldMatchServerDefault() {
    JettyServletWebServerFactory jettyFactory = new JettyServletWebServerFactory(0);
    JettyWebServer jetty = (JettyWebServer) jettyFactory.getWebServer();
    Server server = jetty.getServer();
    QueuedThreadPool threadPool = (QueuedThreadPool) server.getThreadPool();
    int idleTimeout = threadPool.getIdleTimeout();
    int maxThreads = threadPool.getMaxThreads();
    int minThreads = threadPool.getMinThreads();
    assertThat(this.properties.jetty.getThreads().getIdleTimeout().toMillis()).isEqualTo(idleTimeout);
    assertThat(this.properties.jetty.getThreads().getMax()).isEqualTo(maxThreads);
    assertThat(this.properties.jetty.getThreads().getMin()).isEqualTo(minThreads);
  }

  @Test
  void jettyMaxHttpFormPostSizeMatchesDefault() {
    JettyServletWebServerFactory jettyFactory = new JettyServletWebServerFactory(0);
    JettyWebServer jetty = (JettyWebServer) jettyFactory.getWebServer();
    Server server = jetty.getServer();
    assertThat(this.properties.jetty.getMaxHttpFormPostSize().toBytes())
            .isEqualTo(((ServletContextHandler) server.getHandler()).getMaxFormContentSize());
  }

  @Test
  void undertowMaxHttpPostSizeMatchesDefault() {
    assertThat(this.properties.undertow.getMaxHttpPostSize().toBytes())
            .isEqualTo(UndertowOptions.DEFAULT_MAX_ENTITY_SIZE);
  }

  @Test
  void nettyMaxChunkSizeMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.getMaxChunkSize().toBytes())
            .isEqualTo(HttpDecoderSpec.DEFAULT_MAX_CHUNK_SIZE);
  }

  @Test
  void nettyMaxInitialLineLengthMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.getMaxInitialLineLength().toBytes())
            .isEqualTo(HttpDecoderSpec.DEFAULT_MAX_INITIAL_LINE_LENGTH);
  }

  @Test
  void nettyValidateHeadersMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.isValidateHeaders()).isEqualTo(HttpDecoderSpec.DEFAULT_VALIDATE_HEADERS);
  }

  @Test
  void nettyH2cMaxContentLengthMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.getH2cMaxContentLength().toBytes())
            .isEqualTo(HttpRequestDecoderSpec.DEFAULT_H2C_MAX_CONTENT_LENGTH);
  }

  @Test
  void nettyInitialBufferSizeMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.reactorNetty.getInitialBufferSize().toBytes())
            .isEqualTo(HttpDecoderSpec.DEFAULT_INITIAL_BUFFER_SIZE);
  }

  @Test
  void nettyWorkThreadCount() {
    assertThat(this.properties.netty.getWorkerThreads()).isNull();

    bind("server.netty.workerThreads", "10");
    assertThat(this.properties.netty.getWorkerThreads()).isEqualTo(10);

    bind("server.netty.worker-threads", "100");
    assertThat(this.properties.netty.getWorkerThreads()).isEqualTo(100);
  }

  @Test
  void nettyBossThreadCount() {
    assertThat(this.properties.netty.getAcceptorThreads()).isNull();
    bind("server.netty.acceptorThreads", "10");
    assertThat(this.properties.netty.getAcceptorThreads()).isEqualTo(10);

    bind("server.netty.acceptor-threads", "100");
    assertThat(this.properties.netty.getAcceptorThreads()).isEqualTo(100);
  }

  @Test
  void nettyLoggingLevel() {
    assertThat(this.properties.netty.getLoggingLevel()).isNull();

    bind("server.netty.loggingLevel", "INFO");
    assertThat(this.properties.netty.getLoggingLevel()).isEqualTo(LogLevel.INFO);

    bind("server.netty.logging-level", "DEBUG");
    assertThat(this.properties.netty.getLoggingLevel()).isEqualTo(LogLevel.DEBUG);
  }

  @Test
  void nettySocketChannel() {
    assertThat(this.properties.netty.getSocketChannel()).isNull();

    bind("server.netty.socketChannel", "io.netty.channel.socket.nio.NioServerSocketChannel");
    assertThat(this.properties.netty.getSocketChannel()).isEqualTo(NioServerSocketChannel.class);

    bind("server.netty.socket-channel", "io.netty.channel.socket.nio.NioServerSocketChannel");
    assertThat(this.properties.netty.getSocketChannel()).isEqualTo(NioServerSocketChannel.class);
  }

  @Test
  void maxConnection() {
    bind("server.netty.maxConnection", "100");
    assertThat(this.properties.netty.getMaxConnection()).isEqualTo(100);

    bind("server.netty.max-connection", "1000");
    assertThat(this.properties.netty.getMaxConnection()).isEqualTo(1000);
  }

  private Connector getDefaultConnector() {
    return new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
  }

  private AbstractProtocol<?> getDefaultProtocol() throws Exception {
    return (AbstractProtocol<?>) Class.forName(TomcatServletWebServerFactory.DEFAULT_PROTOCOL)
            .getDeclaredConstructor().newInstance();
  }

  private void bind(String name, String value) {
    bind(Collections.singletonMap(name, value));
  }

  private void bind(Map<String, String> map) {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
    new Binder(source).bind("server", Bindable.ofInstance(this.properties));
  }

}
