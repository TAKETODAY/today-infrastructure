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

import org.apache.catalina.Context;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http2.Http2Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatWebServer;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.ServerProperties.ForwardHeadersStrategy;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.test.context.support.TestPropertySourceUtils;
import cn.taketoday.test.web.servlet.DirtiesUrlFactories;
import cn.taketoday.util.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatWebServerFactoryCustomizer}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author Rob Tompkins
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 * @author Andrew McGhie
 * @author Rafiullah Hamedy
 * @author Victor Mandujano
 * @author Parviz Rozikov
 */
@DirtiesUrlFactories
@Execution(ExecutionMode.SAME_THREAD)
class TomcatWebServerFactoryCustomizerTests {

  private MockEnvironment environment;

  private ServerProperties serverProperties;

  private TomcatWebServerFactoryCustomizer customizer;

  @BeforeEach
  void setup() {
    this.environment = new MockEnvironment();
    this.serverProperties = new ServerProperties();
    ConfigurationPropertySources.attach(this.environment);
    this.customizer = new TomcatWebServerFactoryCustomizer(this.environment, this.serverProperties);
  }

  @Test
  void defaultsAreConsistent() {
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxSwallowSize())
            .isEqualTo(this.serverProperties.tomcat.maxSwallowSize.toBytes()));
  }

  @Test
  void customAcceptCount() {
    bind("server.tomcat.accept-count=10");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getAcceptCount())
            .isEqualTo(10));
  }

  @Test
  void customProcessorCache() {
    bind("server.tomcat.processor-cache=100");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getProcessorCache())
            .isEqualTo(100));
  }

  @Test
  void customKeepAliveTimeout() {
    bind("server.tomcat.keep-alive-timeout=30ms");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getKeepAliveTimeout())
            .isEqualTo(30));
  }

  @Test
  void defaultKeepAliveTimeoutWithHttp2() {
    bind("server.http2.enabled=true");
    customizeAndRunServer((server) -> assertThat(
            ((Http2Protocol) server.getTomcat().getConnector().findUpgradeProtocols()[0]).getKeepAliveTimeout())
            .isEqualTo(20000L));
  }

  @Test
  void customKeepAliveTimeoutWithHttp2() {
    bind("server.tomcat.keep-alive-timeout=30s", "server.http2.enabled=true");
    customizeAndRunServer((server) -> assertThat(
            ((Http2Protocol) server.getTomcat().getConnector().findUpgradeProtocols()[0]).getKeepAliveTimeout())
            .isEqualTo(30000L));
  }

  @Test
  void customMaxKeepAliveRequests() {
    bind("server.tomcat.max-keep-alive-requests=-1");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxKeepAliveRequests()).isEqualTo(-1));
  }

  @Test
  void defaultMaxKeepAliveRequests() {
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxKeepAliveRequests()).isEqualTo(100));
  }

  @Test
  void unlimitedProcessorCache() {
    bind("server.tomcat.processor-cache=-1");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getProcessorCache())
            .isEqualTo(-1));
  }

  @Test
  void customBackgroundProcessorDelay() {
    bind("server.tomcat.background-processor-delay=5");
    TomcatWebServer server = customizeAndGetServer();
    assertThat(server.getTomcat().getEngine().getBackgroundProcessorDelay()).isEqualTo(5);
  }

  @Test
  void customDisableMaxHttpFormPostSize() {
    bind("server.tomcat.max-http-form-post-size=-1");
    customizeAndRunServer((server) -> assertThat(server.getTomcat().getConnector().getMaxPostSize()).isEqualTo(-1));
  }

  @Test
  void customMaxConnections() {
    bind("server.tomcat.max-connections=5");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getMaxConnections())
            .isEqualTo(5));
  }

  @Test
  void customMaxHttpFormPostSize() {
    bind("server.tomcat.max-http-form-post-size=10000");
    customizeAndRunServer(
            (server) -> assertThat(server.getTomcat().getConnector().getMaxPostSize()).isEqualTo(10000));
  }

  @Test
  void customMaxHttpHeaderSize() {
    bind("server.max-http-request-header-size=1KB");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(1).toBytes()));
  }

  @Test
  void customMaxHttpHeaderSizeWithHttp2() {
    bind("server.max-http-request-header-size=1KB", "server.http2.enabled=true");
    customizeAndRunServer((server) -> {
      AbstractHttp11Protocol<?> protocolHandler = (AbstractHttp11Protocol<?>) server.getTomcat().getConnector()
              .getProtocolHandler();
      long expectedSize = DataSize.ofKilobytes(1).toBytes();
      assertThat(protocolHandler.getMaxHttpRequestHeaderSize()).isEqualTo(expectedSize);
      assertThat(((Http2Protocol) protocolHandler.getUpgradeProtocol("h2c")).getMaxHeaderSize())
              .isEqualTo(expectedSize);
    });
  }

  @Test
  void customMaxHttpHeaderSizeIgnoredIfNegative() {
    bind("server.max-http-request-header-size=-1");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
  }

  @Test
  void customMaxHttpHeaderSizeIgnoredIfZero() {
    bind("server.max-http-request-header-size=0");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
  }

  @Test
  void defaultMaxHttpRequestHeaderSize() {
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
  }

  @Test
  void customMaxHttpRequestHeaderSize() {
    bind("server.max-http-request-header-size=10MB");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofMegabytes(10).toBytes()));
  }

  @Test
  void customMaxRequestHttpHeaderSizeIgnoredIfNegative() {
    bind("server.max-http-request-header-size=-1");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
  }

  @Test
  void customMaxRequestHttpHeaderSizeIgnoredIfZero() {
    bind("server.max-http-request-header-size=0");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpRequestHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
  }

  @Test
  void defaultMaxHttpResponseHeaderSize() {
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpResponseHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
  }

  @Test
  void customMaxHttpResponseHeaderSize() {
    bind("server.tomcat.max-http-response-header-size=10MB");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpResponseHeaderSize()).isEqualTo(DataSize.ofMegabytes(10).toBytes()));
  }

  @Test
  void customMaxResponseHttpHeaderSizeIgnoredIfNegative() {
    bind("server.tomcat.max-http-response-header-size=-1");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpResponseHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
  }

  @Test
  void customMaxResponseHttpHeaderSizeIgnoredIfZero() {
    bind("server.tomcat.max-http-response-header-size=0");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxHttpResponseHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
  }

  @Test
  void customMaxSwallowSize() {
    bind("server.tomcat.max-swallow-size=10MB");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getMaxSwallowSize()).isEqualTo(DataSize.ofMegabytes(10).toBytes()));
  }

  @Test
  void customRemoteIpValve() {
    bind("server.tomcat.remoteip.remote-ip-header=x-my-remote-ip-header",
            "server.tomcat.remoteip.protocol-header=x-my-protocol-header",
            "server.tomcat.remoteip.internal-proxies=192.168.0.1",
            "server.tomcat.remoteip.host-header=x-my-forward-host",
            "server.tomcat.remoteip.port-header=x-my-forward-port",
            "server.tomcat.remoteip.protocol-header-https-value=On",
            "server.tomcat.remoteip.trusted-proxies=proxy1|proxy2");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(factory.getEngineValves()).hasSize(1);
    Valve valve = factory.getEngineValves().iterator().next();
    assertThat(valve).isInstanceOf(RemoteIpValve.class);
    RemoteIpValve remoteIpValve = (RemoteIpValve) valve;
    assertThat(remoteIpValve.getProtocolHeader()).isEqualTo("x-my-protocol-header");
    assertThat(remoteIpValve.getProtocolHeaderHttpsValue()).isEqualTo("On");
    assertThat(remoteIpValve.getRemoteIpHeader()).isEqualTo("x-my-remote-ip-header");
    assertThat(remoteIpValve.getHostHeader()).isEqualTo("x-my-forward-host");
    assertThat(remoteIpValve.getPortHeader()).isEqualTo("x-my-forward-port");
    assertThat(remoteIpValve.getInternalProxies()).isEqualTo("192.168.0.1");
    assertThat(remoteIpValve.getTrustedProxies()).isEqualTo("proxy1|proxy2");
  }

  @Test
  void customStaticResourceAllowCaching() {
    bind("server.tomcat.resource.allow-caching=false");
    customizeAndRunServer((server) -> {
      Tomcat tomcat = server.getTomcat();
      Context context = (Context) tomcat.getHost().findChildren()[0];
      assertThat(context.getResources().isCachingAllowed()).isFalse();
    });
  }

  @Test
  void customStaticResourceCacheTtl() {
    bind("server.tomcat.resource.cache-ttl=10000");
    customizeAndRunServer((server) -> {
      Tomcat tomcat = server.getTomcat();
      Context context = (Context) tomcat.getHost().findChildren()[0];
      assertThat(context.getResources().getCacheTtl()).isEqualTo(10000L);
    });
  }

  @Test
  void customRelaxedPathChars() {
    bind("server.tomcat.relaxed-path-chars=|,^");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getRelaxedPathChars()).isEqualTo("|^"));
  }

  @Test
  void customRelaxedQueryChars() {
    bind("server.tomcat.relaxed-query-chars=^  ,  | ");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getRelaxedQueryChars()).isEqualTo("^|"));
  }

  @Test
  void deduceUseForwardHeaders() {
    this.environment.setProperty("DYNO", "-");
    testRemoteIpValveConfigured();
  }

  @Test
  void defaultUseForwardHeaders() {
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(factory.getEngineValves()).hasSize(0);
  }

  @Test
  void forwardHeadersWhenStrategyIsNativeShouldConfigureValve() {
    this.serverProperties.forwardHeadersStrategy = (ForwardHeadersStrategy.NATIVE);
    testRemoteIpValveConfigured();
  }

  @Test
  void forwardHeadersWhenStrategyIsNoneShouldNotConfigureValve() {
    this.environment.setProperty("DYNO", "-");
    this.serverProperties.forwardHeadersStrategy = (ForwardHeadersStrategy.NONE);
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(factory.getEngineValves()).hasSize(0);
  }

  @Test
  void defaultRemoteIpValve() {
    // Since 1.1.7 you need to specify at least the protocol
    bind("server.tomcat.remoteip.protocol-header=X-Forwarded-Proto",
            "server.tomcat.remoteip.remote-ip-header=X-Forwarded-For");
    testRemoteIpValveConfigured();
  }

  @Test
  void setUseNativeForwardHeadersStrategy() {
    this.serverProperties.forwardHeadersStrategy = (ForwardHeadersStrategy.NATIVE);
    testRemoteIpValveConfigured();
  }

  private void testRemoteIpValveConfigured() {
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(factory.getEngineValves()).hasSize(1);
    Valve valve = factory.getEngineValves().iterator().next();
    assertThat(valve).isInstanceOf(RemoteIpValve.class);
    RemoteIpValve remoteIpValve = (RemoteIpValve) valve;
    assertThat(remoteIpValve.getProtocolHeader()).isEqualTo("X-Forwarded-Proto");
    assertThat(remoteIpValve.getProtocolHeaderHttpsValue()).isEqualTo("https");
    assertThat(remoteIpValve.getRemoteIpHeader()).isEqualTo("X-Forwarded-For");
    assertThat(remoteIpValve.getHostHeader()).isEqualTo("X-Forwarded-Host");
    assertThat(remoteIpValve.getPortHeader()).isEqualTo("X-Forwarded-Port");
    String expectedInternalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
            + "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
            + "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
            + "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 127/8
            + "100\\.6[4-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
            + "100\\.[7-9]{1}\\d{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
            + "100\\.1[0-1]{1}\\d{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
            + "100\\.12[0-7]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
            + "172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
            + "172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
            + "172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
            + "0:0:0:0:0:0:0:1|::1";
    assertThat(remoteIpValve.getInternalProxies()).isEqualTo(expectedInternalProxies);
  }

  @Test
  void defaultBackgroundProcessorDelay() {
    TomcatWebServer server = customizeAndGetServer();
    assertThat(server.getTomcat().getEngine().getBackgroundProcessorDelay()).isEqualTo(10);
  }

  @Test
  void disableRemoteIpValve() {
    bind("server.tomcat.remoteip.remote-ip-header=", "server.tomcat.remoteip.protocol-header=");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(factory.getEngineValves()).isEmpty();
  }

  @Test
  void testCustomizeRejectIllegalHeader() {
    bind("server.tomcat.reject-illegal-header=false");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
                    .getRejectIllegalHeader()).isFalse());
  }

  @Test
  void errorReportValveIsConfiguredToNotReportStackTraces() {
    TomcatWebServer server = customizeAndGetServer();
    Valve[] valves = server.getTomcat().getHost().getPipeline().getValves();
    assertThat(valves).hasAtLeastOneElementOfType(ErrorReportValve.class);
    for (Valve valve : valves) {
      if (valve instanceof ErrorReportValve errorReportValve) {
        assertThat(errorReportValve.isShowReport()).isFalse();
        assertThat(errorReportValve.isShowServerInfo()).isFalse();
      }
    }
  }

  @Test
  void testCustomizeMinSpareThreads() {
    bind("server.tomcat.threads.min-spare=10");
    assertThat(this.serverProperties.tomcat.threads.minSpare).isEqualTo(10);
  }

  @Test
  void customConnectionTimeout() {
    bind("server.tomcat.connection-timeout=30s");
    customizeAndRunServer((server) -> assertThat(
            ((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getConnectionTimeout())
            .isEqualTo(30000));
  }

  @Test
  void accessLogBufferingCanBeDisabled() {
    bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.buffered=false");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).isBuffered()).isFalse();
  }

  @Test
  void accessLogCanBeEnabled() {
    bind("server.tomcat.accesslog.enabled=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(factory.getEngineValves()).hasSize(1);
    assertThat(factory.getEngineValves()).first().isInstanceOf(AccessLogValve.class);
  }

  @Test
  void accessLogFileDateFormatByDefault() {
    bind("server.tomcat.accesslog.enabled=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getFileDateFormat())
            .isEqualTo(".yyyy-MM-dd");
  }

  @Test
  void accessLogFileDateFormatCanBeRedefined() {
    bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.file-date-format=yyyy-MM-dd.HH");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getFileDateFormat())
            .isEqualTo("yyyy-MM-dd.HH");
  }

  @Test
  void accessLogIsBufferedByDefault() {
    bind("server.tomcat.accesslog.enabled=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).isBuffered()).isTrue();
  }

  @Test
  void accessLogIsDisabledByDefault() {
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(factory.getEngineValves()).isEmpty();
  }

  @Test
  void accessLogMaxDaysDefault() {
    bind("server.tomcat.accesslog.enabled=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getMaxDays())
            .isEqualTo(this.serverProperties.tomcat.accesslog.maxDays);
  }

  @Test
  void accessLogConditionCanBeSpecified() {
    bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.conditionIf=foo",
            "server.tomcat.accesslog.conditionUnless=bar");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getConditionIf()).isEqualTo("foo");
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getConditionUnless())
            .isEqualTo("bar");
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getCondition())
            .describedAs("value of condition should equal conditionUnless - provided for backwards compatibility")
            .isEqualTo("bar");
  }

  @Test
  void accessLogEncodingIsNullWhenNotSpecified() {
    bind("server.tomcat.accesslog.enabled=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getEncoding()).isNull();
  }

  @Test
  void accessLogEncodingCanBeSpecified() {
    bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.encoding=UTF-8");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getEncoding()).isEqualTo("UTF-8");
  }

  @Test
  void accessLogWithDefaultLocale() {
    bind("server.tomcat.accesslog.enabled=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getLocale())
            .isEqualTo(Locale.getDefault().toString());
  }

  @Test
  void accessLogLocaleCanBeSpecified() {
    String locale = "en_AU".equals(Locale.getDefault().toString()) ? "en_US" : "en_AU";
    bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.locale=" + locale);
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getLocale()).isEqualTo(locale);
  }

  @Test
  void accessLogCheckExistsDefault() {
    bind("server.tomcat.accesslog.enabled=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).isCheckExists()).isFalse();
  }

  @Test
  void accessLogCheckExistsSpecified() {
    bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.check-exists=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).isCheckExists()).isTrue();
  }

  @Test
  void accessLogMaxDaysCanBeRedefined() {
    bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.max-days=20");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getMaxDays()).isEqualTo(20);
  }

  @Test
  void accessLogDoesNotUseIpv6CanonicalFormatByDefault() {
    bind("server.tomcat.accesslog.enabled=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getIpv6Canonical()).isFalse();
  }

  @Test
  void accessLogWithIpv6CanonicalSet() {
    bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.ipv6-canonical=true");
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getIpv6Canonical()).isTrue();
  }

  @Test
  void ajpConnectorCanBeCustomized() {
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
    factory.setProtocol("AJP/1.3");
    factory.addConnectorCustomizers(
            (connector) -> ((AbstractAjpProtocol<?>) connector.getProtocolHandler()).setSecretRequired(false));
    this.customizer.customize(factory);
    WebServer server = factory.getWebServer();
    server.start();
    server.stop();
  }

  @Test
  void configureExecutor() {
    bind("server.tomcat.threads.max=10", "server.tomcat.threads.min-spare=2",
            "server.tomcat.threads.max-queue-capacity=20");
    customizeAndRunServer((server) -> {
      Executor executor = server.getTomcat().getConnector().getProtocolHandler().getExecutor();
      assertThat(executor).isInstanceOf(StandardThreadExecutor.class);
      StandardThreadExecutor standardThreadExecutor = (StandardThreadExecutor) executor;
      assertThat(standardThreadExecutor.getMaxThreads()).isEqualTo(10);
      assertThat(standardThreadExecutor.getMinSpareThreads()).isEqualTo(2);
      assertThat(standardThreadExecutor.getMaxQueueSize()).isEqualTo(20);
    });
  }

  private void bind(String... inlinedProperties) {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, inlinedProperties);
    new Binder(ConfigurationPropertySources.get(this.environment)).bind("server",
            Bindable.ofInstance(this.serverProperties));
  }

  private void customizeAndRunServer(Consumer<TomcatWebServer> consumer) {
    TomcatWebServer server = customizeAndGetServer();
    server.start();
    try {
      consumer.accept(server);
    }
    finally {
      server.stop();
    }
  }

  private TomcatWebServer customizeAndGetServer() {
    TomcatServletWebServerFactory factory = customizeAndGetFactory();
    return (TomcatWebServer) factory.getWebServer();
  }

  private TomcatServletWebServerFactory customizeAndGetFactory() {
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
    factory.setHttp2(this.serverProperties.http2);
    this.customizer.customize(factory);
    return factory;
  }

}
