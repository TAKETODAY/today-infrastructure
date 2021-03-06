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
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.framework.Constant;
import cn.taketoday.framework.WebServerApplicationContext;
import cn.taketoday.framework.annotation.Starter;
import cn.taketoday.framework.bean.ErrorPage;
import cn.taketoday.framework.bean.MimeMappings;
import cn.taketoday.framework.config.CompositeWebApplicationConfiguration;
import cn.taketoday.framework.config.CompressionConfiguration;
import cn.taketoday.framework.config.WebApplicationConfiguration;
import cn.taketoday.framework.config.WebDocumentConfiguration;
import cn.taketoday.framework.utils.ApplicationUtils;
import cn.taketoday.web.config.WebApplicationInitializer;
import cn.taketoday.web.config.WebApplicationLoader;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-01-26 11:08
 */
@Getter
@Setter
public abstract class AbstractWebServer implements ConfigurableWebServer {

  private int port = 8080;
  private String host = "localhost";
  private String contextPath = Constant.BLANK;
  private String serverHeader = null;
  private boolean enableHttp2 = false;

  private String displayName = "Web-App";

  private String deployName = "deploy-web-app";

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

    if (contextInitializers != null) {
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
    final WebServerApplicationContext context = getApplicationContext();
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
    return ApplicationUtils.getTemporalDirectory(getApplicationContext().getStartupClass(), dir);
  }

  protected abstract WebServerApplicationContext getApplicationContext();
}
