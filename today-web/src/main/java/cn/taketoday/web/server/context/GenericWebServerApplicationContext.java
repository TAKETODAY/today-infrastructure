/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.server.context;

import java.util.Set;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.server.ChannelWebServerFactory;
import cn.taketoday.web.server.WebServer;
import io.netty.channel.ChannelHandler;

/**
 * A {@link GenericWebServerApplicationContext} that can be used to bootstrap itself
 * from a contained {@link ChannelWebServerFactory} bean.
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
    ChannelHandler channelHandler = getChannelHandler();
    ChannelWebServerFactory factory = getWebServerFactory();

    WebServer webServer = factory.getWebServer(channelHandler);

    beanFactory.registerSingleton("webServerStartStop", new WebServerStartStopLifecycle(this, webServer));
    beanFactory.registerSingleton("webServerGracefulShutdown", new WebServerGracefulShutdownLifecycle(webServer));
    return webServer;
  }

  /**
   * Returns the {@link ChannelWebServerFactory} that should be used to create the
   * embedded {@link WebServer}. By default this method searches for a suitable bean in
   * the context itself.
   *
   * @return a {@link ChannelWebServerFactory} (never {@code null})
   */
  private ChannelWebServerFactory getWebServerFactory() {
    // Use bean names so that we don't consider the hierarchy
    Set<String> beanNames = beanFactory.getBeanNamesForType(ChannelWebServerFactory.class);
    if (beanNames.isEmpty()) {
      throw new MissingWebServerFactoryBeanException(getClass(), ChannelWebServerFactory.class);
    }
    if (beanNames.size() > 1) {
      throw new ApplicationContextException("Unable to start WebServerApplicationContext due to multiple WebServerFactory beans : "
              + StringUtils.collectionToCommaDelimitedString(beanNames));
    }
    return beanFactory.getBean(CollectionUtils.firstElement(beanNames), ChannelWebServerFactory.class);
  }

  /**
   * Return the {@link cn.taketoday.web.server.support.NettyChannelHandler} that should be used to process the web
   * server. By default, this method searches for a suitable bean in the context itself.
   *
   * @return a {@link cn.taketoday.web.server.support.NettyChannelHandler} (never {@code null}
   */
  protected ChannelHandler getChannelHandler() {
    try {
      return beanFactory.getBean(ChannelWebServerFactory.CHANNEL_HANDLER_BEAN_NAME, ChannelHandler.class);
    }
    catch (NoSuchBeanDefinitionException e) {
      throw new ApplicationContextException(
              "Unable to start WebServerApplicationContext due to missing ChannelHandler bean.");
    }
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
