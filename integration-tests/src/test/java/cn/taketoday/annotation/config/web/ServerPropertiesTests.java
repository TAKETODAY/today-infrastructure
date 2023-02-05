/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.web;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;
import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.embedded.jetty.JettyWebServer;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.ServerProperties.Tomcat.Accesslog;
import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.util.DataSize;
import cn.taketoday.web.client.ResponseErrorHandler;
import cn.taketoday.web.client.RestTemplate;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.undertow.UndertowOptions;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    ServerProperties.Tomcat tomcat = this.properties.getTomcat();
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
    assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/foo");
  }

  @Test
  void testSlashOfContextPathIsDefaultValue() {
    bind("server.servlet.context-path", "/");
    assertThat(this.properties.getServlet().getContextPath()).isEqualTo("");
  }

  @Test
  void testContextPathWithLeadingWhitespace() {
    bind("server.servlet.context-path", " /assets");
    assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/assets");
  }

  @Test
  void testContextPathWithTrailingWhitespace() {
    bind("server.servlet.context-path", "/assets/copy/ ");
    assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/assets/copy");
  }

  @Test
  void testContextPathWithLeadingAndTrailingWhitespace() {
    bind("server.servlet.context-path", " /assets ");
    assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/assets");
  }

  @Test
  void testContextPathWithLeadingAndTrailingWhitespaceAndContextWithSpace() {
    bind("server.servlet.context-path", "  /assets /copy/    ");
    assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/assets /copy");
  }

  @Test
  void testCustomizeUriEncoding() {
    bind("server.tomcat.uri-encoding", "US-ASCII");
    assertThat(this.properties.getTomcat().getUriEncoding()).isEqualTo(StandardCharsets.US_ASCII);
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
    assertThat(this.properties.getTomcat().getThreads().getMax()).isEqualTo(10);
  }

  @Test
  void testCustomizeTomcatKeepAliveTimeout() {
    bind("server.tomcat.keep-alive-timeout", "30s");
    assertThat(this.properties.getTomcat().getKeepAliveTimeout()).hasSeconds(30);
  }

  @Test
  void testCustomizeTomcatKeepAliveTimeoutWithInfinite() {
    bind("server.tomcat.keep-alive-timeout", "-1");
    assertThat(this.properties.getTomcat().getKeepAliveTimeout()).hasMillis(-1);
  }

  @Test
  void customizeMaxKeepAliveRequests() {
    bind("server.tomcat.max-keep-alive-requests", "200");
    assertThat(this.properties.getTomcat().getMaxKeepAliveRequests()).isEqualTo(200);
  }

  @Test
  void customizeMaxKeepAliveRequestsWithInfinite() {
    bind("server.tomcat.max-keep-alive-requests", "-1");
    assertThat(this.properties.getTomcat().getMaxKeepAliveRequests()).isEqualTo(-1);
  }

  @Test
  void testCustomizeTomcatMinSpareThreads() {
    bind("server.tomcat.threads.min-spare", "10");
    assertThat(this.properties.getTomcat().getThreads().getMinSpare()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettyAcceptors() {
    bind("server.jetty.threads.acceptors", "10");
    assertThat(this.properties.getJetty().getThreads().getAcceptors()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettySelectors() {
    bind("server.jetty.threads.selectors", "10");
    assertThat(this.properties.getJetty().getThreads().getSelectors()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettyMaxThreads() {
    bind("server.jetty.threads.max", "10");
    assertThat(this.properties.getJetty().getThreads().getMax()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettyMinThreads() {
    bind("server.jetty.threads.min", "10");
    assertThat(this.properties.getJetty().getThreads().getMin()).isEqualTo(10);
  }

  @Test
  void testCustomizeJettyIdleTimeout() {
    bind("server.jetty.threads.idle-timeout", "10s");
    assertThat(this.properties.getJetty().getThreads().getIdleTimeout()).isEqualTo(Duration.ofSeconds(10));
  }

  @Test
  void testCustomizeJettyMaxQueueCapacity() {
    bind("server.jetty.threads.max-queue-capacity", "5150");
    assertThat(this.properties.getJetty().getThreads().getMaxQueueCapacity()).isEqualTo(5150);
  }

  @Test
  void testCustomizeUndertowServerOption() {
    bind("server.undertow.options.server.ALWAYS_SET_KEEP_ALIVE", "true");
    assertThat(this.properties.getUndertow().getOptions().getServer()).containsEntry("ALWAYS_SET_KEEP_ALIVE",
            "true");
  }

  @Test
  void testCustomizeUndertowSocketOption() {
    bind("server.undertow.options.socket.ALWAYS_SET_KEEP_ALIVE", "true");
    assertThat(this.properties.getUndertow().getOptions().getSocket()).containsEntry("ALWAYS_SET_KEEP_ALIVE",
            "true");
  }

  @Test
  void testCustomizeUndertowIoThreads() {
    bind("server.undertow.threads.io", "4");
    assertThat(this.properties.getUndertow().getThreads().getIo()).isEqualTo(4);
  }

  @Test
  void testCustomizeUndertowWorkerThreads() {
    bind("server.undertow.threads.worker", "10");
    assertThat(this.properties.getUndertow().getThreads().getWorker()).isEqualTo(10);
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
    ServerProperties.Jetty jetty = this.properties.getJetty();
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
    assertThat(this.properties.getReactorNetty().getIdleTimeout()).isEqualTo(Duration.ofSeconds(10));
  }

  @Test
  void testCustomizeNettyMaxKeepAliveRequests() {
    bind("server.reactor-netty.max-keep-alive-requests", "100");
    assertThat(this.properties.getReactorNetty().getMaxKeepAliveRequests()).isEqualTo(100);
  }

  @Test
  void tomcatAcceptCountMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.getTomcat().getAcceptCount()).isEqualTo(getDefaultProtocol().getAcceptCount());
  }

  @Test
  void tomcatProcessorCacheMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.getTomcat().getProcessorCache()).isEqualTo(getDefaultProtocol().getProcessorCache());
  }

  @Test
  void tomcatMaxConnectionsMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.getTomcat().getMaxConnections()).isEqualTo(getDefaultProtocol().getMaxConnections());
  }

  @Test
  void tomcatMaxThreadsMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.getTomcat().getThreads().getMax()).isEqualTo(getDefaultProtocol().getMaxThreads());
  }

  @Test
  void tomcatMinSpareThreadsMatchesProtocolDefault() throws Exception {
    assertThat(this.properties.getTomcat().getThreads().getMinSpare())
            .isEqualTo(getDefaultProtocol().getMinSpareThreads());
  }

  @Test
  void tomcatMaxHttpPostSizeMatchesConnectorDefault() {
    assertThat(this.properties.getTomcat().getMaxHttpFormPostSize().toBytes())
            .isEqualTo(getDefaultConnector().getMaxPostSize());
  }

  @Test
  void tomcatBackgroundProcessorDelayMatchesEngineDefault() {
    assertThat(this.properties.getTomcat().getBackgroundProcessorDelay())
            .hasSeconds((new StandardEngine().getBackgroundProcessorDelay()));
  }

  @Test
  void tomcatMaxHttpFormPostSizeMatchesConnectorDefault() {
    assertThat(this.properties.getTomcat().getMaxHttpFormPostSize().toBytes())
            .isEqualTo(getDefaultConnector().getMaxPostSize());
  }

  @Test
  void tomcatUriEncodingMatchesConnectorDefault() {
    assertThat(this.properties.getTomcat().getUriEncoding().name())
            .isEqualTo(getDefaultConnector().getURIEncoding());
  }

  @Test
  void tomcatRedirectContextRootMatchesDefault() {
    assertThat(this.properties.getTomcat().getRedirectContextRoot())
            .isEqualTo(new StandardContext().getMapperContextRootRedirectEnabled());
  }

  @Test
  void tomcatAccessLogRenameOnRotateMatchesDefault() {
    assertThat(this.properties.getTomcat().getAccesslog().isRenameOnRotate())
            .isEqualTo(new AccessLogValve().isRenameOnRotate());
  }

  @Test
  void tomcatAccessLogRequestAttributesEnabledMatchesDefault() {
    assertThat(this.properties.getTomcat().getAccesslog().isRequestAttributesEnabled())
            .isEqualTo(new AccessLogValve().getRequestAttributesEnabled());
  }

  @Test
  void tomcatInternalProxiesMatchesDefault() {
    assertThat(this.properties.getTomcat().getRemoteip().getInternalProxies())
            .isEqualTo(new RemoteIpValve().getInternalProxies());
  }

  @Test
  void tomcatRejectIllegalHeaderMatchesProtocolDefault() throws Exception {
    assertThat(getDefaultProtocol()).hasFieldOrPropertyWithValue("rejectIllegalHeader",
            this.properties.getTomcat().isRejectIllegalHeader());
  }

  @Test
  void tomcatUseRelativeRedirectsDefaultsToFalse() {
    assertThat(this.properties.getTomcat().isUseRelativeRedirects()).isFalse();
  }

  @Test
  void tomcatMaxKeepAliveRequestsDefault() throws Exception {
    AbstractEndpoint<?, ?> endpoint = (AbstractEndpoint<?, ?>) ReflectionTestUtils.getField(getDefaultProtocol(),
            "endpoint");
    int defaultMaxKeepAliveRequests = (int) ReflectionTestUtils.getField(endpoint, "maxKeepAliveRequests");
    assertThat(this.properties.getTomcat().getMaxKeepAliveRequests()).isEqualTo(defaultMaxKeepAliveRequests);
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
    assertThat(this.properties.getJetty().getThreads().getIdleTimeout().toMillis()).isEqualTo(idleTimeout);
    assertThat(this.properties.getJetty().getThreads().getMax()).isEqualTo(maxThreads);
    assertThat(this.properties.getJetty().getThreads().getMin()).isEqualTo(minThreads);
  }

  @Test
  void jettyMaxHttpFormPostSizeMatchesDefault() {
    JettyServletWebServerFactory jettyFactory = new JettyServletWebServerFactory(0);
    JettyWebServer jetty = (JettyWebServer) jettyFactory
            .getWebServer((ServletContextInitializer) (servletContext) -> servletContext
                    .addServlet("formPost", new HttpServlet() {

                      @Override
                      protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                              throws ServletException, IOException {
                        req.getParameterMap();
                      }

                    }).addMapping("/form"));
    jetty.start();
    org.eclipse.jetty.server.Connector connector = jetty.getServer().getConnectors()[0];
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    connector.addBean(new HttpChannel.Listener() {

      @Override
      public void onDispatchFailure(Request request, Throwable ex) {
        failure.set(ex);
      }

    });
    try {
      RestTemplate template = new RestTemplate();
      template.setErrorHandler(new ResponseErrorHandler() {

        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
          return false;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {

        }

      });
      HttpHeaders headers = HttpHeaders.create();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("data", "a".repeat(250000));
      HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
      template.postForEntity(URI.create("http://localhost:" + jetty.getPort() + "/form"), entity, Void.class);
      assertThat(failure.get()).isNotNull();
      String message = failure.get().getCause().getMessage();
      int defaultMaxPostSize = Integer.parseInt(message.substring(message.lastIndexOf(' ')).trim());
      assertThat(this.properties.getJetty().getMaxHttpFormPostSize().toBytes()).isEqualTo(defaultMaxPostSize);
    }
    finally {
      jetty.stop();
    }
  }

  @Test
  void undertowMaxHttpPostSizeMatchesDefault() {
    assertThat(this.properties.getUndertow().getMaxHttpPostSize().toBytes())
            .isEqualTo(UndertowOptions.DEFAULT_MAX_ENTITY_SIZE);
  }

  @Test
  void nettyMaxChunkSizeMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.getReactorNetty().getMaxChunkSize().toBytes())
            .isEqualTo(HttpDecoderSpec.DEFAULT_MAX_CHUNK_SIZE);
  }

  @Test
  void nettyMaxInitialLineLengthMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.getReactorNetty().getMaxInitialLineLength().toBytes())
            .isEqualTo(HttpDecoderSpec.DEFAULT_MAX_INITIAL_LINE_LENGTH);
  }

  @Test
  void nettyValidateHeadersMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.getReactorNetty().isValidateHeaders()).isEqualTo(HttpDecoderSpec.DEFAULT_VALIDATE_HEADERS);
  }

  @Test
  void nettyH2cMaxContentLengthMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.getReactorNetty().getH2cMaxContentLength().toBytes())
            .isEqualTo(HttpRequestDecoderSpec.DEFAULT_H2C_MAX_CONTENT_LENGTH);
  }

  @Test
  void nettyInitialBufferSizeMatchesHttpDecoderSpecDefault() {
    assertThat(this.properties.getReactorNetty().getInitialBufferSize().toBytes())
            .isEqualTo(HttpDecoderSpec.DEFAULT_INITIAL_BUFFER_SIZE);
  }

  @Test
  void nettyWorkThreadCount() {
    assertThat(this.properties.getNetty().getWorkThreadCount()).isNull();

    bind("server.netty.workThreadCount", "10");
    assertThat(this.properties.getNetty().getWorkThreadCount()).isEqualTo(10);

    bind("server.netty.work-thread-count", "100");
    assertThat(this.properties.getNetty().getWorkThreadCount()).isEqualTo(100);
  }

  @Test
  void nettyBossThreadCount() {
    assertThat(this.properties.getNetty().getBossThreadCount()).isNull();
    bind("server.netty.bossThreadCount", "10");
    assertThat(this.properties.getNetty().getBossThreadCount()).isEqualTo(10);

    bind("server.netty.boss-thread-count", "100");
    assertThat(this.properties.getNetty().getBossThreadCount()).isEqualTo(100);
  }

  @Test
  void nettyLoggingLevel() {
    assertThat(this.properties.getNetty().getLoggingLevel()).isNull();

    bind("server.netty.loggingLevel", "INFO");
    assertThat(this.properties.getNetty().getLoggingLevel()).isEqualTo(LogLevel.INFO);

    bind("server.netty.logging-level", "DEBUG");
    assertThat(this.properties.getNetty().getLoggingLevel()).isEqualTo(LogLevel.DEBUG);
  }

  @Test
  void nettySocketChannel() {
    assertThat(this.properties.getNetty().getSocketChannel()).isNull();

    bind("server.netty.socketChannel", "io.netty.channel.socket.nio.NioServerSocketChannel");
    assertThat(this.properties.getNetty().getSocketChannel()).isEqualTo(NioServerSocketChannel.class);

    bind("server.netty.socket-channel", "io.netty.channel.socket.nio.NioServerSocketChannel");
    assertThat(this.properties.getNetty().getSocketChannel()).isEqualTo(NioServerSocketChannel.class);
  }

  @Test
  void nettyFastThreadLocal() {
    bind("server.netty.fastThreadLocal", "false");
    assertThat(this.properties.getNetty().isFastThreadLocal()).isEqualTo(false);

    bind("server.netty.fast-thread-local", "true");
    assertThat(this.properties.getNetty().isFastThreadLocal()).isEqualTo(true);
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
