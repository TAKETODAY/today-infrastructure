/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.embedded.jetty;

import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.awaitility.Awaitility;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee10.webapp.ClassMatcher;
import org.eclipse.jetty.ee10.webapp.Configuration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.web.server.Compression;
import cn.taketoday.framework.web.server.GracefulShutdownResult;
import cn.taketoday.framework.web.server.PortInUseException;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.framework.web.servlet.server.AbstractServletWebServerFactory;
import cn.taketoday.framework.web.servlet.server.AbstractServletWebServerFactoryTests;
import cn.taketoday.test.classpath.ClassPathOverrides;
import cn.taketoday.test.web.servlet.Servlet5ClassPathOverrides;
import cn.taketoday.util.ReflectionUtils;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration.Dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Henri Kerola
 */
@Servlet5ClassPathOverrides
@ClassPathOverrides({ "org.eclipse.jetty:jetty-jndi:11.0.16",
        "org.eclipse.jetty:jetty-server:11.0.16" })
class JettyServletWebServerFactoryTests extends AbstractServletWebServerFactoryTests {

  @Override
  protected JettyServletWebServerFactory getFactory() {
    return new JettyServletWebServerFactory(0);
  }

  @Override
  protected void addConnector(int port, AbstractServletWebServerFactory factory) {
    ((JettyServletWebServerFactory) factory).addServerCustomizers((server) -> {
      ServerConnector connector = new ServerConnector(server);
      connector.setPort(port);
      server.addConnector(connector);
    });
  }

  @Override
  protected Map<String, String> getActualMimeMappings() {
    WebAppContext context = findWebAppContext((JettyWebServer) this.webServer);
    return context.getMimeTypes().getMimeMap();
  }

  @Override
  protected Charset getCharset(Locale locale) {
    WebAppContext context = findWebAppContext((JettyWebServer) this.webServer);
    String charsetName = context.getLocaleEncoding(locale);
    return (charsetName != null) ? Charset.forName(charsetName) : null;
  }

  @Override
  protected void handleExceptionCausedByBlockedPortOnPrimaryConnector(RuntimeException ex, int blockedPort) {
    assertThat(ex).isInstanceOf(PortInUseException.class);
    assertThat(((PortInUseException) ex).getPort()).isEqualTo(blockedPort);
  }

  @Override
  protected void handleExceptionCausedByBlockedPortOnSecondaryConnector(RuntimeException ex, int blockedPort) {
    handleExceptionCausedByBlockedPortOnPrimaryConnector(ex, blockedPort);
  }

  @Test
  @Override
  @Disabled("Jetty 11 does not support User-Agent-based compression")
  protected void noCompressionForUserAgent() {

  }

  @Test
  void contextPathIsLoggedOnStartupWhenCompressionIsEnabled(CapturedOutput output) {
    AbstractServletWebServerFactory factory = getFactory();
    factory.setContextPath("/custom");
    Compression compression = new Compression();
    compression.setEnabled(true);
    factory.setCompression(compression);
    this.webServer = factory.getWebServer(exampleServletRegistration());
    this.webServer.start();
    assertThat(output).containsOnlyOnce("with context path '/custom'");
  }

  @Test
  void jettyConfigurations() throws Exception {
    JettyServletWebServerFactory factory = getFactory();
    Configuration[] configurations = new Configuration[] { mockConfiguration(Configuration1.class),
            mockConfiguration(Configuration2.class), mockConfiguration(Configuration3.class),
            mockConfiguration(Configuration4.class) };
    factory.setConfigurations(Arrays.asList(configurations[0], configurations[1]));
    factory.addConfigurations(configurations[2], configurations[3]);
    this.webServer = factory.getWebServer();
    InOrder ordered = inOrder((Object[]) configurations);
    for (Configuration configuration : configurations) {
      ordered.verify(configuration).configure(any(WebAppContext.class));
    }
  }

  Configuration mockConfiguration(Class<? extends Configuration> type) {
    Configuration mock = mock(type);
    ClassMatcher classMatcher = new ClassMatcher();
    given(mock.getSystemClasses()).willReturn(classMatcher);
    given(mock.getServerClasses()).willReturn(classMatcher);
    return mock;
  }

  @Test
  void jettyCustomizations() {
    JettyServletWebServerFactory factory = getFactory();
    JettyServerCustomizer[] configurations = new JettyServerCustomizer[4];
    Arrays.setAll(configurations, (i) -> mock(JettyServerCustomizer.class));
    factory.setServerCustomizers(Arrays.asList(configurations[0], configurations[1]));
    factory.addServerCustomizers(configurations[2], configurations[3]);
    this.webServer = factory.getWebServer();
    InOrder ordered = inOrder((Object[]) configurations);
    for (JettyServerCustomizer configuration : configurations) {
      ordered.verify(configuration).customize(any(Server.class));
    }
  }

  @Test
  void sessionTimeout() {
    JettyServletWebServerFactory factory = getFactory();
    factory.getSession().setTimeout(Duration.ofSeconds(10));
    assertTimeout(factory, 10);
  }

  @Test
  void sessionTimeoutInMinutes() {
    JettyServletWebServerFactory factory = getFactory();
    factory.getSession().setTimeout(Duration.ofMinutes(1));
    assertTimeout(factory, 60);
  }

  @Test
  void sslCiphersConfiguration() {
    Ssl ssl = new Ssl();
    ssl.setKeyStore("src/test/resources/test.jks");
    ssl.setKeyStorePassword("secret");
    ssl.setKeyPassword("password");
    ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });

    JettyServletWebServerFactory factory = getFactory();
    factory.setSsl(ssl);

    this.webServer = factory.getWebServer();
    this.webServer.start();

    JettyWebServer jettyWebServer = (JettyWebServer) this.webServer;
    ServerConnector connector = (ServerConnector) jettyWebServer.getServer().getConnectors()[0];
    SslConnectionFactory connectionFactory = connector.getConnectionFactory(SslConnectionFactory.class);
    SslContextFactory sslContextFactory = extractSslContextFactory(connectionFactory);
    assertThat(sslContextFactory.getIncludeCipherSuites()).containsExactly("ALPHA", "BRAVO", "CHARLIE");
    assertThat(sslContextFactory.getExcludeCipherSuites()).isEmpty();
  }

  @Test
  void destroyCalledWithoutStart() {
    JettyServletWebServerFactory factory = getFactory();
    this.webServer = factory.getWebServer(exampleServletRegistration());
    this.webServer.destroy();
    Server server = ((JettyWebServer) this.webServer).getServer();
    assertThat(server.isStopped()).isTrue();
  }

  @Test
  void sslEnabledMultiProtocolsConfiguration() {
    JettyServletWebServerFactory factory = getFactory();
    factory.setSsl(getSslSettings("TLSv1.1", "TLSv1.2"));
    this.webServer = factory.getWebServer();
    this.webServer.start();
    JettyWebServer jettyWebServer = (JettyWebServer) this.webServer;
    ServerConnector connector = (ServerConnector) jettyWebServer.getServer().getConnectors()[0];
    SslConnectionFactory connectionFactory = connector.getConnectionFactory(SslConnectionFactory.class);
    SslContextFactory sslContextFactory = extractSslContextFactory(connectionFactory);
    assertThat(sslContextFactory.getIncludeProtocols()).containsExactly("TLSv1.1", "TLSv1.2");
  }

  @Test
  void sslEnabledProtocolsConfiguration() {
    JettyServletWebServerFactory factory = getFactory();
    factory.setSsl(getSslSettings("TLSv1.1"));
    this.webServer = factory.getWebServer();
    this.webServer.start();
    JettyWebServer jettyWebServer = (JettyWebServer) this.webServer;
    ServerConnector connector = (ServerConnector) jettyWebServer.getServer().getConnectors()[0];
    SslConnectionFactory connectionFactory = connector.getConnectionFactory(SslConnectionFactory.class);
    SslContextFactory sslContextFactory = extractSslContextFactory(connectionFactory);
    assertThat(sslContextFactory.getIncludeProtocols()).containsExactly("TLSv1.1");
  }

  private SslContextFactory extractSslContextFactory(SslConnectionFactory connectionFactory) {
    try {
      return connectionFactory.getSslContextFactory();
    }
    catch (NoSuchMethodError ex) {
      Method getSslContextFactory = ReflectionUtils.findMethod(connectionFactory.getClass(),
              "getSslContextFactory");
      return (SslContextFactory) ReflectionUtils.invokeMethod(getSslContextFactory, connectionFactory);
    }
  }

  @Test
  void whenServerIsShuttingDownGracefullyThenNewConnectionsCannotBeMade() throws Exception {
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
    this.webServer.shutDownGracefully((result) -> {
    });
    Future<Object> unconnectableRequest = initiateGetRequest(port, "/");
    blockingServlet.admitOne();
    Object response = request.get();
    assertThat(response).isInstanceOf(HttpResponse.class);
    assertThat(unconnectableRequest.get()).isInstanceOf(HttpHostConnectException.class);
    this.webServer.stop();
  }

  @Test
  void whenServerIsShuttingDownGracefullyThenResponseToRequestOnIdleConnectionWillHaveAConnectionCloseHeader()
          throws Exception {
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
    HttpClient client = HttpClients.createMinimal();
    Future<Object> request = initiateGetRequest(client, port, "/blocking");
    blockingServlet.awaitQueue();
    blockingServlet.admitOne();
    Object response = request.get();
    assertThat(response).isInstanceOf(HttpResponse.class);
    assertThat(((HttpResponse) response).getCode()).isEqualTo(200);
    assertThat(((HttpResponse) response).getFirstHeader("Connection")).isNull();
    this.webServer.shutDownGracefully((result) -> {
    });
    request = initiateGetRequest(client, port, "/blocking");
    blockingServlet.awaitQueue();
    blockingServlet.admitOne();
    response = request.get();
    assertThat(response).isInstanceOf(HttpResponse.class);
    assertThat(((HttpResponse) response).getCode()).isEqualTo(200);
    assertThat(((HttpResponse) response).getFirstHeader("Connection")).isNotNull()
            .extracting(Header::getValue)
            .isEqualTo("close");
    this.webServer.stop();
  }

  @Test
  void whenARequestCompletesAfterGracefulShutdownHasBegunThenItHasAConnectionCloseHeader()
          throws InterruptedException, ExecutionException {
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
    blockingServlet.admitOne();
    Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> GracefulShutdownResult.IDLE == result.get());
    Object requestResult = request.get();
    assertThat(requestResult).isInstanceOf(HttpResponse.class);
    assertThat(((HttpResponse) requestResult).getFirstHeader("Connection").getValue()).isEqualTo("close");
  }

  private Ssl getSslSettings(String... enabledProtocols) {
    Ssl ssl = new Ssl();
    ssl.setKeyStore("src/test/resources/test.jks");
    ssl.setKeyStorePassword("secret");
    ssl.setKeyPassword("password");
    ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
    ssl.setEnabledProtocols(enabledProtocols);
    return ssl;
  }

  private void assertTimeout(JettyServletWebServerFactory factory, int expected) {
    this.webServer = factory.getWebServer();
    JettyWebServer jettyWebServer = (JettyWebServer) this.webServer;
    WebAppContext webAppContext = findWebAppContext(jettyWebServer);
    int actual = webAppContext.getSessionHandler().getMaxInactiveInterval();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void wrappedHandlers() throws Exception {
    JettyServletWebServerFactory factory = getFactory();
    factory.setServerCustomizers(Collections.singletonList((server) -> {
      Handler handler = server.getHandler();
      Handler.Wrapper wrapper = new Handler.Wrapper();
      wrapper.setHandler(handler);
      Handler.Collection collection = new ContextHandlerCollection();
      collection.addHandler(wrapper);
      server.setHandler(collection);
    }));
    this.webServer = factory.getWebServer(exampleServletRegistration());
    this.webServer.start();
    assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
  }

  @Test
  void basicSslClasspathKeyStore() throws Exception {
    testBasicSslWithKeyStore("classpath:test.jks");
  }

  @Test
  void useForwardHeaders() throws Exception {
    JettyServletWebServerFactory factory = getFactory();
    factory.setUseForwardHeaders(true);
    assertForwardHeaderIsUsed(factory);
  }

  @Test
  void defaultThreadPool() {
    JettyServletWebServerFactory factory = getFactory();
    factory.setThreadPool(null);
    assertThat(factory.getThreadPool()).isNull();
    this.webServer = factory.getWebServer();
    assertThat(((JettyWebServer) this.webServer).getServer().getThreadPool()).isNotNull();
  }

  @Test
  void customThreadPool() {
    JettyServletWebServerFactory factory = getFactory();
    ThreadPool threadPool = mock(ThreadPool.class);
    factory.setThreadPool(threadPool);
    this.webServer = factory.getWebServer();
    assertThat(((JettyWebServer) this.webServer).getServer().getThreadPool()).isSameAs(threadPool);
  }

  @Test
  void startFailsWhenThreadPoolIsTooSmall() {
    JettyServletWebServerFactory factory = getFactory();
    factory.addServerCustomizers((server) -> {
      QueuedThreadPool threadPool = server.getBean(QueuedThreadPool.class);
      threadPool.setMaxThreads(2);
      threadPool.setMinThreads(2);
    });
    assertThatExceptionOfType(WebServerException.class).isThrownBy(factory.getWebServer()::start)
            .withCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void specificIPAddressNotReverseResolved() throws Exception {
    JettyServletWebServerFactory factory = getFactory();
    InetAddress localhost = InetAddress.getLocalHost();
    factory.setAddress(InetAddress.getByAddress(localhost.getAddress()));
    this.webServer = factory.getWebServer();
    this.webServer.start();
    Connector connector = ((JettyWebServer) this.webServer).getServer().getConnectors()[0];
    assertThat(((ServerConnector) connector).getHost()).isEqualTo(localhost.getHostAddress());
  }

  @Test
  void specificIPAddressWithSslIsNotReverseResolved() throws Exception {
    JettyServletWebServerFactory factory = getFactory();
    InetAddress localhost = InetAddress.getLocalHost();
    factory.setAddress(InetAddress.getByAddress(localhost.getAddress()));
    Ssl ssl = new Ssl();
    ssl.setKeyStore("src/test/resources/test.jks");
    ssl.setKeyStorePassword("secret");
    ssl.setKeyPassword("password");
    factory.setSsl(ssl);
    this.webServer = factory.getWebServer();
    this.webServer.start();
    Connector connector = ((JettyWebServer) this.webServer).getServer().getConnectors()[0];
    assertThat(((ServerConnector) connector).getHost()).isEqualTo(localhost.getHostAddress());
  }

  @Test
  void faultyListenerCausesStartFailure() {
    JettyServletWebServerFactory factory = getFactory();
    factory.addServerCustomizers((JettyServerCustomizer) (server) -> {
      Collection<WebAppContext> contexts = server.getBeans(WebAppContext.class);
      EventListener eventListener = new ServletContextListener() {

        @Override
        public void contextInitialized(ServletContextEvent event) {
          throw new RuntimeException();
        }

        @Override
        public void contextDestroyed(ServletContextEvent event) {
        }
      };
      WebAppContext context = contexts.iterator().next();
      try {
        context.addEventListener(eventListener);
      }
      catch (NoSuchMethodError ex) {
        // Jetty 10
        Method addEventListener = ReflectionUtils.findMethod(context.getClass(), "addEventListener",
                EventListener.class);
        ReflectionUtils.invokeMethod(addEventListener, context, eventListener);
      }
    });
    assertThatExceptionOfType(WebServerException.class).isThrownBy(() -> {
      JettyWebServer jettyWebServer = (JettyWebServer) factory.getWebServer();
      try {
        jettyWebServer.start();
      }
      finally {
        QueuedThreadPool threadPool = (QueuedThreadPool) jettyWebServer.getServer().getThreadPool();
        assertThat(threadPool.isRunning()).isFalse();
      }
    });
  }

  @Test
  void errorHandlerCanBeOverridden() {
    JettyServletWebServerFactory factory = getFactory();
    factory.addConfigurations(new EmptyBuilderConfiguration() {

      @Override
      public void configure(WebAppContext context) throws Exception {
        context.setErrorHandler(new CustomErrorHandler());
      }

    });
    JettyWebServer jettyWebServer = (JettyWebServer) factory.getWebServer();
    WebAppContext context = findWebAppContext(jettyWebServer);
    assertThat(context.getErrorHandler()).isInstanceOf(CustomErrorHandler.class);
  }

  @Test
  void shouldApplyMaxConnections() {
    JettyServletWebServerFactory factory = getFactory();
    factory.setMaxConnections(1);
    this.webServer = factory.getWebServer();
    Server server = ((JettyWebServer) this.webServer).getServer();
    ConnectionLimit connectionLimit = server.getBean(ConnectionLimit.class);
    assertThat(connectionLimit).isNotNull();
    assertThat(connectionLimit.getMaxConnections()).isOne();
  }

  @Override
  protected String startedLogMessage() {
    return ((JettyWebServer) this.webServer).getStartedLogMessage();
  }

  private WebAppContext findWebAppContext(JettyWebServer webServer) {
    return findWebAppContext(webServer.getServer().getHandler());
  }

  private WebAppContext findWebAppContext(Handler handler) {
    if (handler instanceof WebAppContext webAppContext) {
      return webAppContext;
    }
    if (handler instanceof Handler.Wrapper wrapper) {
      return findWebAppContext(wrapper.getHandler());
    }
    throw new IllegalStateException("No WebAppContext found");
  }

  private static class CustomErrorHandler extends ErrorPageErrorHandler {

  }

  interface Configuration1 extends Configuration {

  }

  interface Configuration2 extends Configuration {

  }

  interface Configuration3 extends Configuration {

  }

  interface Configuration4 extends Configuration {

  }

}
