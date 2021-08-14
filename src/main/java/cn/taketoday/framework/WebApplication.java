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
package cn.taketoday.framework;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.framework.server.WebServer;

/**
 * Web Application Runner
 *
 * @author TODAY 2018-10-16 15:46
 */
public class WebApplication {
  private static final Logger log = LoggerFactory.getLogger(WebApplication.class);

  private final ConfigurableWebServerApplicationContext context;
  private final String appBasePath = System.getProperty("user.dir");

  public WebApplication() {
    this(null);
  }

  public WebApplication(Class<?> startupClass, String... args) {
    context = ClassUtils.isPresent(Constant.ENV_SERVLET)
              ? new ServletWebServerApplicationContext(startupClass, args)
              : new StandardWebServerApplicationContext(startupClass, args);
  }

  public WebApplication(ConfigurableWebServerApplicationContext context) {
    this.context = context;
  }

  public ConfigurableWebServerApplicationContext getApplicationContext() {
    return context;
  }

  /**
   * Startup Web Application
   *
   * @param startupClass
   *         Startup class
   * @param args
   *         Startup arguments
   */
  public static ConfigurableWebServerApplicationContext run(Class<?> startupClass, String... args) {
    return new WebApplication(startupClass, args).run(args);
  }

  /**
   * Startup Reactive Web Application
   *
   * @param startupClass
   *         Startup class
   * @param args
   *         Startup arguments
   */
  public static ConfigurableWebServerApplicationContext runReactive(Class<?> startupClass, String... args) {
    return new WebApplication(new StandardWebServerApplicationContext(startupClass, args)).run(args);
  }

  /**
   * Startup Web Application
   *
   * @param args
   *         Startup arguments
   *
   * @return {@link ConfigurableWebServerApplicationContext}
   */
  public ConfigurableWebServerApplicationContext run(String... args) {
    log.info("Starting Web Application at [{}]", getAppBasePath());

    final ConfigurableWebServerApplicationContext context = getApplicationContext();
    try {
      context.registerSingleton(this);
      final Class<?> startupClass = context.getStartupClass();
      context.importBeans(startupClass); // @since 1.0.2 import startup class
      if (startupClass == null) {
        log.info("There isn't a Startup Class");
        context.load(); // load from all classpath
      }
      else {
        context.load(startupClass.getPackage().getName());
      }

      final WebServer webServer = context.getWebServer();
      Assert.state(webServer != null, "No Web server.");
      webServer.start();

      log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
               System.currentTimeMillis() - context.getStartupDate()//
      );
      return context;
    }
    catch (Throwable e) {
      e = ExceptionUtils.unwrapThrowable(e);
      context.close();
      throw new ConfigurationException("Your Application Initialized ERROR: [" + e + "]", e);
    }
  }

  public String getAppBasePath() {
    return appBasePath;
  }

}
