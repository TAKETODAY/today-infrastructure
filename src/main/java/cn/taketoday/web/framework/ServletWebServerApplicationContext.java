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

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.framework.server.WebServerLifecycle;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.framework.server.WebServer;
import cn.taketoday.web.servlet.StandardWebServletApplicationContext;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;

/**
 * {@link Servlet} based Web {@link ApplicationContext}
 *
 * @author TODAY <br>
 * 2019-01-17 15:54
 */
public class ServletWebServerApplicationContext
        extends StandardWebServletApplicationContext implements WebServerApplicationContext {

  private WebServer webServer;

  public ServletWebServerApplicationContext() { }

  public ServletWebServerApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  public ServletWebServerApplicationContext(@Nullable ApplicationContext parent) {
    super(parent);
  }

  public ServletWebServerApplicationContext(Class<?>... components) {
    super(components);
  }

  public ServletWebServerApplicationContext(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void onRefresh() {
    super.onRefresh();
    try {
      createWebServer();
    }
    catch (Throwable ex) {
      throw new ApplicationContextException("Unable to start web server", ex);
    }
  }

  private void createWebServer() {
    this.webServer = WebApplicationUtils.obtainWebServer(this);
    getBeanFactory().registerSingleton(
            WebServerLifecycle.BEAN_NAME, new WebServerLifecycle(this.webServer));
  }

  @Override
  public WebServer getWebServer() {
    return webServer;
  }

}
