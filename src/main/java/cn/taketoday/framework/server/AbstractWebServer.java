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

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.beans.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableEnvironment;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.AnnotationUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.OrderUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.framework.WebServerApplicationContext;
import cn.taketoday.framework.config.Starter;
import cn.taketoday.framework.config.CompositeWebApplicationConfiguration;
import cn.taketoday.framework.config.CompressionConfiguration;
import cn.taketoday.framework.config.ErrorPage;
import cn.taketoday.framework.config.MimeMappings;
import cn.taketoday.framework.config.WebApplicationConfiguration;
import cn.taketoday.framework.config.WebDocumentConfiguration;
import cn.taketoday.framework.utils.WebApplicationUtils;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.WebConstant;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.config.WebApplicationLoader;
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

  @Autowired(required = false)
  private SessionConfiguration sessionConfig;

  @Autowired(required = false)
  private CompressionConfiguration compression;
  private Set<String> welcomePages = new LinkedHashSet<>();
  private Set<ErrorPage> errorPages = new LinkedHashSet<>();
  private List<WebApplicationInitializer> contextInitializers = new ArrayList<>();
  private final MimeMappings mimeMappings = new MimeMappings(MimeMappings.DEFAULT);

  @Autowired(required = false)
  private WebDocumentConfiguration webDocumentConfiguration;
  private AtomicBoolean started = new AtomicBoolean(false);
  private WebApplicationConfiguration webApplicationConfiguration;

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
    return OrderUtils.reversedSort(contextInitializers);
  }

  protected void prepareInitialize() {
    log.info("Prepare initialize web server");

    final WebServerApplicationContext context = obtainApplicationContext();
    if (context.getEnvironment() instanceof ConfigurableEnvironment) {
      final Starter starter;
      final ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();
      environment.setProperty(WebConstant.ENABLE_WEB_STARTED_LOG, Boolean.FALSE.toString());
      String webMvcConfigLocation = environment.getProperty(WebConstant.WEB_MVC_CONFIG_LOCATION);
      if (StringUtils.isNotEmpty(webMvcConfigLocation)) {
        environment.setProperty(WebConstant.ENABLE_WEB_MVC_XML, Boolean.TRUE.toString());
      }
      else if ((starter = AnnotationUtils.getAnnotation(Starter.class, context.getStartupClass())) != null) {
        // find webMvcConfigLocation
        webMvcConfigLocation = starter.webMvcConfigLocation();
        if (StringUtils.isNotEmpty(webMvcConfigLocation)) {
          environment.setProperty(WebConstant.ENABLE_WEB_MVC_XML, Boolean.TRUE.toString());
          environment.setProperty(WebConstant.WEB_MVC_CONFIG_LOCATION, webMvcConfigLocation);
        }
      }
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
      final List<WebApplicationConfiguration> configurations =
              obtainApplicationContext().getBeans(WebApplicationConfiguration.class);

      OrderUtils.reversedSort(configurations);
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
    return WebApplicationUtils.getTemporalDirectory(obtainApplicationContext().getStartupClass(), dir);
  }

  @Override
  public WebServerApplicationContext obtainApplicationContext() {
    return (WebServerApplicationContext) super.obtainApplicationContext();
  }

  /**
   * get session store directory
   */
  public File getStoreDirectory(Class<?> startupClass) throws IOException {
    Assert.state(sessionConfig != null, "Please enable web session");
    final Resource storeDirectory = sessionConfig.getStoreDirectory();
    if (storeDirectory == null || !storeDirectory.exists()) {
      return WebApplicationUtils.getTemporalDirectory(startupClass, "web-app-sessions");
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

  public SessionConfiguration getSessionConfig() {
    return sessionConfig;
  }

  public void setSessionConfig(SessionConfiguration sessionConfig) {
    this.sessionConfig = sessionConfig;
  }

  public CompressionConfiguration getCompression() {
    return compression;
  }

  public void setCompression(CompressionConfiguration compression) {
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

  public WebDocumentConfiguration getWebDocumentConfiguration() {
    return webDocumentConfiguration;
  }

  public void setWebDocumentConfiguration(WebDocumentConfiguration webDocumentConfiguration) {
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
