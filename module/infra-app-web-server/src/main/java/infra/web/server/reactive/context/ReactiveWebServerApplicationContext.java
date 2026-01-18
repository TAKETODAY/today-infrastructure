/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server.reactive.context;

import org.jspecify.annotations.Nullable;

import infra.app.web.context.reactive.GenericReactiveWebApplicationContext;
import infra.beans.BeansException;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContextException;
import infra.http.reactive.server.HttpHandler;
import infra.util.StringUtils;
import infra.web.server.WebServer;
import infra.web.server.context.ConfigurableWebServerApplicationContext;
import infra.web.server.MissingWebServerFactoryBeanException;
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

  @Nullable
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
    return serverManager != null ? serverManager.webServer : null;
  }

  @Nullable
  @Override
  public String getServerNamespace() {
    return this.serverNamespace;
  }

  @Override
  public void setServerNamespace(@Nullable String serverNamespace) {
    this.serverNamespace = serverNamespace;
  }

}
