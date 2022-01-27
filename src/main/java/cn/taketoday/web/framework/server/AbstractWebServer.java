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

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.config.WebApplicationLoader;
import cn.taketoday.web.framework.WebServerApplicationContext;
import cn.taketoday.web.framework.config.CompositeWebApplicationConfiguration;
import cn.taketoday.web.framework.config.CompressionConfiguration;
import cn.taketoday.web.framework.config.ErrorPage;
import cn.taketoday.web.framework.config.MimeMappings;
import cn.taketoday.web.framework.config.WebApplicationConfiguration;
import cn.taketoday.web.framework.config.WebDocumentConfiguration;
import cn.taketoday.web.session.SessionConfiguration;

/**
 * @author TODAY 2019-01-26 11:08
 */
public abstract class AbstractWebServer
        extends WebApplicationContextSupport implements ConfigurableWebServer {

  private int port = 8080;
  private String host = "localhost";
  private String contextPath = Constant.BLANK;
  private String serverHeader = null;
  private boolean enableHttp2 = false;

  private String displayName = "Web-App";

  private String deployName = "deploy-web-app";

  @Nullable
  private SessionConfiguration sessionConfig;

  @Nullable
  private CompressionConfiguration compression;

  private Set<String> welcomePages = new LinkedHashSet<>();
  private Set<ErrorPage> errorPages = new LinkedHashSet<>();
  private List<WebApplicationInitializer> contextInitializers = new ArrayList<>();
  private final MimeMappings mimeMappings = new MimeMappings(MimeMappings.DEFAULT);

  @Nullable
  private WebDocumentConfiguration webDocumentConfiguration;
  private AtomicBoolean started = new AtomicBoolean(false);
  private WebApplicationConfiguration webApplicationConfiguration;

  @Autowired
  private Application application;

  @Override
  public void initialize(WebApplicationInitializer... contextInitializers) {
    log.info("Initializing web server: {}", this);

    if (ObjectUtils.isNotEmpty(contextInitializers)) {
      Collections.addAll(this.contextInitializers, contextInitializers);
    }

    // prepare initialize
    prepareInitialize();
    // initialize server context
    initializeContext();
    // context initialized
    contextInitialized();
    // finish initialized
    finishInitialize();
  }

  /**
   * Context Initialized
   */
  protected void contextInitialized() {
    log.info("Server context initialized");
  }

  /**
   * Finish initialized
   */
  protected void finishInitialize() {
    log.info("Finish initialize web server");
  }

  /**
   * Before {@link WebApplicationLoader} Startup
   */
  protected List<WebApplicationInitializer> getMergedInitializers() {
    AnnotationAwareOrderComparator.sort(contextInitializers);
    return contextInitializers;
  }

  protected void prepareInitialize() {
    log.info("Prepare initialize web server");

    WebServerApplicationContext context = obtainApplicationContext();
    Environment environment = context.getEnvironment();
    TodayStrategies.setProperty(WebApplicationLoader.ENABLE_WEB_STARTED_LOG, Boolean.FALSE.toString());

    String webMvcConfigLocation = environment.getProperty(WebApplicationLoader.WEB_MVC_CONFIG_LOCATION);
    if (StringUtils.isNotEmpty(webMvcConfigLocation)) {
      TodayStrategies.setProperty(WebApplicationLoader.ENABLE_WEB_MVC_XML, Boolean.TRUE.toString());
    }
  }

  /**
   * Prepare {@link ApplicationContext}
   */
  protected void initializeContext() {
    log.info("Initialize server context");
  }

  protected boolean isZeroOrLess(Duration sessionTimeout) {
    return sessionTimeout == null || sessionTimeout.isNegative() || sessionTimeout.isZero();
  }

  public WebApplicationConfiguration getWebApplicationConfiguration() {
    WebApplicationConfiguration webApplicationConfiguration = this.webApplicationConfiguration;
    if (webApplicationConfiguration == null) {
      List<WebApplicationConfiguration> configurations =
              obtainApplicationContext().getBeans(WebApplicationConfiguration.class);

      AnnotationAwareOrderComparator.sort(configurations);
      webApplicationConfiguration = new CompositeWebApplicationConfiguration(configurations);
      this.webApplicationConfiguration = webApplicationConfiguration;
    }
    return webApplicationConfiguration;
  }

  /**
   * Get base temporal directory
   *
   * @return base temporal directory
   */
  protected File getTemporalDirectory() {
    return getTemporalDirectory(null);
  }

  /**
   * Get a temporal directory with sub directory
   *
   * @return temporal directory with sub directory
   */
  protected File getTemporalDirectory(String dir) {
    Class<?> mainApplicationClass = getMainApplicationClass();
    return ApplicationUtils.getTemporalDirectory(mainApplicationClass, dir);
  }

  @Nullable
  protected Class<?> getMainApplicationClass() {
    return application.getMainApplicationClass();
  }

  @Override
  public WebServerApplicationContext obtainApplicationContext() {
    return (WebServerApplicationContext) super.obtainApplicationContext();
  }

  /**
   * get session store directory
   */
  public File getStoreDirectory(@Nullable Class<?> startupClass) throws IOException {
    Assert.state(sessionConfig != null, "Please enable web session");
    Resource storeDirectory = sessionConfig.getStoreDirectory();
    if (storeDirectory == null || !storeDirectory.exists()) {
      return ApplicationUtils.getTemporalDirectory(startupClass, "web-app-sessions");
    }

    if (storeDirectory.isDirectory()) {
      LoggerFactory.getLogger(getClass())
              .info("Use directory: [{}] to store sessions", storeDirectory);
      return storeDirectory.getFile();
    }
    throw new ConfigurationException("Store directory must be a 'directory'");
  }

  //

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  public String getServerHeader() {
    return serverHeader;
  }

  public void setServerHeader(String serverHeader) {
    this.serverHeader = serverHeader;
  }

  public boolean isEnableHttp2() {
    return enableHttp2;
  }

  public void setEnableHttp2(boolean enableHttp2) {
    this.enableHttp2 = enableHttp2;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDeployName() {
    return deployName;
  }

  public void setDeployName(String deployName) {
    this.deployName = deployName;
  }

  @Nullable
  public SessionConfiguration getSessionConfig() {
    return sessionConfig;
  }

  @Autowired(required = false)
  public void setSessionConfig(@Nullable SessionConfiguration sessionConfig) {
    this.sessionConfig = sessionConfig;
  }

  @Nullable
  public CompressionConfiguration getCompression() {
    return compression;
  }

  @Autowired(required = false)
  public void setCompression(@Nullable CompressionConfiguration compression) {
    this.compression = compression;
  }

  public Set<ErrorPage> getErrorPages() {
    return errorPages;
  }

  public void setErrorPages(Set<ErrorPage> errorPages) {
    this.errorPages = errorPages;
  }

  public Set<String> getWelcomePages() {
    return welcomePages;
  }

  public void setWelcomePages(Set<String> welcomePages) {
    this.welcomePages = welcomePages;
  }

  public List<WebApplicationInitializer> getContextInitializers() {
    return contextInitializers;
  }

  public void setContextInitializers(List<WebApplicationInitializer> contextInitializers) {
    this.contextInitializers = contextInitializers;
  }

  public MimeMappings getMimeMappings() {
    return mimeMappings;
  }

  @Nullable
  public WebDocumentConfiguration getWebDocumentConfiguration() {
    return webDocumentConfiguration;
  }

  @Autowired(required = false)
  public void setWebDocumentConfiguration(@Nullable WebDocumentConfiguration webDocumentConfiguration) {
    this.webDocumentConfiguration = webDocumentConfiguration;
  }

  public AtomicBoolean getStarted() {
    return started;
  }

  public void setStarted(AtomicBoolean started) {
    this.started = started;
  }

  public void setWebApplicationConfiguration(WebApplicationConfiguration webApplicationConfiguration) {
    this.webApplicationConfiguration = webApplicationConfiguration;
  }
}
