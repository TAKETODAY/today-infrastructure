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

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Service;
import org.apache.catalina.SessionIdGenerator;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.naming.ContextBindings;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.NamingException;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.support.DependencyInjectorAwareInstantiator;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.framework.WebServerException;
import cn.taketoday.web.framework.config.CompressionConfiguration;
import cn.taketoday.web.framework.config.ErrorPage;
import cn.taketoday.web.framework.config.JspServletConfiguration;
import cn.taketoday.web.framework.config.MimeMappings;
import cn.taketoday.web.framework.config.WebDocumentConfiguration;
import cn.taketoday.web.session.SessionConfiguration;
import cn.taketoday.web.session.SessionCookieConfig;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContainerInitializer;
import lombok.Getter;
import lombok.Setter;

/**
 * Tomcat Servlet web server
 *
 * @author TODAY 2018-10-15 20:44
 */
@Setter
@Getter
public class TomcatServer extends AbstractServletWebServer {

  // connector
  private String protocol = "HTTP/1.1";
  private File baseDirectory;

  // engine
  private int backgroundProcessorDelay = -1;

  // {@link AprLifecycleListener}
  private String SSLEngine = "on";
  private String SSLRandomSeed = "builtin";
  private String FIPSMode = "off";
  private boolean useOpenSSL = true;

  private String uriEncoding = Constant.DEFAULT_ENCODING;

  @Autowired(required = false)
  private Manager sessionManager;

  @Autowired(required = false)
  private SessionIdGenerator sessionIdGenerator = new LazySessionIdGenerator();

  private List<Valve> engineValves = new ArrayList<>();
  private List<Valve> contextValves = new ArrayList<>();

  /** additional connector */
  private List<Connector> additionalTomcatConnectors = new ArrayList<>();
  private List<LifecycleListener> contextLifecycleListeners = new ArrayList<>();

  private final Map<Service, Connector[]> serviceConnectors = new HashMap<>();

  private Tomcat tomcat;
  private boolean enableApr;
  private boolean autoStart = true;
  private boolean useRelativeRedirects = false;

  public boolean isAutoStart() {
    return autoStart;
  }

  public boolean isUseRelativeRedirects() {
    return useRelativeRedirects;
  }

  private Context findContext() {
    for (Container child : this.tomcat.getHost().findChildren()) {
      if (child instanceof Context) {
        return (Context) child;
      }
    }
    throw new IllegalStateException("The host does not contain a Context");
  }

  private void removeServiceConnectors() {

    for (Service service : this.tomcat.getServer().findServices()) {
      Connector[] connectors = service.findConnectors().clone();
      this.serviceConnectors.put(service, connectors);
      for (Connector connector : connectors) {
        service.removeConnector(connector);
      }
    }
  }

  private void startDaemonAwaitThread() {
    Thread awaitThread = new Thread("container-tomcat") {
      public void run() {
        TomcatServer.this.tomcat.getServer().await();
      }
    };

    awaitThread.setContextClassLoader(getClass().getClassLoader());
    awaitThread.setDaemon(false);
    awaitThread.start();
  }

  @Override
  public synchronized void start() throws WebServerException {
    if (getStarted().get()) {
      return;
    }
    try {
      addPreviouslyRemovedConnectors();

      getStarted().set(true);
      log.info("Tomcat started on port: [{}] with context path '{}'", getPort(), getContextPath());
    }
    catch (Throwable ex) {
      stopSilently();
      throw new WebServerException("Unable to start embedded Tomcat server", ex);
    }
    finally {
      Context context = findContext();
      ContextBindings.unbindClassLoader(context, context.getNamingToken(), getClass().getClassLoader());
    }
  }

  /**
   * Stop tomcat
   */
  private void stopSilently() {
    try {
      if (tomcat != null) {
        log.info("Tomcat is stopping");
        tomcat.stop();
        tomcat.destroy();
      }
      log.info("Tomcat is stopped");
    }
    catch (Throwable ex) {
      log.error("Tomcat failed to stop", ex);
      throw new WebServerException(ex);
    }
  }

  private void addPreviouslyRemovedConnectors() {
    Service[] services = this.tomcat.getServer().findServices();
    for (Service service : services) {
      Connector[] connectors = this.serviceConnectors.get(service);
      if (connectors != null) {
        for (Connector connector : connectors) {
          service.addConnector(connector);
          if (!this.autoStart) {
            stopProtocolHandler(connector);
          }
        }
        this.serviceConnectors.remove(service);
      }
    }
  }

  private void stopProtocolHandler(Connector connector) {
    try {
      connector.getProtocolHandler().stop();
    }
    catch (Exception ex) {
      log.error("Cannot pause connector: ", ex);
    }
  }

  @Override
  public synchronized void stop() throws WebServerException {
    try {
      if (getStarted().compareAndSet(true, false)) {
        stopSilently();
      }
    }
    catch (Exception ex) {
      throw new WebServerException("Unable to stop embedded Tomcat", ex);
    }
  }

  /**
   * Returns access to the underlying Tomcat server.
   *
   * @return the Tomcat server
   */
  public Tomcat getTomcat() {
    return this.tomcat;
  }

  @Override
  protected void initializeContext() {
    super.initializeContext();
    log.info("Initialize tomcat web server context");

    Tomcat tomcat = new Tomcat();
    this.tomcat = tomcat;
    doPrepareContext(tomcat.getHost());

    File baseDir = (this.baseDirectory != null) ? this.baseDirectory : getTemporalDirectory("tomcat");

    tomcat.setBaseDir(baseDir.getAbsolutePath());
    Connector connector = new Connector(this.protocol);

    tomcat.getService().addConnector(connector);
    configureConnector(connector);
    tomcat.setConnector(connector);

    configureEngine(tomcat.getEngine());
    for (Connector additionalConnector : this.additionalTomcatConnectors) {
      tomcat.getService().addConnector(additionalConnector);
    }
  }

  @Override
  protected void contextInitialized() {
    super.contextInitialized();
    try {
      Context context = findContext();
      context.addLifecycleListener((event) -> {
        if (context.equals(event.getSource()) && Lifecycle.START_EVENT.equals(event.getType())) {
          // Remove service connectors so that protocol binding doesn't
          // happen when the service is started.
          removeServiceConnectors();
        }
      });
      // Start the server to trigger initialization listeners
      this.tomcat.start();
      try {
        ContextBindings.bindClassLoader(
                context, context.getNamingToken(), getClass().getClassLoader());
      }
      catch (NamingException ex) {
        // Naming is not enabled. Continue
      }
      // Unlike Jetty, all Tomcat threads are daemon threads. We create a blocking non-daemon to stop immediate shutdown
      startDaemonAwaitThread();
    }
    catch (Exception ex) {
      stopSilently();
      throw new WebServerException("Unable to start embedded Tomcat", ex);
    }
  }

  private void configureEngine(Engine engine) {
    engine.setBackgroundProcessorDelay(this.backgroundProcessorDelay);
    for (Valve valve : this.engineValves) {
      engine.getPipeline().addValve(valve);
    }
  }

  /**
   * Configure {@link Connector}
   *
   * @param connector Connector
   */
  protected void configureConnector(Connector connector) {
    connector.setPort(getPort());
    if (StringUtils.isNotEmpty(getServerHeader())) {
      connector.setProperty("server", getServerHeader());
    }
    CompressionConfiguration compression = getCompression();
    // config compression
    if (compression != null) {
      getWebApplicationConfiguration().configureCompression(compression);
      if (compression.isEnable()) {
        ProtocolHandler handler = connector.getProtocolHandler();
        if (handler instanceof AbstractHttp11Protocol) {
          configureCompressionProtocol(compression, (AbstractHttp11Protocol<?>) handler);
        }
        else {
          log.warn("ProtocolHandler must be AbstractHttp11Protocol can support compression", handler);
        }
      }
    }
    connector.setURIEncoding(uriEncoding);
    connector.setProperty("bindOnInit", "false");
  }

  private void configureCompressionProtocol(CompressionConfiguration compression, AbstractHttp11Protocol<?> protocol) {
    if (isEnableHttp2()) {
      protocol.addUpgradeProtocol(new Http2Protocol());
    }
    protocol.setCompression(compression.getLevel());
    protocol.setCompressionMinSize((int) compression.getMinResponseSize().toBytes());
    protocol.setCompressibleMimeType(StringUtils.arrayToCommaDelimitedString(compression.getMimeTypes()));

    if (ObjectUtils.isNotEmpty(compression.getExcludeUserAgents())) {
      protocol.setNoCompressionUserAgents(StringUtils.arrayToCommaDelimitedString(compression.getExcludeUserAgents()));
    }
  }

  protected void doPrepareContext(Host host) {
    try {
      ServletWebServerApplicationLoader starter = //
              new ServletWebServerApplicationLoader(obtainApplicationContext(), this::getMergedInitializers);
      TomcatEmbeddedContext context = new TomcatEmbeddedContext(sessionIdGenerator);
      context.setFailCtxIfServletStartFails(true);

      context.setName(getContextPath());
      context.setDisplayName(getDisplayName());
      context.setPath(getContextPath());

      WebDocumentConfiguration webDocumentConfiguration = getWebDocumentConfiguration();
      if (webDocumentConfiguration != null) {
        Resource validDocBase = webDocumentConfiguration.getValidDocumentDirectory();
        if (validDocBase != null && validDocBase.exists() && validDocBase.isDirectory()) {
          context.setDocBase(validDocBase.getFile().getAbsolutePath());
        }
      }

      context.addLifecycleListener(new FixContextListener());
      context.setParentClassLoader(ClassUtils.getDefaultClassLoader());

      resetDefaultLocaleMapping(context);
      addLocaleMappings(context);

      context.setUseRelativeRedirects(useRelativeRedirects);

      WebappLoader loader = new WebappLoader();
      loader.setLoaderClass(WebappClassLoader.class.getName());
      loader.setDelegate(true);
      context.setLoader(loader);

      configureJasperInitializer(context);

      host.addChild(context);

      context.addServletContainerInitializer(starter, Collections.emptySet());
      configureTomcatContext(context);
    }
    catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  protected void configureJasperInitializer(TomcatEmbeddedContext context) {

    JspServletConfiguration jspServletConfiguration = getJspServletConfiguration();
    if (jspServletConfiguration != null && jspServletConfiguration.isEnabled()) {
      // org.apache.jasper.servlet.JasperInitializer
      Class<ServletContainerInitializer> jasperInitializer =
              ClassUtils.load("org.apache.jasper.servlet.JasperInitializer");
      if (jasperInitializer != null) {
        context.addServletContainerInitializer(
                DependencyInjectorAwareInstantiator.instantiate(
                        jasperInitializer, getApplicationContext()), null);
      }
      else {
        throw new ConfigurationException("no 'org.apache.jasper.servlet.JasperInitializer in classpath");
      }
    }
  }

  private void resetDefaultLocaleMapping(TomcatEmbeddedContext context) {
    context.addLocaleEncodingMappingParameter(Locale.ENGLISH.toString(), Constant.DEFAULT_CHARSET.displayName());
    context.addLocaleEncodingMappingParameter(Locale.FRENCH.toString(), Constant.DEFAULT_CHARSET.displayName());
  }

  protected void addLocaleMappings(TomcatEmbeddedContext context) {

    for (Entry<Locale, Charset> entry : getLocaleCharsetMappings().entrySet()) {
      context.addLocaleEncodingMappingParameter(entry.getKey().toString(), entry.getValue().toString());
    }
  }

  @Override
  protected void prepareInitialize() {
    super.prepareInitialize();

    prepareApr();
  }

  protected void prepareApr() {
    if (enableApr) {
      AprLifecycleListener aprLifecycleListener = getApplicationContext().getBean(AprLifecycleListener.class);
      if (aprLifecycleListener == null) {
        aprLifecycleListener = new AprLifecycleListener();

        aprLifecycleListener.setFIPSMode(FIPSMode);
        aprLifecycleListener.setSSLEngine(SSLEngine);
        aprLifecycleListener.setUseOpenSSL(useOpenSSL);
        aprLifecycleListener.setSSLRandomSeed(SSLRandomSeed);
      }
      contextLifecycleListeners.add(aprLifecycleListener);
    }
  }

  /**
   * Configure the Tomcat {@link Context}.
   *
   * @param context the Tomcat context
   */
  protected void configureTomcatContext(Context context) {

    for (LifecycleListener lifecycleListener : this.contextLifecycleListeners) {
      context.addLifecycleListener(lifecycleListener);
    }

    if (!this.contextValves.isEmpty()) {
      Pipeline pipeline = context.getPipeline();
      for (Valve valve : this.contextValves) {
        pipeline.addValve(valve);
      }
    }

    configureErrorPages(context);
    configureWelcomePages(context);

    MimeMappings mimeMappings = getMimeMappings();
    // config MimeMappings
    getWebApplicationConfiguration().configureMimeMappings(mimeMappings);

    for (MimeMappings.Mapping mapping : mimeMappings) {
      context.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
    }
    // internal servlet HttpSession settings
    SessionConfiguration sessionConfig = getSessionConfig();
    if (sessionConfig != null && sessionConfig.isEnableHttpSession()) {
      configureSession(context, sessionConfig);
    }
  }

  protected void configureWelcomePages(Context context) {
    Set<String> welcomePages = getWelcomePages();
    getWebApplicationConfiguration().configureWelcomePages(welcomePages);

    for (String welcomePage : welcomePages) {
      context.addWelcomeFile(welcomePage);
    }
  }

  /**
   *
   */
  protected void configureErrorPages(Context context) {
    Set<ErrorPage> errorPages = getErrorPages();

    // config error pages
    getWebApplicationConfiguration().configureErrorPages(errorPages);
    for (ErrorPage errorPage : errorPages) {
      org.apache.tomcat.util.descriptor.web.ErrorPage tomcatErrorPage = new org.apache.tomcat.util.descriptor.web.ErrorPage();
      if (errorPage.getPath() != null) {
        tomcatErrorPage.setLocation(errorPage.getPath());
      }
      tomcatErrorPage.setErrorCode(errorPage.getStatus());
      if (errorPage.getException() != null) {
        tomcatErrorPage.setExceptionType(errorPage.getException().getName());
      }
      context.addErrorPage(tomcatErrorPage);
    }
  }

  protected void configureSession(Context context, SessionConfiguration sessionConfig) {
    context.setSessionTimeout(getSessionTimeoutInMinutes(sessionConfig));
    SessionCookieConfig cookieConfig = sessionConfig.getCookieConfig();
    if (cookieConfig != null) {
      context.setUseHttpOnly(cookieConfig.isHttpOnly());
    }

    Manager manager = context.getManager();
    if (manager == null) {
      manager = getSessionManager();
      if (manager == null) {
        manager = new StandardManager();
      }
      context.setManager(manager);
    }

    if (sessionConfig.isPersistent()) {
      configurePersistSession(manager);
    }
    else {
      context.addLifecycleListener((event) -> {
        if (Lifecycle.START_EVENT.equals(event.getType())) {
          Context context_ = (Context) event.getLifecycle();
          Manager manager_ = context_.getManager();
          if (manager_ instanceof StandardManager) {
            ((StandardManager) manager_).setPathname(null);
          }
        }
      });
    }
  }

  protected int getSessionTimeoutInMinutes(SessionConfiguration sessionConfig) {
    Duration sessionTimeout = sessionConfig.getTimeout();
    if (isZeroOrLess(sessionTimeout)) {
      return 0;
    }
    return (int) Math.max(sessionTimeout.toMinutes(), 1);
  }

  protected void configurePersistSession(Manager manager) {
    if (manager instanceof StandardManager) {
      try {
        Class<?> mainApplicationClass = getMainApplicationClass();
        File storeDirectory = getStoreDirectory(mainApplicationClass);
        ((StandardManager) manager).setPathname(new File(storeDirectory, "SESSIONS.ser").getAbsolutePath());
      }
      catch (IOException e) {
        throw new ConfigurationException(e);
      }
    }
  }

  @Override
  protected Servlet createDefaultServlet() {
    return new DefaultServlet();
  }

  public void setEnableApr(boolean enableApr) {
    this.enableApr = enableApr;
  }

  public boolean isEnableApr() {
    return enableApr;
  }

  private static final class LazySessionIdGenerator extends StandardSessionIdGenerator {

    @Override
    protected void startInternal() throws LifecycleException {
      setState(LifecycleState.STARTING);
    }

  }

}
