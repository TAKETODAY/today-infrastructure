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
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.WebServerApplicationContext;
import cn.taketoday.framework.annotation.Starter;
import cn.taketoday.framework.config.ErrorPage;
import cn.taketoday.framework.config.MimeMappings;
import cn.taketoday.framework.config.CompositeWebApplicationConfiguration;
import cn.taketoday.framework.config.CompressionConfiguration;
import cn.taketoday.framework.config.SessionConfiguration;
import cn.taketoday.framework.config.WebApplicationConfiguration;
import cn.taketoday.framework.config.WebDocumentConfiguration;
import cn.taketoday.framework.utils.ApplicationUtils;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.config.WebApplicationLoader;

/**
 * @author TODAY <br>
 * 2019-01-26 11:08
 */
public abstract class AbstractWebServer
        extends WebApplicationContextSupport implements ConfigurableWebServer {

  private int port = 8080;
  private String host = "localhost";
  protected String contextPath = Constant.BLANK;
  private String serverHeader = null;
  private boolean enableHttp2 = false;

  private String displayName = "Web-App";

  private String deployName = "deploy-web-app";

  @Autowired(required = false)
  private SessionConfiguration sessionConfiguration;

  @Autowired(required = false)
  private CompressionConfiguration compression;
  private LinkedHashSet<ErrorPage> errorPages = new LinkedHashSet<>();
  private LinkedHashSet<String> welcomePages = new LinkedHashSet<>();
  private LinkedList<WebApplicationInitializer> contextInitializers = new LinkedList<>();
  private final MimeMappings mimeMappings = new MimeMappings(MimeMappings.DEFAULT);

  @Autowired(required = false)
  private WebDocumentConfiguration webDocumentConfiguration;
  private AtomicBoolean started = new AtomicBoolean(false);
  private WebApplicationConfiguration webApplicationConfiguration;

  @Override
  public void initialize(WebApplicationInitializer... contextInitializers) {

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
  protected void contextInitialized() {}

  /**
   * Finish initialized
   */
  protected void finishInitialize() {}

  /**
   * Before {@link WebApplicationLoader} Startup
   */
  protected List<WebApplicationInitializer> getMergedInitializers() {
    return OrderUtils.reversedSort(contextInitializers);
  }

  protected void prepareInitialize() {
    final WebServerApplicationContext context = obtainApplicationContext();
    if (context.getEnvironment() instanceof ConfigurableEnvironment) {
      final Starter starter;
      final ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();
      environment.setProperty(Constant.ENABLE_WEB_STARTED_LOG, Boolean.FALSE.toString());
      String webMvcConfigLocation = environment.getProperty(Constant.WEB_MVC_CONFIG_LOCATION);
      if (StringUtils.isNotEmpty(webMvcConfigLocation)) {
        environment.setProperty(Constant.ENABLE_WEB_MVC_XML, Boolean.TRUE.toString());
      }
      else if ((starter = ClassUtils.getAnnotation(Starter.class, context.getStartupClass())) != null) {
        // find webMvcConfigLocation
        webMvcConfigLocation = starter.webMvcConfigLocation();
        if (StringUtils.isNotEmpty(webMvcConfigLocation)) {
          environment.setProperty(Constant.ENABLE_WEB_MVC_XML, Boolean.TRUE.toString());
          environment.setProperty(Constant.WEB_MVC_CONFIG_LOCATION, webMvcConfigLocation);
        }
      }
    }
  }

  /**
   * Prepare {@link ApplicationContext}
   */
  protected void initializeContext() {}

  protected boolean isZeroOrLess(Duration sessionTimeout) {
    return sessionTimeout == null || sessionTimeout.isNegative() || sessionTimeout.isZero();
  }

  public WebApplicationConfiguration getWebApplicationConfiguration() {

    if (webApplicationConfiguration == null) {
      final List<WebApplicationConfiguration> configurations = //
              getApplicationContext().getBeans(WebApplicationConfiguration.class);

      OrderUtils.reversedSort(configurations);
      return webApplicationConfiguration = new CompositeWebApplicationConfiguration(configurations);
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
    return ApplicationUtils.getTemporalDirectory(obtainApplicationContext().getStartupClass(), dir);
  }

  @Override
  public WebServerApplicationContext obtainApplicationContext() {
    return (WebServerApplicationContext) super.obtainApplicationContext();
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

  public SessionConfiguration getSessionConfiguration() {
    return sessionConfiguration;
  }

  public void setSessionConfiguration(SessionConfiguration sessionConfiguration) {
    this.sessionConfiguration = sessionConfiguration;
  }

  public CompressionConfiguration getCompression() {
    return compression;
  }

  public void setCompression(CompressionConfiguration compression) {
    this.compression = compression;
  }

  public LinkedHashSet<ErrorPage> getErrorPages() {
    return errorPages;
  }

  public void setErrorPages(LinkedHashSet<ErrorPage> errorPages) {
    this.errorPages = errorPages;
  }

  public LinkedHashSet<String> getWelcomePages() {
    return welcomePages;
  }

  public void setWelcomePages(LinkedHashSet<String> welcomePages) {
    this.welcomePages = welcomePages;
  }

  public LinkedList<WebApplicationInitializer> getContextInitializers() {
    return contextInitializers;
  }

  public void setContextInitializers(LinkedList<WebApplicationInitializer> contextInitializers) {
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
