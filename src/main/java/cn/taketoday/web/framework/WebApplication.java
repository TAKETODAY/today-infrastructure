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
package cn.taketoday.web.framework;

import cn.taketoday.beans.factory.SingletonBeanRegistry;
import cn.taketoday.context.AnnotationConfigRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.WebApplicationFailedEvent;
import cn.taketoday.web.framework.server.WebServer;

/**
 * Web Application Runner
 *
 * @author TODAY 2018-10-16 15:46
 */
public class WebApplication {
  private static final Logger log = LoggerFactory.getLogger(WebApplication.class);
  private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

  private boolean headless = true;

  private final WebServerApplicationContext context;
  private final String appBasePath = System.getProperty("user.dir");

  public WebApplication() {
    this(null);
  }

  public WebApplication(Class<?> startupClass, String... args) {
    context = ClassUtils.isPresent(Constant.ENV_SERVLET)
              ? new ServletWebServerApplicationContext(startupClass, args)
              : new StandardWebServerApplicationContext(startupClass, args);
  }

  public WebApplication(WebServerApplicationContext context) {
    this.context = context;
  }

  public WebServerApplicationContext getApplicationContext() {
    return context;
  }

  /**
   * Startup Web Application
   *
   * @param startupClass Startup class
   * @param args Startup arguments
   */
  public static WebServerApplicationContext run(Class<?> startupClass, String... args) {
    return new WebApplication(startupClass, args).run(args);
  }

  /**
   * Startup Reactive Web Application
   *
   * @param startupClass Startup class
   * @param args Startup arguments
   */
  public static WebServerApplicationContext runReactive(Class<?> startupClass, String... args) {
    return new WebApplication(new StandardWebServerApplicationContext(startupClass, args)).run(args);
  }

  /**
   * Startup Web Application
   *
   * @param args Startup arguments
   * @return {@link WebServerApplicationContext}
   */
  public WebServerApplicationContext run(String... args) {
    configureHeadlessProperty();
    log.info("Starting Web Application at [{}]", getAppBasePath());

    WebServerApplicationContext context = getApplicationContext();
    try {
      SingletonBeanRegistry registry = context.unwrapFactory(SingletonBeanRegistry.class);
      registry.registerSingleton(this);

      AnnotationConfigRegistry configRegistry = context.unwrap(AnnotationConfigRegistry.class);

      Class<?> startupClass = context.getStartupClass();
      configRegistry.register(startupClass); // @since 1.0.2 import startup class
      if (startupClass == null) {
        log.info("There isn't a Startup Class");
        configRegistry.scan(); // load from all classpath
      }
      else {
        configRegistry.scan(startupClass.getPackage().getName());
      }

      context.refresh();

      WebServer webServer = context.getWebServer();
      Assert.state(webServer != null, "No Web server.");
      webServer.start();

      log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
              System.currentTimeMillis() - context.getStartupDate()//
      );
      return context;
    }
    catch (Throwable e) {
      context.close();
      try {
        context.publishEvent(new WebApplicationFailedEvent(context, e));
      }
      catch (Throwable ex) {
        log.warn("Exception thrown from publishEvent handling WebApplicationFailedEvent", ex);
      }
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  private void configureHeadlessProperty() {
    System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, System.getProperty(
            SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless)));
  }

  public String getAppBasePath() {
    return appBasePath;
  }

  public void setHeadless(boolean headless) {
    this.headless = headless;
  }

  public boolean isHeadless() {
    return headless;
  }

}
