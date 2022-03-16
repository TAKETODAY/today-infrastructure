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
package cn.taketoday.web.framework;

import cn.taketoday.core.ConfigurationException;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.web.config.WebApplicationLoader;
import cn.taketoday.web.framework.server.AbstractWebServer;
import cn.taketoday.web.framework.server.ConfigurableWebServer;

/**
 * @author TODAY 2019-06-19 20:05
 */
public abstract class WebApplicationUtils {

  /**
   * Obtain a {@link WebServer} form bean-factory
   *
   * @param beanFactory Target bean-factory
   * @return WebServer
   */
  public static WebServer obtainWebServer(WebServerApplicationContext beanFactory) {
    // disable web mvc xml
    TodayStrategies.setProperty(WebApplicationLoader.ENABLE_WEB_MVC_XML, "false");
    // Get WebServer instance
    WebServer webServer = beanFactory.getBean(WebServer.class);
    if (webServer == null) {
      throw new ConfigurationException(
              "The bean factory: [" + beanFactory + "] doesn't exist a [cn.taketoday.web.framework.server.WebServer] bean");
    }
    if (webServer instanceof ConfigurableWebServer) {
      if (webServer instanceof AbstractWebServer) {
        ((AbstractWebServer) webServer).getWebApplicationConfiguration()
                .configureWebServer((AbstractWebServer) webServer);
      }
      ((ConfigurableWebServer) webServer).initialize();
    }
    return webServer;
  }

}
