/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ListenerHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.servlet.Source;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventListener;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.web.server.Cookie.SameSite;
import cn.taketoday.framework.web.server.ErrorPage;
import cn.taketoday.framework.web.server.MimeMappings;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import cn.taketoday.framework.web.servlet.server.AbstractServletWebServerFactory;
import cn.taketoday.framework.web.servlet.server.CookieSameSiteSupplier;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * {@link ServletWebServerFactory} that can be used to create a {@link JettyWebServer}.
 * Can be initialized using Framework's {@link ServletContextInitializer}s or Jetty
 * {@link Configuration}s.
 * <p>
 * Unless explicitly configured otherwise this factory will create servers that listen for
 * HTTP requests on port 8080.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andrey Hihlovskiy
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Venil Noronha
 * @author Henri Kerola
 * @see #setPort(int)
 * @see #setConfigurations(Collection)
 * @see JettyWebServer
 * @since 4.0
 */
public class JettyServletWebServerFactory extends AbstractServletWebServerFactory
        implements ConfigurableJettyWebServerFactory, ResourceLoaderAware {

  private List<Configuration> configurations = new ArrayList<>();

  private boolean useForwardHeaders;

  /**
   * The number of acceptor threads to use.
   */
  private int acceptors = -1;

  /**
   * The number of selector threads to use.
   */
  private int selectors = -1;

  private Set<JettyServerCustomizer> jettyServerCustomizers = new LinkedHashSet<>();

  @Nullable
  private ResourceLoader resourceLoader;

  @Nullable
  private ThreadPool threadPool;

  /**
   * Create a new {@link JettyServletWebServerFactory} instance.
   */
  public JettyServletWebServerFactory() {
  }

  /**
   * Create a new {@link JettyServletWebServerFactory} that listens for requests using
   * the specified port.
   *
   * @param port the port to listen on
   */
  public JettyServletWebServerFactory(int port) {
    super(port);
  }

  /**
   * Create a new {@link JettyServletWebServerFactory} with the specified context path
   * and port.
   *
   * @param contextPath the root context path
   * @param port the port to listen on
   */
  public JettyServletWebServerFactory(String contextPath, int port) {
    super(contextPath, port);
  }

  @Override
  public WebServer getWebServer(ServletContextInitializer... initializers) {
    JettyEmbeddedWebAppContext context = new JettyEmbeddedWebAppContext();
    int port = Math.max(getPort(), 0);
    InetSocketAddress address = new InetSocketAddress(getAddress(), port);
    Server server = createServer(address);
    configureWebAppContext(context, initializers);
    server.setHandler(addHandlerWrappers(context));
    this.logger.info("Server initialized with port: {}", port);
    if (getSsl() != null && getSsl().isEnabled()) {
      customizeSsl(server, address);
    }
    for (JettyServerCustomizer customizer : getServerCustomizers()) {
      customizer.customize(server);
    }
    if (this.useForwardHeaders) {
      new ForwardHeadersCustomizer().customize(server);
    }
    if (getShutdown() == Shutdown.GRACEFUL) {
      StatisticsHandler statisticsHandler = new StatisticsHandler();
      statisticsHandler.setHandler(server.getHandler());
      server.setHandler(statisticsHandler);
    }
    return getJettyWebServer(server);
  }

  private Server createServer(InetSocketAddress address) {
    Server server = new Server(getThreadPool());
    server.setConnectors(new Connector[] { createConnector(address, server) });
    server.setStopTimeout(0);
    return server;
  }

  private AbstractConnector createConnector(InetSocketAddress address, Server server) {
    HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setSendServerVersion(false);
    List<ConnectionFactory> connectionFactories = new ArrayList<>();
    connectionFactories.add(new HttpConnectionFactory(httpConfiguration));
    if (getHttp2() != null && getHttp2().isEnabled()) {
      connectionFactories.add(new HTTP2CServerConnectionFactory(httpConfiguration));
    }
    ServerConnector connector = new ServerConnector(server, this.acceptors, this.selectors,
            connectionFactories.toArray(new ConnectionFactory[0]));
    connector.setHost(address.getHostString());
    connector.setPort(address.getPort());
    return connector;
  }

  private Handler addHandlerWrappers(Handler handler) {
    if (getCompression() != null && getCompression().getEnabled()) {
      handler = applyWrapper(handler, JettyHandlerWrappers.createGzipHandlerWrapper(getCompression()));
    }
    if (StringUtils.hasText(getServerHeader())) {
      handler = applyWrapper(handler, JettyHandlerWrappers.createServerHeaderHandlerWrapper(getServerHeader()));
    }
    if (CollectionUtils.isNotEmpty(getCookieSameSiteSuppliers())) {
      handler = applyWrapper(handler, new SuppliedSameSiteCookieHandlerWrapper(getCookieSameSiteSuppliers()));
    }
    return handler;
  }

  private Handler applyWrapper(Handler handler, HandlerWrapper wrapper) {
    wrapper.setHandler(handler);
    return wrapper;
  }

  private void customizeSsl(Server server, InetSocketAddress address) {
    new SslServerCustomizer(address, getSsl(), getSslStoreProvider(), getHttp2()).customize(server);
  }

  /**
   * Configure the given Jetty {@link WebAppContext} for use.
   *
   * @param context the context to configure
   * @param initializers the set of initializers to apply
   */
  protected final void configureWebAppContext(WebAppContext context, ServletContextInitializer... initializers) {
    Assert.notNull(context, "Context must not be null");
    context.clearAliasChecks();
    context.setTempDirectory(getTempDirectory());
    if (this.resourceLoader != null) {
      context.setClassLoader(this.resourceLoader.getClassLoader());
    }
    String contextPath = getContextPath();
    context.setContextPath(StringUtils.isNotEmpty(contextPath) ? contextPath : "/");
    context.setDisplayName(getDisplayName());
    configureDocumentRoot(context);
    if (isRegisterDefaultServlet()) {
      addDefaultServlet(context);
    }
    if (shouldRegisterJspServlet()) {
      addJspServlet(context);
      context.addBean(new JasperInitializer(context), true);
    }
    addLocaleMappings(context);
    ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
    Configuration[] configurations = getWebAppContextConfigurations(context, initializersToUse);
    context.setConfigurations(configurations);
    context.setThrowUnavailableOnStartupException(true);
    configureSession(context);
    postProcessWebAppContext(context);
  }

  private void configureSession(WebAppContext context) {
    SessionHandler handler = context.getSessionHandler();
    SameSite sessionSameSite = getSession().getCookie().getSameSite();
    if (sessionSameSite != null) {
      handler.setSameSite(HttpCookie.SameSite.valueOf(sessionSameSite.name()));
    }
    Duration sessionTimeout = getSession().getTimeout();
    handler.setMaxInactiveInterval(isNegative(sessionTimeout) ? -1 : (int) sessionTimeout.getSeconds());
    if (getSession().isPersistent()) {
      DefaultSessionCache cache = new DefaultSessionCache(handler);
      FileSessionDataStore store = new FileSessionDataStore();
      store.setStoreDir(getValidSessionStoreDir());
      cache.setSessionDataStore(store);
      handler.setSessionCache(cache);
    }
  }

  private boolean isNegative(@Nullable Duration sessionTimeout) {
    return sessionTimeout == null || sessionTimeout.isNegative();
  }

  private void addLocaleMappings(WebAppContext context) {
    getLocaleCharsetMappings()
            .forEach((locale, charset) -> context.addLocaleEncoding(locale.toString(), charset.toString()));
  }

  @Nullable
  private File getTempDirectory() {
    String temp = System.getProperty("java.io.tmpdir");
    return (temp != null) ? new File(temp) : null;
  }

  private void configureDocumentRoot(WebAppContext handler) {
    File root = getValidDocumentRoot();
    File docBase = (root != null) ? root : createTempDir("jetty-docbase");
    try {
      ArrayList<Resource> resources = new ArrayList<>();
      Resource rootResource = docBase.isDirectory() ? Resource.newResource(docBase.getCanonicalFile())
                                                    : JarResource.newJarResource(Resource.newResource(docBase));
      resources.add(rootResource);
      for (URL resourceJarUrl : getUrlsOfJarsWithMetaInfResources()) {
        Resource resource = createResource(resourceJarUrl);
        if (resource.exists() && resource.isDirectory()) {
          resources.add(resource);
        }
      }
      handler.setBaseResource(new ResourceCollection(resources.toArray(new Resource[0])));
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private Resource createResource(URL url) throws Exception {
    if ("file".equals(url.getProtocol())) {
      File file = new File(url.toURI());
      if (file.isFile()) {
        return Resource.newResource("jar:" + url + "!/META-INF/resources");
      }
    }
    return Resource.newResource(url + "META-INF/resources");
  }

  /**
   * Add Jetty's {@code DefaultServlet} to the given {@link WebAppContext}.
   *
   * @param context the jetty {@link WebAppContext}
   */
  protected final void addDefaultServlet(WebAppContext context) {
    Assert.notNull(context, "Context must not be null");
    ServletHolder holder = new ServletHolder();
    holder.setName("default");
    holder.setClassName("org.eclipse.jetty.servlet.DefaultServlet");
    holder.setInitParameter("dirAllowed", "false");
    holder.setInitOrder(1);
    context.getServletHandler().addServletWithMapping(holder, "/");
    ServletMapping servletMapping = context.getServletHandler().getServletMapping("/");
    servletMapping.setFromDefaultDescriptor(true);
  }

  /**
   * Add Jetty's {@code JspServlet} to the given {@link WebAppContext}.
   *
   * @param context the jetty {@link WebAppContext}
   */
  protected final void addJspServlet(WebAppContext context) {
    Assert.notNull(context, "Context must not be null");
    ServletHolder holder = new ServletHolder();
    holder.setName("jsp");
    holder.setClassName(getJsp().getClassName());
    holder.setInitParameter("fork", "false");
    holder.setInitParameters(getJsp().getInitParameters());
    holder.setInitOrder(3);
    context.getServletHandler().addServlet(holder);
    ServletMapping mapping = new ServletMapping();
    mapping.setServletName("jsp");
    mapping.setPathSpecs(new String[] { "*.jsp", "*.jspx" });
    context.getServletHandler().addServletMapping(mapping);
  }

  /**
   * Return the Jetty {@link Configuration}s that should be applied to the server.
   *
   * @param webAppContext the Jetty {@link WebAppContext}
   * @param initializers the {@link ServletContextInitializer}s to apply
   * @return configurations to apply
   */
  protected Configuration[] getWebAppContextConfigurations(
          WebAppContext webAppContext, ServletContextInitializer... initializers) {
    List<Configuration> configurations = new ArrayList<>();
    configurations.add(getServletContextInitializerConfiguration(webAppContext, initializers));
    configurations.add(getErrorPageConfiguration());
    configurations.add(getMimeTypeConfiguration());
    configurations.add(new WebListenersConfiguration(getWebListenerClassNames()));
    configurations.addAll(getConfigurations());
    return configurations.toArray(new Configuration[0]);
  }

  /**
   * Create a configuration object that adds error handlers.
   *
   * @return a configuration object for adding error pages
   */
  private Configuration getErrorPageConfiguration() {
    return new AbstractConfiguration() {

      @Override
      public void configure(WebAppContext context) {
        JettyEmbeddedErrorHandler errorHandler = new JettyEmbeddedErrorHandler();
        context.setErrorHandler(errorHandler);
        addJettyErrorPages(errorHandler, getErrorPages());
      }

    };
  }

  /**
   * Create a configuration object that adds mime type mappings.
   *
   * @return a configuration object for adding mime type mappings
   */
  private Configuration getMimeTypeConfiguration() {
    return new AbstractConfiguration() {

      @Override
      public void configure(WebAppContext context) {
        MimeTypes mimeTypes = context.getMimeTypes();
        for (MimeMappings.Mapping mapping : getMimeMappings()) {
          mimeTypes.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
        }
      }

    };
  }

  /**
   * Return a Jetty {@link Configuration} that will invoke the specified
   * {@link ServletContextInitializer}s. By default this method will return a
   * {@link ServletContextInitializerConfiguration}.
   *
   * @param webAppContext the Jetty {@link WebAppContext}
   * @param initializers the {@link ServletContextInitializer}s to apply
   * @return the {@link Configuration} instance
   */
  protected Configuration getServletContextInitializerConfiguration(
          WebAppContext webAppContext, ServletContextInitializer... initializers) {
    return new ServletContextInitializerConfiguration(initializers);
  }

  /**
   * Post process the Jetty {@link WebAppContext} before it's used with the Jetty
   * Server. Subclasses can override this method to apply additional processing to the
   * {@link WebAppContext}.
   *
   * @param webAppContext the Jetty {@link WebAppContext}
   */
  protected void postProcessWebAppContext(WebAppContext webAppContext) { }

  /**
   * Factory method called to create the {@link JettyWebServer}. Subclasses can override
   * this method to return a different {@link JettyWebServer} or apply additional
   * processing to the Jetty server.
   *
   * @param server the Jetty server.
   * @return a new {@link JettyWebServer} instance
   */
  protected JettyWebServer getJettyWebServer(Server server) {
    return new JettyWebServer(server, getPort() >= 0);
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Override
  public void setUseForwardHeaders(boolean useForwardHeaders) {
    this.useForwardHeaders = useForwardHeaders;
  }

  @Override
  public void setAcceptors(int acceptors) {
    this.acceptors = acceptors;
  }

  @Override
  public void setSelectors(int selectors) {
    this.selectors = selectors;
  }

  /**
   * Sets {@link JettyServerCustomizer}s that will be applied to the {@link Server}
   * before it is started. Calling this method will replace any existing customizers.
   *
   * @param customizers the Jetty customizers to apply
   */
  public void setServerCustomizers(Collection<? extends JettyServerCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    this.jettyServerCustomizers = new LinkedHashSet<>(customizers);
  }

  /**
   * Returns a mutable collection of Jetty {@link JettyServerCustomizer}s that will be
   * applied to the {@link Server} before the it is created.
   *
   * @return the {@link JettyServerCustomizer}s
   */
  public Collection<JettyServerCustomizer> getServerCustomizers() {
    return this.jettyServerCustomizers;
  }

  @Override
  public void addServerCustomizers(JettyServerCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    this.jettyServerCustomizers.addAll(Arrays.asList(customizers));
  }

  /**
   * Sets Jetty {@link Configuration}s that will be applied to the {@link WebAppContext}
   * before the server is created. Calling this method will replace any existing
   * configurations.
   *
   * @param configurations the Jetty configurations to apply
   */
  public void setConfigurations(Collection<? extends Configuration> configurations) {
    Assert.notNull(configurations, "Configurations must not be null");
    this.configurations = new ArrayList<>(configurations);
  }

  /**
   * Returns a mutable collection of Jetty {@link Configuration}s that will be applied
   * to the {@link WebAppContext} before the server is created.
   *
   * @return the Jetty {@link Configuration}s
   */
  public Collection<Configuration> getConfigurations() {
    return this.configurations;
  }

  /**
   * Add {@link Configuration}s that will be applied to the {@link WebAppContext} before
   * the server is started.
   *
   * @param configurations the configurations to add
   */
  public void addConfigurations(Configuration... configurations) {
    Assert.notNull(configurations, "Configurations must not be null");
    this.configurations.addAll(Arrays.asList(configurations));
  }

  /**
   * Returns a Jetty {@link ThreadPool} that should be used by the {@link Server}.
   *
   * @return a Jetty {@link ThreadPool} or {@code null}
   */
  @Nullable
  public ThreadPool getThreadPool() {
    return this.threadPool;
  }

  @Override
  public void setThreadPool(ThreadPool threadPool) {
    this.threadPool = threadPool;
  }

  private void addJettyErrorPages(ErrorHandler errorHandler, Collection<ErrorPage> errorPages) {
    if (errorHandler instanceof ErrorPageErrorHandler handler) {
      for (ErrorPage errorPage : errorPages) {
        if (errorPage.isGlobal()) {
          handler.addErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE, errorPage.getPath());
        }
        else {
          if (errorPage.getExceptionName() != null) {
            handler.addErrorPage(errorPage.getExceptionName(), errorPage.getPath());
          }
          else {
            handler.addErrorPage(errorPage.getStatusCode(), errorPage.getPath());
          }
        }
      }
    }
  }

  /**
   * {@link AbstractConfiguration} to apply {@code @WebListener} classes.
   */
  private static class WebListenersConfiguration extends AbstractConfiguration {

    private final Set<String> classNames;

    WebListenersConfiguration(Set<String> webListenerClassNames) {
      this.classNames = webListenerClassNames;
    }

    @Override
    public void configure(WebAppContext context) throws Exception {
      ServletHandler servletHandler = context.getServletHandler();
      for (String className : this.classNames) {
        configure(context, servletHandler, className);
      }
    }

    private void configure(WebAppContext context, ServletHandler servletHandler, String className)
            throws ClassNotFoundException {
      ListenerHolder holder = servletHandler.newListenerHolder(new Source(Source.Origin.ANNOTATION, className));
      holder.setHeldClass(loadClass(context, className));
      servletHandler.addListener(holder);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends EventListener> loadClass(WebAppContext context, String className)
            throws ClassNotFoundException {
      ClassLoader classLoader = context.getClassLoader();
      classLoader = (classLoader != null) ? classLoader : getClass().getClassLoader();
      return (Class<? extends EventListener>) classLoader.loadClass(className);
    }

  }

  /**
   * {@link HandlerWrapper} to apply {@link CookieSameSiteSupplier supplied}
   * {@link SameSite} cookie values.
   */
  private static class SuppliedSameSiteCookieHandlerWrapper extends HandlerWrapper {

    private final List<CookieSameSiteSupplier> suppliers;

    SuppliedSameSiteCookieHandlerWrapper(List<CookieSameSiteSupplier> suppliers) {
      this.suppliers = suppliers;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
      HttpServletResponse wrappedResponse = new ResponseWrapper(response);
      super.handle(target, baseRequest, request, wrappedResponse);
    }

    class ResponseWrapper extends HttpServletResponseWrapper {

      ResponseWrapper(HttpServletResponse response) {
        super(response);
      }

      @Override
      public void addCookie(Cookie cookie) {
        SameSite sameSite = getSameSite(cookie);
        if (sameSite != null) {
          String comment = HttpCookie.getCommentWithoutAttributes(cookie.getComment());
          String sameSiteComment = getSameSiteComment(sameSite);
          cookie.setComment((comment != null) ? comment + sameSiteComment : sameSiteComment);
        }
        super.addCookie(cookie);
      }

      private String getSameSiteComment(SameSite sameSite) {
        return switch (sameSite) {
          case LAX -> HttpCookie.SAME_SITE_LAX_COMMENT;
          case NONE -> HttpCookie.SAME_SITE_NONE_COMMENT;
          case STRICT -> HttpCookie.SAME_SITE_STRICT_COMMENT;
        };
      }

      @Nullable
      private SameSite getSameSite(Cookie cookie) {
        for (CookieSameSiteSupplier supplier : SuppliedSameSiteCookieHandlerWrapper.this.suppliers) {
          SameSite sameSite = supplier.getSameSite(cookie);
          if (sameSite != null) {
            return sameSite;
          }
        }
        return null;
      }

    }

  }

}
