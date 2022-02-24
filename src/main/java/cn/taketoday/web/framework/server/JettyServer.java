/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.framework.server;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.IOException;
import java.net.BindException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.framework.WebServerException;
import cn.taketoday.web.framework.config.CompressionConfiguration;
import cn.taketoday.web.framework.config.ErrorPage;
import cn.taketoday.web.framework.config.MimeMappings;
import cn.taketoday.web.framework.config.WebDocumentConfiguration;
import cn.taketoday.web.servlet.initializer.ServletContextInitializer;
import cn.taketoday.web.session.SessionConfiguration;
import jakarta.servlet.Servlet;
import lombok.Getter;
import lombok.Setter;

/**
 * Jetty Servlet web server.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author David Liu
 * @author Eddú Meléndez
 * @author Brian Clozel
 * @author Kristine Jetzke
 * @author TODAY <br>
 * 2018-10-15 20:44
 */
@Setter
@Getter
public class JettyServer
        extends AbstractServletWebServer implements WebServer {

  private Server server;

  private boolean autoStart = true;

  private Connector[] connectors;

  private List<Configuration> configurations = new ArrayList<>();

  private boolean useForwardHeaders;

  /** The number of acceptor threads to use. default value */
  private int acceptors = -1;

  /** The number of selector threads to use. default value */
  private int selectors = -1;

  private ThreadPool threadPool;

  private boolean sendVersion;

  /**
   * @see WebAppContext#setThrowUnavailableOnStartupException(boolean)
   * @since 1.0.1
   */
  private boolean throwUnavailableOnStartupException = true;

  @Override
  protected synchronized void contextInitialized() {
    super.contextInitialized();

    try {
      // Cache the connectors and then remove them to prevent requests being
      // handled before the application context is ready.
      this.connectors = this.server.getConnectors();
      this.server.addBean(new AbstractLifeCycle() {

        @Override
        protected void doStart() throws Exception {
          for (Connector connector : JettyServer.this.connectors) {
            Assert.state(connector.isStopped(),
                    () -> "Connector " + connector + " has been started prematurely");
          }
          JettyServer.this.server.setConnectors(null);
        }

      });
      // Start the server so that the ServletContext is available
      this.server.start();
      this.server.setStopAtShutdown(false);
      log.info("Jetty initialized on port: '{}' with context path: '{}'", getPort(), getContextPath());
    }
    catch (Throwable ex) {
      // Ensure process isn't left running
      stopSilently();
      throw new WebServerException("Unable to start embedded Jetty web server", ex);
    }
  }

  private void stopSilently() {
    try {
      this.server.stop();
    }
    catch (Exception ignored) { }
  }

  @Override
  public synchronized void start() {
    if (getStarted().get()) {
      return;
    }
    this.server.setConnectors(this.connectors);
    if (!this.autoStart) {
      return;
    }
    try {
      this.server.start();
      Connector[] connectors = this.server.getConnectors();
      for (Connector connector : connectors) {
        try {
          connector.start();
        }
        catch (BindException ex) {
          if (connector instanceof NetworkConnector) {
            log.error("The port: [{}] is already in use", ((NetworkConnector) connector).getPort(), ex);
          }
          throw ex;
        }
      }
      getStarted().set(true);
      log.info("Jetty started on port(s) '{}' with context path '{}'", //
              getActualPortsDescription(), getContextPath());
    }
    catch (WebServerException ex) {
      stopSilently();
      throw ex;
    }
    catch (Exception ex) {
      stopSilently();
      throw new WebServerException("Unable to start embedded Jetty server", ex);
    }
  }

  private String getActualPortsDescription() {
    StringBuilder ports = new StringBuilder();
    for (Connector connector : this.server.getConnectors()) {
      if (ports.length() != 0) {
        ports.append(", ");
      }
      ports.append(getPort()).append(getProtocols(connector));
    }
    return ports.toString();
  }

  private String getProtocols(Connector connector) {
    List<String> protocols = connector.getProtocols();
    return " (" + StringUtils.collectionToCommaDelimitedString(protocols) + ")";
  }

  @Override
  public synchronized void stop() {
    getStarted().set(false);
    log.info("Jetty stopping on port(s) '{}' with context path '{}'", //
            getActualPortsDescription(), getContextPath());
    try {
      this.server.stop();
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    catch (Exception ex) {
      throw new WebServerException("Unable to stop embedded Jetty server", ex);
    }
  }

  public Server getJetty() {
    return this.server;
  }

  @Override
  protected void initializeContext() {
    super.initializeContext();
    log.info("Jetty Server initializing with port: {}", getPort());
    final WebAppContext context = new WebAppContext();
    final Server server = new Server(getThreadPool());
    this.server = server;
    server.setConnectors(new Connector[] { getServerConnector(getHost(), getPort(), server) });

    configureWebAppContext(context);
    server.setHandler(getHandlerWrappers(context));
    if (this.useForwardHeaders) {
      final ForwardedRequestCustomizer customizer = new ForwardedRequestCustomizer();
      for (final Connector connector : server.getConnectors()) {
        for (final ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
          if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {
            ((HttpConfiguration.ConnectionFactory) connectionFactory).getHttpConfiguration()//
                    .addCustomizer(customizer);
          }
        }
      }
    }
  }

  /**
   * Create a sever {@link Connector}
   *
   * @param host server host
   * @param port server port
   * @param server server instance
   * @return a {@link ServerConnector}
   */
  protected ServerConnector getServerConnector(
          final String host, final int port, final Server server) {
    final ServerConnector connector = new ServerConnector(server, this.acceptors, this.selectors);
    connector.setHost(host);
    connector.setPort(port);
    for (final ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
      if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {
        // send version
        ((HttpConfiguration.ConnectionFactory) connectionFactory).getHttpConfiguration()//
                .setSendServerVersion(sendVersion);
      }
    }
    return connector;
  }

  protected Handler getHandlerWrappers(Handler handler) {
    final CompressionConfiguration compression = getCompression();
    if (compression != null) {
      getWebApplicationConfiguration().configureCompression(compression);
      if (compression.isEnable()) {
        log.info("Compression enabled");
        handler = applyHandler(handler, configureCompression(compression));
      }
    }
    // if (StringUtils.isNotEmpty(getServerHeader())) { // TODO server header }
    return handler;
  }

  protected Handler applyHandler(Handler handler, HandlerWrapper wrapper) {
    wrapper.setHandler(handler);
    return wrapper;
  }

  /**
   * Configure the given Jetty {@link WebAppContext} for use.
   *
   * @param context the context to configure
   */
  protected void configureWebAppContext(final WebAppContext context) {
    Assert.notNull(context, "WebAppContext must not be null");
    context.setTempDirectory(getTemporalDirectory()); // base temp dir
    final String contextPath = getContextPath();
    context.setContextPath(StringUtils.isNotEmpty(contextPath) ? contextPath : "/");
    context.setDisplayName(getDisplayName());
    configureDocumentRoot(context);

    configureLocaleMappings(context);
    configureWelcomePages(context);

    final Configuration[] configurations = getWebAppContextConfigurations(context);
    context.setConfigurations(configurations);
    context.setThrowUnavailableOnStartupException(throwUnavailableOnStartupException);

    // http session config
    final SessionConfiguration sessionConfig = getSessionConfig();
    if (sessionConfig != null && sessionConfig.isEnableHttpSession()) {
      configureSession(context, sessionConfig);
    }
  }

  protected void configureWelcomePages(WebAppContext context) {
    final Set<String> welcomePages = getWelcomePages();
    getWebApplicationConfiguration().configureWelcomePages(welcomePages);
    context.setWelcomeFiles(StringUtils.toStringArray(welcomePages));
  }

  /**
   * Return the Jetty {@link Configuration}s that should be applied to the server.
   *
   * @param webAppContext the Jetty {@link WebAppContext}
   * @return configurations to apply
   */
  protected Configuration[] getWebAppContextConfigurations(final WebAppContext webAppContext) {
    final List<Configuration> configurations = new ArrayList<>();
    configurations.add(getJettyServletContextInitializer(webAppContext));

    configurations.addAll(getConfigurations()); // user define
    configurations.add(getErrorPageConfiguration());
    configurations.add(getMimeTypeConfiguration());
    return configurations.toArray(new Configuration[0]);
  }

  /**
   * Create a configuration that adds mime type mappings.
   *
   * @return a configuration for adding mime type mappings
   */
  protected Configuration getMimeTypeConfiguration() {
    return new AbstractConfiguration() {
      @Override
      public void configure(WebAppContext context) {
        final MimeTypes mimeTypes = context.getMimeTypes();
        final MimeMappings mimeMappings = getMimeMappings();
        getWebApplicationConfiguration().configureMimeMappings(mimeMappings);
        for (MimeMappings.Mapping mapping : mimeMappings) {
          mimeTypes.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
        }
      }
    };
  }

  /**
   * Get a configuration that adds error pages.
   *
   * @return a configuration to add error pages
   */
  protected Configuration getErrorPageConfiguration() {
    return new AbstractConfiguration() {
      @Override
      public void configure(WebAppContext context) {
        addJettyErrorPages(context.getErrorHandler(), getErrorPages());
      }
    };
  }

  /**
   * Add jetty {@link ErrorPage}
   */
  protected void addJettyErrorPages(ErrorHandler errorHandler, Set<ErrorPage> errorPages) {
    getWebApplicationConfiguration().configureErrorPages(errorPages);
    if (errorHandler instanceof ErrorPageErrorHandler handler) {
      for (ErrorPage errorPage : errorPages) {
        if (errorPage.getException() != null) {
          handler.addErrorPage(errorPage.getException(), errorPage.getPath());
        }
        if (errorPage.getStatus() != 0) {
          handler.addErrorPage(errorPage.getStatus(), errorPage.getPath());
        }
      }
    }
  }

  /**
   * Return a Jetty {@link Configuration} that will invoke the specified
   * {@link ServletContextInitializer}s. By default this method will return a
   * {@link ServletContextInitializerConfiguration}.
   *
   * @param webAppContext the Jetty {@link WebAppContext}
   * @return the {@link Configuration} instance
   */
  protected Configuration getJettyServletContextInitializer(final WebAppContext webAppContext) {
    return new ServletContextInitializerConfiguration(this::getMergedInitializers);
  }

  /**
   * Configure session timeout, store directory
   *
   * @param context jetty web app context
   * @param sessionConfig SessionConfiguration
   */
  protected void configureSession(final WebAppContext context, SessionConfiguration sessionConfig) {
    final SessionHandler sessionHandler = context.getSessionHandler();
    final Duration sessionTimeout = sessionConfig.getTimeout();
    sessionHandler.setMaxInactiveInterval(isNegative(sessionTimeout)
                                          ? -1
                                          : (int) sessionTimeout.getSeconds());

    if (sessionConfig.isPersistent()) {
      final DefaultSessionCache cache = new DefaultSessionCache(sessionHandler);
      final FileSessionDataStore store = new FileSessionDataStore();
      try {
        Class<?> mainApplicationClass = getMainApplicationClass();
        store.setStoreDir(getStoreDirectory(mainApplicationClass));
      }
      catch (IOException e) {
        throw new ConfigurationException(e);
      }

      cache.setSessionDataStore(store);
      sessionHandler.setSessionCache(cache);
    }
  }

  private boolean isNegative(Duration sessionTimeout) {
    return sessionTimeout == null || sessionTimeout.isNegative();
  }

  protected void configureLocaleMappings(WebAppContext context) {
    for (Entry<Locale, Charset> entry : getLocaleCharsetMappings().entrySet()) {
      context.addLocaleEncoding(entry.getKey().toString(), entry.getValue().toString());
    }
  }

  /**
   * Configure jetty root document dir
   */
  protected void configureDocumentRoot(final WebAppContext webAppContext) {
    final WebDocumentConfiguration webDocument = getWebDocumentConfiguration();
    try {
      webAppContext.setBaseResource(getRootResource(webDocument == null
                                                    ? null
                                                    : webDocument.getValidDocumentDirectory()));
    }
    catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  protected Resource getRootResource(final cn.taketoday.core.io.Resource validDocBase) throws IOException {

    if (validDocBase instanceof cn.taketoday.core.io.JarResource) {
      return JarResource.newJarResource(Resource.newResource(validDocBase.getFile()));
    }
    if (validDocBase instanceof FileBasedResource) {
      return Resource.newResource(validDocBase.getFile());
    }
    if (validDocBase instanceof ClassPathResource) {
      return getRootResource(((ClassPathResource) validDocBase).getOriginalResource());
    }
    return Resource.newResource(getTemporalDirectory("jetty-docbase"));
  }

  @Override
  protected Servlet createDefaultServlet() {
    return new DefaultServlet();
  }

  protected GzipHandler configureCompression(final CompressionConfiguration compression) {
    GzipHandler handler = new GzipHandler();
    // handler.setCompressionLevel(compression.getLevel());

    handler.setMinGzipSize((int) compression.getMinResponseSize().toBytes());
    handler.addIncludedMimeTypes(compression.getMimeTypes());

    // ---path
    if (ObjectUtils.isNotEmpty(compression.getIncludedPaths())) {
      handler.addIncludedPaths(compression.getIncludedPaths());
    }
    if (ObjectUtils.isNotEmpty(compression.getExcludePaths())) {
      handler.addExcludedPaths(compression.getExcludePaths());
    }
    // --- method
    if (ObjectUtils.isNotEmpty(compression.getIncludeMethods())) {
      handler.addIncludedMethods(compression.getIncludeMethods());
    }

    if (ObjectUtils.isNotEmpty(compression.getExcludeMethods())) {
      handler.addExcludedMethods(compression.getExcludeMethods());
    }
    return handler;
  }

  /**
   * Use {@link ServletWebServerApplicationLoader} load application
   *
   * @author TODAY <br>
   * 2019-10-14 01:07
   */
  public class ServletContextInitializerConfiguration extends AbstractConfiguration {
    private ServletWebServerApplicationLoader starter;

    public ServletContextInitializerConfiguration(final Supplier<List<WebApplicationInitializer>> initializersSupplier) {
      this.starter = new ServletWebServerApplicationLoader(obtainApplicationContext(), initializersSupplier);
    }

    @Override
    public void configure(WebAppContext context) {
      context.addBean(new Initializer(context), true);
    }

    private class Initializer extends AbstractLifeCycle {

      private final WebAppContext context;

      Initializer(WebAppContext context) {
        this.context = context;
      }

      @Override
      protected void doStart() {

        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.context.getClassLoader());
        try {
          setExtendedListenerTypes(true);
          final Context servletContext = this.context.getServletContext();

          starter.onStartup(null, servletContext);
        }
        finally {
          setExtendedListenerTypes(false);
          Thread.currentThread().setContextClassLoader(oldClassLoader);
          starter = null;
        }
      }

      private void setExtendedListenerTypes(boolean extended) {
        this.context.getServletContext().setExtendedListenerTypes(extended);
      }
    }
  }

}
