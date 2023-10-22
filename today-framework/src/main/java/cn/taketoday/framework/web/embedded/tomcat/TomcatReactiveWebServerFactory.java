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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.scan.StandardJarScanFilter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.Http2;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.TomcatHttpHandlerAdapter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LambdaSafe;
import cn.taketoday.util.StringUtils;

/**
 * {@link ReactiveWebServerFactory} that can be used to create a {@link TomcatWebServer}.
 *
 * @author Brian Clozel
 * @author HaiTao Zhang
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class TomcatReactiveWebServerFactory extends AbstractReactiveWebServerFactory
        implements ConfigurableTomcatWebServerFactory {

  private static final Logger log = LoggerFactory.getLogger(TomcatServletWebServerFactory.class);

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  /**
   * The class name of default protocol used.
   */
  public static final String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

  @Nullable
  private File baseDirectory;

  private final List<Valve> engineValves = new ArrayList<>();

  private List<LifecycleListener> contextLifecycleListeners = new ArrayList<>();

  private final List<LifecycleListener> serverLifecycleListeners = getDefaultServerLifecycleListeners();

  private Set<TomcatContextCustomizer> tomcatContextCustomizers = new LinkedHashSet<>();

  private Set<TomcatConnectorCustomizer> tomcatConnectorCustomizers = new LinkedHashSet<>();

  private Set<TomcatProtocolHandlerCustomizer<?>> tomcatProtocolHandlerCustomizers = new LinkedHashSet<>();

  private final List<Connector> additionalTomcatConnectors = new ArrayList<>();

  private String protocol = DEFAULT_PROTOCOL;

  private Charset uriEncoding = DEFAULT_CHARSET;

  private int backgroundProcessorDelay;

  private boolean disableMBeanRegistry = true;

  /**
   * Create a new {@link TomcatReactiveWebServerFactory} instance.
   */
  public TomcatReactiveWebServerFactory() { }

  /**
   * Create a new {@link TomcatReactiveWebServerFactory} that listens for requests using
   * the specified port.
   *
   * @param port the port to listen on
   */
  public TomcatReactiveWebServerFactory(int port) {
    super(port);
  }

  private static List<LifecycleListener> getDefaultServerLifecycleListeners() {
    return AprLifecycleListener.isAprAvailable()
           ? CollectionUtils.newArrayList(new AprLifecycleListener()) : new ArrayList<>();
  }

  @Override
  public WebServer getWebServer(HttpHandler httpHandler) {
    if (this.disableMBeanRegistry) {
      Registry.disableRegistry();
    }
    Tomcat tomcat = new Tomcat();
    File baseDir = baseDirectory != null ? baseDirectory : createTempDir("tomcat");
    tomcat.setBaseDir(baseDir.getAbsolutePath());
    for (LifecycleListener listener : serverLifecycleListeners) {
      tomcat.getServer().addLifecycleListener(listener);
    }
    Connector connector = new Connector(protocol);
    connector.setThrowOnFailure(true);
    tomcat.getService().addConnector(connector);
    customizeConnector(connector);
    tomcat.setConnector(connector);
    tomcat.getHost().setAutoDeploy(false);
    configureEngine(tomcat.getEngine());
    for (Connector additionalConnector : additionalTomcatConnectors) {
      tomcat.getService().addConnector(additionalConnector);
    }
    TomcatHttpHandlerAdapter servlet = new TomcatHttpHandlerAdapter(httpHandler);
    prepareContext(tomcat.getHost(), servlet);
    return getTomcatWebServer(tomcat);
  }

  private void configureEngine(Engine engine) {
    engine.setBackgroundProcessorDelay(this.backgroundProcessorDelay);
    for (Valve valve : this.engineValves) {
      engine.getPipeline().addValve(valve);
    }
  }

  protected void prepareContext(Host host, TomcatHttpHandlerAdapter servlet) {
    File docBase = createTempDir("tomcat-docbase");
    TomcatEmbeddedContext context = new TomcatEmbeddedContext();
    context.setPath("");
    context.setDocBase(docBase.getAbsolutePath());
    context.addLifecycleListener(new Tomcat.FixContextListener());
    context.setParentClassLoader(ClassUtils.getDefaultClassLoader());
    skipAllTldScanning(context);
    WebappLoader loader = new WebappLoader();
    loader.setLoaderClass(TomcatEmbeddedWebappClassLoader.class.getName());
    loader.setDelegate(true);
    context.setLoader(loader);
    Tomcat.addServlet(context, "httpHandlerServlet", servlet).setAsyncSupported(true);
    context.addServletMappingDecoded("/", "httpHandlerServlet");
    host.addChild(context);
    configureContext(context);
  }

  private void skipAllTldScanning(TomcatEmbeddedContext context) {
    StandardJarScanFilter filter = new StandardJarScanFilter();
    filter.setTldSkip("*.jar");
    context.getJarScanner().setJarScanFilter(filter);
  }

  /**
   * Configure the Tomcat {@link Context}.
   *
   * @param context the Tomcat context
   */
  protected void configureContext(Context context) {
    this.contextLifecycleListeners.forEach(context::addLifecycleListener);
    TomcatServletWebServerFactory.disableReferenceClearing(context);

    for (TomcatContextCustomizer customizer : tomcatContextCustomizers) {
      customizer.customize(context);
    }
  }

  protected void customizeConnector(Connector connector) {
    int port = Math.max(getPort(), 0);
    connector.setPort(port);
    if (StringUtils.hasText(getServerHeader())) {
      connector.setProperty("server", getServerHeader());
    }
    if (connector.getProtocolHandler() instanceof AbstractProtocol<?> abstractProtocol) {
      customizeProtocol(abstractProtocol);
    }
    invokeProtocolHandlerCustomizers(connector.getProtocolHandler());
    connector.setURIEncoding(getUriEncoding().name());
    // Don't bind to the socket prematurely if ApplicationContext is slow to start
    connector.setProperty("bindOnInit", "false");
    if (Http2.isEnabled(getHttp2())) {
      connector.addUpgradeProtocol(new Http2Protocol());
    }
    if (Ssl.isEnabled(getSsl())) {
      customizeSsl(getSsl(), connector);
    }

    CompressionConnectorCustomizer.customize(connector, getCompression());
    for (TomcatConnectorCustomizer customizer : tomcatConnectorCustomizers) {
      customizer.customize(connector);
    }
  }

  @SuppressWarnings("unchecked")
  private void invokeProtocolHandlerCustomizers(ProtocolHandler protocolHandler) {
    LambdaSafe.callbacks(TomcatProtocolHandlerCustomizer.class, tomcatProtocolHandlerCustomizers, protocolHandler)
            .invoke(customizer -> customizer.customize(protocolHandler));
  }

  private void customizeProtocol(AbstractProtocol<?> protocol) {
    if (getAddress() != null) {
      protocol.setAddress(getAddress());
    }
  }

  private void customizeSsl(Ssl ssl, Connector connector) {
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(log, connector, ssl.getClientAuth());
    customizer.customize(getSslBundle());
    String sslBundleName = ssl.getBundle();
    if (StringUtils.hasText(sslBundleName)) {
      getSslBundles().addBundleUpdateHandler(sslBundleName, customizer::update);
    }
  }

  @Override
  public void setBaseDirectory(File baseDirectory) {
    this.baseDirectory = baseDirectory;
  }

  @Override
  public void setBackgroundProcessorDelay(int delay) {
    this.backgroundProcessorDelay = delay;
  }

  /**
   * Set {@link TomcatContextCustomizer}s that should be applied to the Tomcat
   * {@link Context}. Calling this method will replace any existing customizers.
   *
   * @param tomcatContextCustomizers the customizers to set
   */
  public void setTomcatContextCustomizers(Collection<? extends TomcatContextCustomizer> tomcatContextCustomizers) {
    Assert.notNull(tomcatContextCustomizers, "TomcatContextCustomizers must not be null");
    this.tomcatContextCustomizers = new LinkedHashSet<>(tomcatContextCustomizers);
  }

  /**
   * Returns a mutable collection of the {@link TomcatContextCustomizer}s that will be
   * applied to the Tomcat {@link Context}.
   *
   * @return the listeners that will be applied
   */
  public Collection<TomcatContextCustomizer> getTomcatContextCustomizers() {
    return this.tomcatContextCustomizers;
  }

  /**
   * Add {@link TomcatContextCustomizer}s that should be added to the Tomcat
   * {@link Context}.
   *
   * @param tomcatContextCustomizers the customizers to add
   */
  @Override
  public void addContextCustomizers(TomcatContextCustomizer... tomcatContextCustomizers) {
    Assert.notNull(tomcatContextCustomizers, "TomcatContextCustomizers must not be null");
    CollectionUtils.addAll(this.tomcatContextCustomizers, tomcatContextCustomizers);
  }

  /**
   * Set {@link TomcatConnectorCustomizer}s that should be applied to the Tomcat
   * {@link Connector}. Calling this method will replace any existing customizers.
   *
   * @param tomcatConnectorCustomizers the customizers to set
   */
  public void setTomcatConnectorCustomizers(
          Collection<? extends TomcatConnectorCustomizer> tomcatConnectorCustomizers) {
    Assert.notNull(tomcatConnectorCustomizers, "TomcatConnectorCustomizers must not be null");
    this.tomcatConnectorCustomizers = new LinkedHashSet<>(tomcatConnectorCustomizers);
  }

  /**
   * Add {@link TomcatConnectorCustomizer}s that should be added to the Tomcat
   * {@link Connector}.
   *
   * @param tomcatConnectorCustomizers the customizers to add
   */
  @Override
  public void addConnectorCustomizers(TomcatConnectorCustomizer... tomcatConnectorCustomizers) {
    Assert.notNull(tomcatConnectorCustomizers, "TomcatConnectorCustomizers must not be null");
    this.tomcatConnectorCustomizers.addAll(Arrays.asList(tomcatConnectorCustomizers));
  }

  /**
   * Returns a mutable collection of the {@link TomcatConnectorCustomizer}s that will be
   * applied to the Tomcat {@link Connector}.
   *
   * @return the customizers that will be applied
   */
  public Collection<TomcatConnectorCustomizer> getTomcatConnectorCustomizers() {
    return this.tomcatConnectorCustomizers;
  }

  /**
   * Set {@link TomcatProtocolHandlerCustomizer}s that should be applied to the Tomcat
   * {@link Connector}. Calling this method will replace any existing customizers.
   *
   * @param tomcatProtocolHandlerCustomizers the customizers to set
   */
  public void setTomcatProtocolHandlerCustomizers(
          Collection<? extends TomcatProtocolHandlerCustomizer<?>> tomcatProtocolHandlerCustomizers) {
    Assert.notNull(tomcatProtocolHandlerCustomizers, "TomcatProtocolHandlerCustomizers must not be null");
    this.tomcatProtocolHandlerCustomizers = new LinkedHashSet<>(tomcatProtocolHandlerCustomizers);
  }

  /**
   * Add {@link TomcatProtocolHandlerCustomizer}s that should be added to the Tomcat
   * {@link Connector}.
   *
   * @param tomcatProtocolHandlerCustomizers the customizers to add
   */
  @Override
  public void addProtocolHandlerCustomizers(TomcatProtocolHandlerCustomizer<?>... tomcatProtocolHandlerCustomizers) {
    Assert.notNull(tomcatProtocolHandlerCustomizers, "TomcatProtocolHandlerCustomizers must not be null");
    CollectionUtils.addAll(this.tomcatProtocolHandlerCustomizers, tomcatProtocolHandlerCustomizers);
  }

  /**
   * Returns a mutable collection of the {@link TomcatProtocolHandlerCustomizer}s that
   * will be applied to the Tomcat {@link Connector}.
   *
   * @return the customizers that will be applied
   */
  public Collection<TomcatProtocolHandlerCustomizer<?>> getTomcatProtocolHandlerCustomizers() {
    return this.tomcatProtocolHandlerCustomizers;
  }

  /**
   * Add {@link Connector}s in addition to the default connector, e.g. for SSL or AJP
   *
   * @param connectors the connectors to add
   */
  public void addAdditionalTomcatConnectors(Connector... connectors) {
    Assert.notNull(connectors, "Connectors must not be null");
    CollectionUtils.addAll(this.additionalTomcatConnectors, connectors);
  }

  /**
   * Returns a mutable collection of the {@link Connector}s that will be added to the
   * Tomcat.
   *
   * @return the additionalTomcatConnectors
   */
  public List<Connector> getAdditionalTomcatConnectors() {
    return this.additionalTomcatConnectors;
  }

  @Override
  public void addEngineValves(Valve... engineValves) {
    Assert.notNull(engineValves, "Valves must not be null");
    CollectionUtils.addAll(this.engineValves, engineValves);
  }

  /**
   * Returns a mutable collection of the {@link Valve}s that will be applied to the
   * Tomcat {@link Engine}.
   *
   * @return the engine valves that will be applied
   */
  public List<Valve> getEngineValves() {
    return this.engineValves;
  }

  /**
   * Set the character encoding to use for URL decoding. If not specified 'UTF-8' will
   * be used.
   *
   * @param uriEncoding the uri encoding to set
   */
  @Override
  public void setUriEncoding(@Nullable Charset uriEncoding) {
    this.uriEncoding = uriEncoding == null ? DEFAULT_CHARSET : uriEncoding;
  }

  /**
   * Returns the character encoding to use for URL decoding.
   *
   * @return the URI encoding
   */
  public Charset getUriEncoding() {
    return this.uriEncoding;
  }

  /**
   * Set {@link LifecycleListener}s that should be applied to the Tomcat
   * {@link Context}. Calling this method will replace any existing listeners.
   *
   * @param contextLifecycleListeners the listeners to set
   */
  public void setContextLifecycleListeners(Collection<? extends LifecycleListener> contextLifecycleListeners) {
    Assert.notNull(contextLifecycleListeners, "ContextLifecycleListeners must not be null");
    this.contextLifecycleListeners = new ArrayList<>(contextLifecycleListeners);
  }

  /**
   * Returns a mutable collection of the {@link LifecycleListener}s that will be applied
   * to the Tomcat {@link Context}.
   *
   * @return the context lifecycle listeners that will be applied
   */
  public Collection<LifecycleListener> getContextLifecycleListeners() {
    return this.contextLifecycleListeners;
  }

  /**
   * Add {@link LifecycleListener}s that should be added to the Tomcat {@link Context}.
   *
   * @param contextLifecycleListeners the listeners to add
   */
  public void addContextLifecycleListeners(LifecycleListener... contextLifecycleListeners) {
    Assert.notNull(contextLifecycleListeners, "ContextLifecycleListeners must not be null");
    CollectionUtils.addAll(this.contextLifecycleListeners, contextLifecycleListeners);
  }

  /**
   * Factory method called to create the {@link TomcatWebServer}. Subclasses can
   * override this method to return a different {@link TomcatWebServer} or apply
   * additional processing to the Tomcat server.
   *
   * @param tomcat the Tomcat server.
   * @return a new {@link TomcatWebServer} instance
   */
  protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
    return new TomcatWebServer(tomcat, getPort() >= 0, getShutdown());
  }

  /**
   * The Tomcat protocol to use when create the {@link Connector}.
   *
   * @param protocol the protocol
   * @see Connector#Connector(String)
   */
  public void setProtocol(String protocol) {
    Assert.hasLength(protocol, "Protocol must not be empty");
    this.protocol = protocol;
  }

  /**
   * Set whether the factory should disable Tomcat's MBean registry prior to creating
   * the server.
   *
   * @param disableMBeanRegistry whether to disable the MBean registry
   */
  public void setDisableMBeanRegistry(boolean disableMBeanRegistry) {
    this.disableMBeanRegistry = disableMBeanRegistry;
  }

}
