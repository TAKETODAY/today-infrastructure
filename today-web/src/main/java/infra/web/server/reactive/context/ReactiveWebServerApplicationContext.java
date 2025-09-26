/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.server.reactive.context;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContextException;
import infra.http.server.reactive.HttpHandler;
import infra.util.StringUtils;
import infra.web.server.WebServer;
import infra.web.server.context.ConfigurableWebServerApplicationContext;
import infra.web.server.context.MissingWebServerFactoryBeanException;
import infra.web.server.context.WebServerGracefulShutdownLifecycle;
import infra.web.server.reactive.ReactiveWebServerFactory;

/**
 * A {@link GenericReactiveWebApplicationContext} that can be used to bootstrap itself
 * from a contained {@link ReactiveWebServerFactory} bean.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactiveWebServerApplicationContext extends GenericReactiveWebApplicationContext
        implements ConfigurableWebServerApplicationContext {

  @Nullable
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
        serverManager.webServer.stop();
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
      StandardBeanFactory beanFactory = getBeanFactory();
      ReactiveWebServerFactory webServerFactory = getWebServerFactory(beanFactory, webServerFactoryBeanName);
      boolean lazyInit = beanFactory.getBeanDefinition(webServerFactoryBeanName).isLazyInit();

      serverManager = new WebServerManager(this, webServerFactory, this::getHttpHandler, lazyInit);
      beanFactory.registerSingleton("webServerGracefulShutdown", new WebServerGracefulShutdownLifecycle(serverManager.webServer));
      beanFactory.registerSingleton("webServerStartStop", new WebServerStartStopLifecycle(serverManager));
      this.serverManager = serverManager;
    }
    initPropertySources();
  }

  protected String getWebServerFactoryBeanName() {
    // Use bean names so that we don't consider the hierarchy
    var beanNames = getBeanFactory().getBeanNamesForType(ReactiveWebServerFactory.class);
    if (beanNames.length == 0) {
      throw new MissingWebServerFactoryBeanException(getClass(), ReactiveWebServerFactory.class);
    }
    if (beanNames.length > 1) {
      throw new ApplicationContextException("Unable to start ReactiveWebApplicationContext due to multiple "
              + "ReactiveWebServerFactory beans : " + StringUtils.arrayToCommaDelimitedString(beanNames));
    }
    return beanNames[0];
  }

  protected ReactiveWebServerFactory getWebServerFactory(ConfigurableBeanFactory beanFactory, String factoryBeanName) {
    return beanFactory.getBean(factoryBeanName, ReactiveWebServerFactory.class);
  }

  /**
   * Return the {@link HttpHandler} that should be used to process the reactive web
   * server. By default this method searches for a suitable bean in the context itself.
   *
   * @return a {@link HttpHandler} (never {@code null}
   */
  protected HttpHandler getHttpHandler() {
    // Use bean names so that we don't consider the hierarchy
    var beanNames = getBeanFactory().getBeanNamesForType(HttpHandler.class);
    if (beanNames.length == 0) {
      throw new ApplicationContextException(
              "Unable to start ReactiveWebApplicationContext due to missing HttpHandler bean.");
    }
    if (beanNames.length > 1) {
      throw new ApplicationContextException(
              "Unable to start ReactiveWebApplicationContext due to multiple HttpHandler beans : "
                      + StringUtils.arrayToCommaDelimitedString(beanNames));
    }
    return getBeanFactory().getBean(beanNames[0], HttpHandler.class);
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
    return (serverManager != null) ? serverManager.webServer : null;
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
