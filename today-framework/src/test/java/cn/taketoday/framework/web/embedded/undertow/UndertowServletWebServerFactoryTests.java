/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.web.embedded.undertow;

import org.apache.hc.core5.http.HttpResponse;
import org.apache.jasper.servlet.JspServlet;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import cn.taketoday.framework.web.server.ErrorPage;
import cn.taketoday.framework.web.server.GracefulShutdownResult;
import cn.taketoday.framework.web.server.PortInUseException;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.servlet.ServletRegistrationBean;
import cn.taketoday.framework.web.servlet.server.AbstractServletWebServerFactory;
import cn.taketoday.framework.web.servlet.server.AbstractServletWebServerFactoryTests;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.test.web.servlet.ExampleServlet;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletContainer;
import jakarta.servlet.ServletRegistration.Dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UndertowServletWebServerFactory}.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 */
class UndertowServletWebServerFactoryTests extends AbstractServletWebServerFactoryTests {

  @Override
  protected UndertowServletWebServerFactory getFactory() {
    return new UndertowServletWebServerFactory(0);
  }

  @AfterEach
  void awaitClosureOfSslRelatedInputStreams() {
    // https://issues.redhat.com/browse/UNDERTOW-1705
    File resource = new File(this.tempDir, "test.txt");
    Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> (!resource.isFile()) || resource.delete());
  }

  @Test
  void errorPage404() throws Exception {
    AbstractServletWebServerFactory factory = getFactory();
    factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/hello"));
    this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(), "/hello"));
    this.webServer.start();
    assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
    assertThat(getResponse(getLocalUrl("/not-found"))).isEqualTo("Hello World");
  }

  @Test
  void setNullBuilderCustomizersThrows() {
    UndertowServletWebServerFactory factory = getFactory();
    assertThatIllegalArgumentException().isThrownBy(() -> factory.setBuilderCustomizers(null))
            .withMessageContaining("Customizers must not be null");
  }

  @Test
  void addNullAddBuilderCustomizersThrows() {
    UndertowServletWebServerFactory factory = getFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.addBuilderCustomizers((UndertowBuilderCustomizer[]) null))
            .withMessageContaining("Customizers must not be null");
  }

  @Test
  void builderCustomizers() {
    UndertowServletWebServerFactory factory = getFactory();
    UndertowBuilderCustomizer[] customizers = new UndertowBuilderCustomizer[4];
    Arrays.setAll(customizers, (i) -> mock(UndertowBuilderCustomizer.class));
    factory.setBuilderCustomizers(Arrays.asList(customizers[0], customizers[1]));
    factory.addBuilderCustomizers(customizers[2], customizers[3]);
    this.webServer = factory.getWebServer();
    InOrder ordered = inOrder((Object[]) customizers);
    for (UndertowBuilderCustomizer customizer : customizers) {
      ordered.verify(customizer).customize(any(Builder.class));
    }
  }

  @Test
  void setNullDeploymentInfoCustomizersThrows() {
    UndertowServletWebServerFactory factory = getFactory();
    assertThatIllegalArgumentException().isThrownBy(() -> factory.setDeploymentInfoCustomizers(null))
            .withMessageContaining("Customizers must not be null");
  }

  @Test
  void addNullAddDeploymentInfoCustomizersThrows() {
    UndertowServletWebServerFactory factory = getFactory();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.addDeploymentInfoCustomizers((UndertowDeploymentInfoCustomizer[]) null))
            .withMessageContaining("Customizers must not be null");
  }

  @Test
  void deploymentInfo() {
    UndertowServletWebServerFactory factory = getFactory();
    UndertowDeploymentInfoCustomizer[] customizers = new UndertowDeploymentInfoCustomizer[4];
    Arrays.setAll(customizers, (i) -> mock(UndertowDeploymentInfoCustomizer.class));
    factory.setDeploymentInfoCustomizers(Arrays.asList(customizers[0], customizers[1]));
    factory.addDeploymentInfoCustomizers(customizers[2], customizers[3]);
    this.webServer = factory.getWebServer();
    InOrder ordered = inOrder((Object[]) customizers);
    for (UndertowDeploymentInfoCustomizer customizer : customizers) {
      ordered.verify(customizer).customize(any(DeploymentInfo.class));
    }
  }

  @Test
  void basicSslClasspathKeyStore() throws Exception {
    testBasicSslWithKeyStore("classpath:test.jks");
  }

  @Test
  void defaultContextPath() {
    UndertowServletWebServerFactory factory = getFactory();
    final AtomicReference<String> contextPath = new AtomicReference<>();
    factory.addDeploymentInfoCustomizers((deploymentInfo) -> contextPath.set(deploymentInfo.getContextPath()));
    this.webServer = factory.getWebServer();
    assertThat(contextPath.get()).isEqualTo("/");
  }

  @Test
  void useForwardHeaders() throws Exception {
    UndertowServletWebServerFactory factory = getFactory();
    factory.setUseForwardHeaders(true);
    assertForwardHeaderIsUsed(factory);
  }

  @Test
  void eachFactoryUsesADiscreteServletContainer() {
    assertThat(getServletContainerFromNewFactory()).isNotEqualTo(getServletContainerFromNewFactory());
  }

  @Test
  void accessLogCanBeEnabled() throws IOException, URISyntaxException {
    testAccessLog(null, null, "access_log.log");
  }

  @Test
  void accessLogCanBeCustomized() throws IOException, URISyntaxException {
    testAccessLog("my_access.", "logz", "my_access.logz");
  }

  @Test
  void whenServerIsShuttingDownGracefullyThenRequestsAreRejectedWithServiceUnavailable() throws Exception {
    AbstractServletWebServerFactory factory = getFactory();
    factory.setShutdown(Shutdown.GRACEFUL);
    BlockingServlet blockingServlet = new BlockingServlet();
    this.webServer = factory.getWebServer((context) -> {
      Dynamic registration = context.addServlet("blockingServlet", blockingServlet);
      registration.addMapping("/blocking");
      registration.setAsyncSupported(true);
    });
    this.webServer.start();
    int port = this.webServer.getPort();
    Future<Object> request = initiateGetRequest(port, "/blocking");
    blockingServlet.awaitQueue();
    AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
    this.webServer.shutDownGracefully(result::set);
    assertThat(result.get()).isNull();
    blockingServlet.admitOne();
    assertThat(request.get()).isInstanceOf(HttpResponse.class);
    Object rejectedResult = initiateGetRequest(port, "/").get();
    assertThat(rejectedResult).isInstanceOf(HttpResponse.class);
    assertThat(((HttpResponse) rejectedResult).getCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    this.webServer.stop();
  }

  private void testAccessLog(String prefix, String suffix, String expectedFile)
          throws IOException, URISyntaxException {
    UndertowServletWebServerFactory factory = getFactory();
    factory.setAccessLogEnabled(true);
    factory.setAccessLogPrefix(prefix);
    factory.setAccessLogSuffix(suffix);
    File accessLogDirectory = this.tempDir;
    factory.setAccessLogDirectory(accessLogDirectory);
    assertThat(accessLogDirectory).isEmptyDirectory();
    this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(), "/hello"));
    this.webServer.start();
    assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
    File accessLog = new File(accessLogDirectory, expectedFile);
    awaitFile(accessLog);
    assertThat(accessLogDirectory.listFiles()).contains(accessLog);
  }

  @Override
  protected void addConnector(int port, AbstractServletWebServerFactory factory) {
    ((UndertowServletWebServerFactory) factory)
            .addBuilderCustomizers((builder) -> builder.addHttpListener(port, "0.0.0.0"));
  }

  @Test
  void sslRestrictedProtocolsEmptyCipherFailure() {
    assertThatIOException()
            .isThrownBy(() -> testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.2" },
                    new String[] { "TLS_EMPTY_RENEGOTIATION_INFO_SCSV" }))
            .isInstanceOfAny(SSLException.class, SSLHandshakeException.class, SocketException.class);
  }

  @Test
  void sslRestrictedProtocolsECDHETLS1Failure() {
    assertThatIOException()
            .isThrownBy(() -> testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1" },
                    new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" }))
            .isInstanceOfAny(SSLException.class, SocketException.class);
  }

  @Test
  void sslRestrictedProtocolsECDHESuccess() throws Exception {
    testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.2" },
            new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" });
  }

  @Test
  void sslRestrictedProtocolsRSATLS12Success() throws Exception {
    testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.2" },
            new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA256" });
  }

  @Test
  void sslRestrictedProtocolsRSATLS11Failure() {
    assertThatIOException()
            .isThrownBy(() -> testRestrictedSSLProtocolsAndCipherSuites(new String[] { "TLSv1.1" },
                    new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA256" }))
            .isInstanceOfAny(SSLException.class, SocketException.class);
  }

  @Override
  protected JspServlet getJspServlet() {
    return null; // Undertow does not support JSPs
  }

  private void awaitFile(File file) {
    Awaitility.waitAtMost(Duration.ofSeconds(10)).until(file::exists, is(true));
  }

  private ServletContainer getServletContainerFromNewFactory() {
    UndertowServletWebServer container = (UndertowServletWebServer) getFactory().getWebServer();
    try {
      return container.getDeploymentManager().getDeployment().getServletContainer();
    }
    finally {
      container.stop();
    }
  }

  @Override
  protected Map<String, String> getActualMimeMappings() {
    return ((UndertowServletWebServer) this.webServer).getDeploymentManager()
            .getDeployment()
            .getMimeExtensionMappings();
  }

  @Override
  protected Charset getCharset(Locale locale) {
    DeploymentInfo info = ((UndertowServletWebServer) this.webServer).getDeploymentManager().getDeployment()
            .getDeploymentInfo();
    String charsetName = info.getLocaleCharsetMapping().get(locale.toString());
    return (charsetName != null) ? Charset.forName(charsetName) : null;
  }

  @Override
  protected void handleExceptionCausedByBlockedPortOnPrimaryConnector(RuntimeException ex, int blockedPort) {
    assertThat(ex).isInstanceOf(PortInUseException.class);
    assertThat(((PortInUseException) ex).getPort()).isEqualTo(blockedPort);
    Undertow undertow = (Undertow) ReflectionTestUtils.getField(this.webServer, "undertow");
    assertThat(undertow.getWorker()).isNull();
  }

  @Override
  protected void handleExceptionCausedByBlockedPortOnSecondaryConnector(RuntimeException ex, int blockedPort) {
    handleExceptionCausedByBlockedPortOnPrimaryConnector(ex, blockedPort);
  }

}
