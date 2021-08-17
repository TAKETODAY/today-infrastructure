/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.framework.server;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import cn.taketoday.beans.Autowired;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.core.io.JarResource;
import cn.taketoday.core.utils.ClassUtils;
import cn.taketoday.core.utils.StringUtils;
import cn.taketoday.framework.config.CompressionConfiguration;
import cn.taketoday.framework.config.ErrorPage;
import cn.taketoday.framework.config.MimeMappings;
import cn.taketoday.framework.config.WebDocumentConfiguration;
import cn.taketoday.web.session.SessionConfiguration;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.MimeMapping;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.util.ImmediateInstanceFactory;

/**
 * Undertow Servlet web server
 *
 * @author TODAY 2019-01-12 17:28
 */
public class UndertowServer
        extends AbstractServletWebServer implements WebServer {

  private boolean useForwardHeaders;

  private Undertow undertow;

  private Builder builder;

  private DeploymentManager manager;

  private boolean eagerInitFilters;

  private int bufferSize;

  private int ioThreads;

  private int workerThreads;

  private Boolean directBuffers;

  private ServletStackTraces stackTraces = ServletStackTraces.NONE;

  @Autowired(required = false)
  private SessionManager sessionManager;

  @Override
  public synchronized void start() {
    log.info("Start Undertow web server");
    try {
      if (getStarted().get()) {
        return;
      }
      if (this.undertow == null) {
        this.undertow = createUndertowServer();
      }
      undertow.start();
      getStarted().set(true);
    }
    catch (Exception e) {
      e.printStackTrace();
      stopSilently();
    }
  }

  protected Undertow createUndertowServer() throws ServletException {
    HttpHandler httpHandler = getContextHandler(this.manager.start());
    if (this.useForwardHeaders) {
      httpHandler = Handlers.proxyPeerAddress(httpHandler);
    }
    if (StringUtils.isNotEmpty(getServerHeader())) {
      httpHandler = Handlers.header(httpHandler, "Server", getServerHeader());
    }
    return this.builder.setHandler(httpHandler).build();
  }

  protected HttpHandler getContextHandler(HttpHandler httpHandler) {
    HttpHandler contextHandler = httpHandler;
    final CompressionConfiguration compression = getCompression();
    if (compression != null) {
      getWebApplicationConfiguration().configureCompression(compression);
      if (compression.isEnable()) {
        log.info("Compression enabled");
        contextHandler = UndertowCompressionUtils.configureCompression(compression, httpHandler);
      }
    }

    if (StringUtils.isEmpty(getContextPath())) {
      return contextHandler;
    }
    return Handlers.path().addPrefixPath(getContextPath(), contextHandler);
  }

  @Override
  @PreDestroy
  public synchronized void stop() {
    if (!getStarted().get()) {
      return;
    }
    log.info("Stop Undertow web server");
    manager.undeploy();
    try {
      manager.stop();
    }
    catch (ServletException e) {
      stopSilently();
    }
    getStarted().set(false);
  }

  private void stopSilently() {
    try {
      if (this.undertow != null) {
        this.undertow.stop();
      }
    }
    catch (Exception ex) {
      // Ignore
    }
  }

  @Override
  protected void initializeContext() {
    super.initializeContext();
    log.info("Initialize Undertow Web Server Context");

    manager = createDeploymentManager();
    builder = createBuilder(getPort());
  }

  protected DeploymentManager createDeploymentManager() {
    final DeploymentInfo deployment = Servlets.deployment();
    final ServletWebServerApplicationLoader starter = //
            new ServletWebServerApplicationLoader(obtainApplicationContext(), this::getMergedInitializers);
    // 添加 ApplicationLoader
    deployment.addServletContainerInitializer(
            new ServletContainerInitializerInfo(ServletWebServerApplicationLoader.class,
                                                new ImmediateInstanceFactory<>(starter),
                                                Collections.emptySet()
            )
    );

    deployment.setClassLoader(getClassLoader());
    deployment.setContextPath(getContextPath());
    deployment.setDisplayName(getDisplayName());

    final String defaultEncoding = getDefaultEncoding();
    deployment.setDefaultEncoding(defaultEncoding);
    deployment.setDefaultRequestEncoding(defaultEncoding);

    deployment.setDeploymentName(getDeployName());

    deployment.setServletStackTraces(stackTraces);

    final ResourceManager documentRootResourceManager = getDocumentRootResourceManager();
    if (documentRootResourceManager != null) {
      deployment.setResourceManager(documentRootResourceManager);
    }
    deployment.setEagerFilterInit(this.eagerInitFilters);

    configureErrorPages(deployment);
    configureMimeMapping(deployment);
    configureWelcomePages(deployment);
    configureLocaleMappings(deployment);

    DeploymentManager manager = Servlets.newContainer().addDeployment(deployment);

    SessionManager sessionManager = this.sessionManager;
    if (sessionManager != null) {
      deployment.setSessionManagerFactory(e -> this.sessionManager);
      manager.deploy();
    }
    else {
      manager.deploy();
      final SessionConfiguration sessionConfig = getSessionConfig();
      if (sessionConfig != null && sessionConfig.isEnableHttpSession()) { // enable http session
        // not support persistent
        sessionManager = manager.getDeployment().getSessionManager();
        // http session timeout settings
        Duration timeoutDuration = sessionConfig.getTimeout();
        int sessionTimeout = (isZeroOrLess(timeoutDuration) ? -1 : (int) timeoutDuration.getSeconds());
        sessionManager.setDefaultSessionTimeout(sessionTimeout);
      }
    }
    return manager;
  }

  protected String getDefaultEncoding() {
    return Constant.DEFAULT_ENCODING;
  }

  protected ClassLoader getClassLoader() {
    return ClassUtils.getClassLoader();
  }

  protected void configureLocaleMappings(DeploymentInfo deployment) {
    for (Entry<Locale, Charset> entry : getLocaleCharsetMappings().entrySet()) {
      deployment.addLocaleCharsetMapping(entry.getKey().toString(), entry.getValue().toString());
    }
  }

  protected void configureWelcomePages(DeploymentInfo deployment) {
    final Set<String> welcomePages = getWelcomePages();
    getWebApplicationConfiguration().configureWelcomePages(welcomePages);

    deployment.addWelcomePages(welcomePages);
  }

  protected void configureMimeMapping(DeploymentInfo deployment) {
    final MimeMappings mimeMappings = getMimeMappings();
    getWebApplicationConfiguration().configureMimeMappings(mimeMappings);
    for (MimeMappings.Mapping mapping : mimeMappings) {
      deployment.addMimeMapping(new MimeMapping(mapping.getExtension(), mapping.getMimeType()));
    }
  }

  protected void configureErrorPages(DeploymentInfo deployment) {
    final Set<ErrorPage> errorPages = getErrorPages();
    // config error pages
    getWebApplicationConfiguration().configureErrorPages(errorPages);
    for (ErrorPage errorPage : errorPages) {
      if (errorPage.getStatus() != 0) {
        deployment.addErrorPage(Servlets.errorPage(errorPage.getPath(), errorPage.getStatus()));
      }
      else if (errorPage.getException() != null) {
        deployment.addErrorPage(Servlets.errorPage(errorPage.getPath(), errorPage.getException()));
      }
      else {
        deployment.addErrorPage(Servlets.errorPage(errorPage.getPath()));
      }
    }
  }

  protected Builder createBuilder(int port) {
    final Builder builder = Undertow.builder();
    if (this.bufferSize != 0) {
      builder.setBufferSize(this.bufferSize);
    }
    if (this.ioThreads != 0) {
      builder.setIoThreads(this.ioThreads);
    }
    if (this.workerThreads != 0) {
      builder.setWorkerThreads(this.workerThreads);
    }
    if (this.directBuffers != null) {
      builder.setDirectBuffers(this.directBuffers);
    }

    return builder.addHttpListener(port, getHost());
  }

  protected ResourceManager getDocumentRootResourceManager() {
    final WebDocumentConfiguration webDocumentConfiguration = getWebDocumentConfiguration();
    try {
      return getRootResource(webDocumentConfiguration == null ? null : webDocumentConfiguration.getValidDocumentDirectory());
    }
    catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  protected ResourceManager getRootResource(final cn.taketoday.core.io.Resource rootDirectory) throws IOException {
    if (rootDirectory instanceof JarResource) {
      return new JarResourceManager((JarResource) rootDirectory);
    }
    if (rootDirectory instanceof FileBasedResource) {
      return new FileResourceManager(rootDirectory.getFile());
    }
    if (rootDirectory instanceof ClassPathResource) {
      return getRootResource(((ClassPathResource) rootDirectory).getOriginalResource());
    }

    return new FileResourceManager(getTemporalDirectory("undertow-docbase"));
  }

  @Override
  protected Servlet createDefaultServlet() {
    return new DefaultServlet();
  }

  private static class JarResourceManager implements ResourceManager {
    private final String jarFilePath;

    JarResourceManager(JarResource rootDirectory) throws IOException {
      this.jarFilePath = StringUtils.cleanPath(rootDirectory.getFile().getAbsolutePath());
    }

    @Override
    public Resource getResource(String path) throws IOException {
      URL url = new URL("jar:file:" + jarFilePath + '!' + (path.startsWith("/") ? path : '/' + path));

      URLResource resource = new URLResource(url, path);
      if (resource.getContentLength() < 0) {
        return null;
      }
      return resource;
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
      return false;
    }

    @Override
    public void registerResourceChangeListener(ResourceChangeListener listener) { }

    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) { }

    @Override
    public void close() { }

  }

  // getter , setter

  public boolean isUseForwardHeaders() {
    return useForwardHeaders;
  }

  public void setUseForwardHeaders(boolean useForwardHeaders) {
    this.useForwardHeaders = useForwardHeaders;
  }

  public Undertow getUndertow() {
    return undertow;
  }

  public void setUndertow(Undertow undertow) {
    this.undertow = undertow;
  }

  public Builder getBuilder() {
    return builder;
  }

  public void setBuilder(Builder builder) {
    this.builder = builder;
  }

  public DeploymentManager getManager() {
    return manager;
  }

  public void setManager(DeploymentManager manager) {
    this.manager = manager;
  }

  public boolean isEagerInitFilters() {
    return eagerInitFilters;
  }

  public void setEagerInitFilters(boolean eagerInitFilters) {
    this.eagerInitFilters = eagerInitFilters;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  public int getIoThreads() {
    return ioThreads;
  }

  public void setIoThreads(int ioThreads) {
    this.ioThreads = ioThreads;
  }

  public int getWorkerThreads() {
    return workerThreads;
  }

  public void setWorkerThreads(int workerThreads) {
    this.workerThreads = workerThreads;
  }

  public Boolean getDirectBuffers() {
    return directBuffers;
  }

  public void setDirectBuffers(Boolean directBuffers) {
    this.directBuffers = directBuffers;
  }

  public ServletStackTraces getStackTraces() {
    return stackTraces;
  }

  public void setStackTraces(ServletStackTraces stackTraces) {
    this.stackTraces = stackTraces;
  }

  public SessionManager getSessionManager() {
    return sessionManager;
  }

  public void setSessionManager(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }
}
