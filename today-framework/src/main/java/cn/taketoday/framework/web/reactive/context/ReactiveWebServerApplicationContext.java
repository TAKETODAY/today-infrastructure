/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.reactive.context;

import java.util.Set;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.availability.AvailabilityChangeEvent;
import cn.taketoday.framework.availability.ReadinessState;
import cn.taketoday.framework.web.context.ConfigurableWebServerApplicationContext;
import cn.taketoday.framework.web.context.MissingWebServerFactoryBeanException;
import cn.taketoday.framework.web.context.WebServerGracefulShutdownLifecycle;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A {@link GenericReactiveWebApplicationContext} that can be used to bootstrap itself
 * from a contained {@link ReactiveWebServerFactory} bean.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public class ReactiveWebServerApplicationContext extends GenericReactiveWebApplicationContext
        implements ConfigurableWebServerApplicationContext {

  private volatile WebServerManager serverManager;

  private String serverNamespace;

  /**
   * Create a new {@link ReactiveWebServerApplicationContext}.
   */
  public ReactiveWebServerApplicationContext() {
  }

  /**
   * Create a new {@link ReactiveWebServerApplicationContext} with the given
   * {@code StandardBeanFactory}.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   */
  public ReactiveWebServerApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  @Override
  public final void refresh() throws BeansException, IllegalStateException {
    try {
      super.refresh();
    }
    catch (RuntimeException ex) {
      WebServerManager serverManager = this.serverManager;
      if (serverManager != null) {
        serverManager.getWebServer().stop();
      }
      throw ex;
    }
  }

  @Override
  protected void onRefresh() {
    super.onRefresh();
    try {
      createWebServer();
    }
    catch (Throwable ex) {
      throw new ApplicationContextException("Unable to start reactive web server", ex);
    }
  }

  private void createWebServer() {
    WebServerManager serverManager = this.serverManager;
    if (serverManager == null) {
      String webServerFactoryBeanName = getWebServerFactoryBeanName();
      ReactiveWebServerFactory webServerFactory = getWebServerFactory(webServerFactoryBeanName);
      StandardBeanFactory beanFactory = getBeanFactory();
      boolean lazyInit = beanFactory.getBeanDefinition(webServerFactoryBeanName).isLazyInit();

      this.serverManager = new WebServerManager(this, webServerFactory, this::getHttpHandler, lazyInit);
      beanFactory.registerSingleton("webServerGracefulShutdown",
              new WebServerGracefulShutdownLifecycle(this.serverManager.getWebServer()));
      beanFactory.registerSingleton("webServerStartStop",
              new WebServerStartStopLifecycle(this.serverManager));
    }
    initPropertySources();
  }

  protected String getWebServerFactoryBeanName() {
    // Use bean names so that we don't consider the hierarchy
    Set<String> beanNames = getBeanFactory().getBeanNamesForType(ReactiveWebServerFactory.class);
    if (beanNames.isEmpty()) {
      throw new MissingWebServerFactoryBeanException(
              getClass(), ReactiveWebServerFactory.class, ApplicationType.REACTIVE_WEB);
    }
    if (beanNames.size() > 1) {
      throw new ApplicationContextException("Unable to start ReactiveWebApplicationContext due to multiple "
              + "ReactiveWebServerFactory beans : " + StringUtils.collectionToCommaDelimitedString(beanNames));
    }
    return CollectionUtils.firstElement(beanNames);
  }

  protected ReactiveWebServerFactory getWebServerFactory(String factoryBeanName) {
    return getBeanFactory().getBean(factoryBeanName, ReactiveWebServerFactory.class);
  }

  /**
   * Return the {@link HttpHandler} that should be used to process the reactive web
   * server. By default this method searches for a suitable bean in the context itself.
   *
   * @return a {@link HttpHandler} (never {@code null}
   */
  protected HttpHandler getHttpHandler() {
    // Use bean names so that we don't consider the hierarchy
    Set<String> beanNames = getBeanFactory().getBeanNamesForType(HttpHandler.class);
    if (beanNames.isEmpty()) {
      throw new ApplicationContextException(
              "Unable to start ReactiveWebApplicationContext due to missing HttpHandler bean.");
    }
    if (beanNames.size() > 1) {
      throw new ApplicationContextException(
              "Unable to start ReactiveWebApplicationContext due to multiple HttpHandler beans : "
                      + StringUtils.collectionToCommaDelimitedString(beanNames));
    }
    return getBeanFactory().getBean(CollectionUtils.firstElement(beanNames), HttpHandler.class);
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
   * @return the web server
   */
  @Nullable
  @Override
  public WebServer getWebServer() {
    WebServerManager serverManager = this.serverManager;
    return (serverManager != null) ? serverManager.getWebServer() : null;
  }

  @Nullable
  @Override
  public String getServerNamespace() {
    return this.serverNamespace;
  }

  @Override
  public void setServerNamespace(String serverNamespace) {
    this.serverNamespace = serverNamespace;
  }

}
