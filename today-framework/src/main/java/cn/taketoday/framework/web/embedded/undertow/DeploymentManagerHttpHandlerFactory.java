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

package cn.taketoday.framework.web.embedded.undertow;

import java.io.Closeable;
import java.io.IOException;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.DeploymentManager;
import jakarta.servlet.ServletException;

/**
 * {@link HttpHandlerFactory} that for a {@link DeploymentManager}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DeploymentManagerHttpHandlerFactory implements HttpHandlerFactory {

  private final DeploymentManager deploymentManager;

  DeploymentManagerHttpHandlerFactory(DeploymentManager deploymentManager) {
    this.deploymentManager = deploymentManager;
  }

  @Override
  public HttpHandler getHandler(@Nullable HttpHandler next) {
    Assert.state(next == null, "DeploymentManagerHttpHandlerFactory must be first");
    return new DeploymentManagerHandler(deploymentManager);
  }

  DeploymentManager getDeploymentManager() {
    return this.deploymentManager;
  }

  /**
   * {@link HttpHandler} that delegates to a {@link DeploymentManager}.
   */
  static class DeploymentManagerHandler implements HttpHandler, Closeable {

    private final DeploymentManager deploymentManager;

    private final HttpHandler handler;

    DeploymentManagerHandler(DeploymentManager deploymentManager) {
      this.deploymentManager = deploymentManager;
      try {
        this.handler = deploymentManager.start();
      }
      catch (ServletException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
      this.handler.handleRequest(exchange);
    }

    @Override
    public void close() throws IOException {
      try {
        this.deploymentManager.stop();
        this.deploymentManager.undeploy();
      }
      catch (ServletException ex) {
        throw new RuntimeException(ex);
      }
    }

    DeploymentManager getDeploymentManager() {
      return this.deploymentManager;
    }

  }

}
