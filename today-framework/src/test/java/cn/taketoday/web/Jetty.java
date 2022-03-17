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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.IOException;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import cn.taketoday.lang.Constant;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.servlet.WebServletApplicationLoader;
import cn.taketoday.web.servlet.initializer.ServletContextInitializer;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 * 2020-04-28 11:21
 */
@Setter
@Getter
public class Jetty {

  private static final Logger log = LoggerFactory.getLogger(Jetty.class);

  private Server server;
  private int port = 8081;
  private String host = "localhost";
  private String contextPath = Constant.BLANK;
  private boolean autoStart = true;
  private AtomicBoolean started = new AtomicBoolean(false);

  private Connector[] connectors;

  private List<Configuration> configurations = new ArrayList<>();

  /** The number of acceptor threads to use. default value */
  private int acceptors = -1;

  /** The number of selector threads to use. default value */
  private int selectors = -1;

  private ThreadPool threadPool;

  private AnnotationConfigServletWebApplicationContext applicationContext;

  protected AnnotationConfigServletWebApplicationContext getApplicationContext() {
    return applicationContext;
  }

  protected synchronized void contextInitialized() {

    try {

      // Cache the connectors and then remove them to prevent requests being
      // handled before the application context is ready.
      this.connectors = this.server.getConnectors();
      this.server.addBean(new AbstractLifeCycle() {

        @Override
        protected void doStart() throws Exception {
          for (Connector connector : Jetty.this.connectors) {
            state(connector.isStopped(), () -> "Connector " + connector + " has been started prematurely");
          }
          Jetty.this.server.setConnectors(null);
        }

      });
      // Start the server so that the ServletContext is available
      this.server.start();
      this.server.setStopAtShutdown(false);
    }
    catch (Throwable ex) {
      // Ensure process isn't left running
      stopSilently();
      throw new InternalServerException("Unable to start embedded Jetty web server", ex);
    }
  }

  public static void state(boolean expression, Supplier<String> messageSupplier) {
    if (!expression) {
      throw new IllegalStateException(nullSafeGet(messageSupplier));
    }
  }

  private static String nullSafeGet(Supplier<String> messageSupplier) {
    return (messageSupplier != null ? messageSupplier.get() : null);
  }

  //@formatter:off
    private void stopSilently() {
        try {
            this.server.stop();
        }
        catch (Exception ex) {}
    }
    // @formatter:on

  public synchronized void start() {
    try {
      // initialize server context
      initializeContext();
      // context initialized
      contextInitialized();

      if (getStarted().get()) {
        return;
      }
      this.server.setConnectors(this.connectors);
      if (!this.autoStart) {
        return;
      }

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
          return;
        }
      }
      getStarted().set(true);

      log.info("Jetty started on port(s) '{}' with context path '{}'", //
              getActualPortsDescription(), getContextPath());
    }
    catch (Exception ex) {
      stopSilently();
      throw new InternalServerException("Unable to start embedded Jetty server", ex);
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
    return " (" + StringUtils.arrayToCommaDelimitedString(protocols.toArray(new String[protocols.size()])) + ")";
  }

  @PreDestroy
  public synchronized void stop() {

    getStarted().set(false);

    try {
      this.server.stop();
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    catch (Exception ex) {
      throw new InternalServerException("Unable to stop embedded Jetty server", ex);
    }
  }

  public Server getJetty() {
    return this.server;
  }

  protected void initializeContext() throws IOException {

    log.info("Jetty Server initializing with port: {}", getPort());

    final WebAppContext context = new WebAppContext();

    final Server server = new Server(getThreadPool());
    this.server = server;
    server.setConnectors(new Connector[] { getServerConnector(getHost(), getPort(), server) });

    configureWebAppContext(context);
    server.setHandler(context);
  }

  /**
   * Create a sever {@link Connector}
   *
   * @param host server host
   * @param port server port
   * @param server server instance
   * @return a {@link ServerConnector}
   */
  protected ServerConnector getServerConnector(final String host, final int port, final Server server) {
    final ServerConnector connector = new ServerConnector(server, this.acceptors, this.selectors);

    connector.setHost(host);
    connector.setPort(port);
    return connector;
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
  protected void configureWebAppContext(final WebAppContext context) throws IOException {
    Objects.requireNonNull(context, "WebAppContext must not be null");
//        context.setTempDirectory(getTemporalDirectory()); // base temp dir

    final String contextPath = getContextPath();

    // D:\Projects\Git\github\today-web\src\test\resources

    final cn.taketoday.core.io.Resource resource = ResourceUtils.getResource("classpath:jetty-root/");

    context.setBaseResource(getRootResource(resource));

    context.setContextPath(StringUtils.isNotEmpty(contextPath) ? contextPath : "/");
    context.setDisplayName(getDisplayName());

    final Configuration[] configurations = getWebAppContextConfigurations(context);

    context.setConfigurations(configurations);

    context.setThrowUnavailableOnStartupException(true);
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
    throw new IOException();
  }

  private String getDisplayName() {
    return "Today Web Test App";
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
    return configurations.toArray(new Configuration[configurations.size()]);
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
    return new ServletContextInitializerConfiguration();
  }

  /**
   * @author TODAY <br>
   * 2019-10-14 01:07
   */
  public class ServletContextInitializerConfiguration extends AbstractConfiguration {

    private WebServletApplicationLoader starter;

    public ServletContextInitializerConfiguration() {
      this.starter = new WebServletApplicationLoader();
    }

    @Override
    public void configure(WebAppContext context) throws Exception {
      context.addBean(new Initializer(context), true);
    }

    private class Initializer extends AbstractLifeCycle {

      private final WebAppContext context;

      Initializer(WebAppContext context) {
        this.context = context;
      }

      @Override
      protected void doStart() throws Exception {

        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.context.getClassLoader());
        try {
          setExtendedListenerTypes(true);
          final Context servletContext = this.context.getServletContext();
          starter.onStartup(null, servletContext);
          applicationContext = (AnnotationConfigServletWebApplicationContext) starter.getApplicationContext();
        }
        finally {
          setExtendedListenerTypes(false);
          Thread.currentThread().setContextClassLoader(oldClassLoader);
          starter = null;
        }
      }

      private final void setExtendedListenerTypes(boolean extended) {
        this.context.getServletContext().setExtendedListenerTypes(extended);
      }
    }
  }

}
