/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.web.context;

import java.util.Set;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.availability.AvailabilityChangeEvent;
import cn.taketoday.framework.availability.ReadinessState;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.server.WebServerFactory;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContextUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/3 16:52
 */
public class GenericWebServerApplicationContext
        extends GenericApplicationContext implements ConfigurableWebServerApplicationContext {

  @Nullable
  private String serverNamespace;

  @Nullable
  private volatile WebServer webServer;

  /**
   * Create a new {@link GenericWebServerApplicationContext}.
   */
  public GenericWebServerApplicationContext() { }

  /**
   * Create a new {@link GenericWebServerApplicationContext} with the given
   * {@code StandardBeanFactory}.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   */
  public GenericWebServerApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  @Override
  public final void refresh() throws BeansException, IllegalStateException {
    try {
      super.refresh();
    }
    catch (RuntimeException ex) {
      WebServer webServer = this.webServer;
      if (webServer != null) {
        webServer.stop();
      }
      throw ex;
    }
  }

  @Override
  protected void onRefresh() {
    super.onRefresh();
    try {
      if (webServer == null) {
        this.webServer = createWebServer();
      }
    }
    catch (Throwable ex) {
      throw new ApplicationContextException("Unable to start web server", ex);
    }
  }

  protected WebServer createWebServer() {
    WebServerFactory factory = getWebServerFactory();
    WebServer webServer = factory.getWebServer();

    StandardBeanFactory beanFactory = getBeanFactory();
    beanFactory.registerSingleton("webServerStartStop", new WebServerStartStopLifecycle(this, webServer));
    beanFactory.registerSingleton("webServerGracefulShutdown", new WebServerGracefulShutdownLifecycle(webServer));

    return webServer;
  }

  /**
   * Returns the {@link ServletWebServerFactory} that should be used to create the
   * embedded {@link WebServer}. By default this method searches for a suitable bean in
   * the context itself.
   *
   * @return a {@link ServletWebServerFactory} (never {@code null})
   */
  private WebServerFactory getWebServerFactory() {
    // Use bean names so that we don't consider the hierarchy
    StandardBeanFactory beanFactory = getBeanFactory();
    Set<String> beanNames = beanFactory.getBeanNamesForType(WebServerFactory.class);
    if (beanNames.size() == 0) {
      throw new MissingWebServerFactoryBeanException(
              getClass(), WebServerFactory.class, ApplicationType.SERVLET_WEB);
    }
    if (beanNames.size() > 1) {
      throw new ApplicationContextException("Unable to start WebServerApplicationContext due to multiple "
              + "WebServerFactory beans : " + StringUtils.collectionToCommaDelimitedString(beanNames));
    }
    return beanFactory.getBean(CollectionUtils.firstElement(beanNames), WebServerFactory.class);
  }

  @Override
  protected void doClose() {
    if (isActive()) {
      AvailabilityChangeEvent.publish(this, ReadinessState.REFUSING_TRAFFIC);
    }
    super.doClose();
  }

  /**
   * Returns the {@link WebServer} that was created by the context or {@code null} if
   * the server has not yet been created.
   *
   * @return the embedded web server
   */
  @Nullable
  @Override
  public WebServer getWebServer() {
    return this.webServer;
  }

  @Override
  public void setServerNamespace(@Nullable String serverNamespace) {
    this.serverNamespace = serverNamespace;
  }

  @Nullable
  @Override
  public String getServerNamespace() {
    return serverNamespace;
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    RequestContextUtils.registerScopes(beanFactory);
  }

}
