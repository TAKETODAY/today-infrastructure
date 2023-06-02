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

package cn.taketoday.framework.web.embedded.undertow;

import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.util.StringUtils;
import io.undertow.Handlers;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentManager;

/**
 * {@link WebServer} that can be used to control an embedded Undertow server. Typically
 * this class should be created using {@link UndertowServletWebServerFactory} and not
 * directly.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Christoph Dreis
 * @author Kristine Jetzke
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see UndertowServletWebServerFactory
 * @since 4.0
 */
public class UndertowServletWebServer extends UndertowWebServer {

  private final String contextPath;

  private final DeploymentManager manager;

  /**
   * Create a new {@link UndertowServletWebServer} instance.
   *
   * @param builder the builder
   * @param httpHandlerFactories the handler factories
   * @param contextPath the root context path
   * @param autoStart if the server should be started
   */
  public UndertowServletWebServer(Builder builder,
          Iterable<HttpHandlerFactory> httpHandlerFactories, String contextPath, boolean autoStart) {
    super(builder, httpHandlerFactories, autoStart);
    this.contextPath = contextPath;
    this.manager = findManager(httpHandlerFactories);
  }

  private DeploymentManager findManager(Iterable<HttpHandlerFactory> httpHandlerFactories) {
    for (HttpHandlerFactory httpHandlerFactory : httpHandlerFactories) {
      if (httpHandlerFactory instanceof DeploymentManagerHttpHandlerFactory) {
        return ((DeploymentManagerHttpHandlerFactory) httpHandlerFactory).getDeploymentManager();
      }
    }
    throw new IllegalStateException(
            "Cannot determine the DeploymentManager from httpHandlerFactories:" + httpHandlerFactories);
  }

  @Override
  protected HttpHandler createHttpHandler() {
    HttpHandler handler = super.createHttpHandler();
    if (StringUtils.isNotEmpty(this.contextPath)) {
      handler = Handlers.path().addPrefixPath(this.contextPath, handler);
    }
    return handler;
  }

  @Override
  protected String getStartLogMessage() {
    String message = super.getStartLogMessage();
    if (StringUtils.hasText(this.contextPath)) {
      message += " with context path '" + this.contextPath + "'";
    }
    return message;
  }

  public DeploymentManager getDeploymentManager() {
    return this.manager;
  }

}
