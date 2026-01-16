/*
 * Copyright 2002-present the original author or authors.
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

package infra.web.server.context;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContextException;
import infra.context.support.GenericApplicationContext;
import infra.util.StringUtils;
import infra.web.server.GenericWebServerFactory;
import infra.web.server.MissingWebServerFactoryBeanException;
import infra.web.server.WebServer;

/**
 * A {@link GenericWebServerApplicationContext} that can be used to bootstrap itself
 * from a contained {@link GenericWebServerFactory} bean.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/3 16:52
 */
public class GenericWebServerApplicationContext extends GenericApplicationContext implements ConfigurableWebServerApplicationContext {

  @Nullable
  private String serverNamespace;

  @Nullable
  private volatile WebServer webServer;

  /**
   * Create a new {@link GenericWebServerApplicationContext}.
   */
  public GenericWebServerApplicationContext() {
  }

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
    GenericWebServerFactory factory = getWebServerFactory();

    WebServer webServer = factory.getWebServer();

    beanFactory.registerSingleton("webServerStartStop", new WebServerStartStopLifecycle(this, webServer));
    beanFactory.registerSingleton("webServerGracefulShutdown", new WebServerGracefulShutdownLifecycle(webServer));
    return webServer;
  }

  /**
   * Returns the {@link GenericWebServerFactory} that should be used to create the
   * embedded {@link WebServer}. By default this method searches for a suitable bean in
   * the context itself.
   *
   * @return a {@link GenericWebServerFactory} (never {@code null})
   */
  private GenericWebServerFactory getWebServerFactory() {
    // Use bean names so that we don't consider the hierarchy
    var beanNames = beanFactory.getBeanNamesForType(GenericWebServerFactory.class);
    if (beanNames.length == 0) {
      throw new MissingWebServerFactoryBeanException(getClass(), GenericWebServerFactory.class);
    }
    if (beanNames.length > 1) {
      throw new ApplicationContextException("Unable to start WebServerApplicationContext due to multiple WebServerFactory beans : "
              + StringUtils.arrayToCommaDelimitedString(beanNames));
    }
    return beanFactory.getBean(beanNames[0], GenericWebServerFactory.class);
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

}
