/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.framework.server.light;

import java.io.IOException;
import java.util.concurrent.Executor;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.framework.WebServerException;
import cn.taketoday.framework.server.AbstractWebServer;
import cn.taketoday.framework.server.WebServerApplicationLoader;
import cn.taketoday.web.handler.DispatcherHandler;

/**
 * @author TODAY 2021/4/12 19:08
 */
public class LightWebServer extends AbstractWebServer {
  protected HTTPServer server;
  protected int socketTimeout = 10000;
  protected Executor executor;

  private DispatcherHandler httpHandler;
  private LightHttpConfig config;

  public LightWebServer() {
    this(new LightHttpConfig());
  }

  public LightWebServer(LightHttpConfig config) {
    this.config = config;
  }

  public void setConfig(LightHttpConfig config) {
    this.config = config;
  }

  public LightHttpConfig getConfig() {
    return config;
  }

  private LightHttpConfig obtainHttpConfig() {
    final LightHttpConfig config = getConfig();
    Assert.state(config != null, "No LightHttpConfig");
    return this.config;
  }

  @Override
  protected void prepareInitialize() {
    super.prepareInitialize();
    final int port = getPort();

    // update port to light config
    final LightHttpConfig config = obtainHttpConfig();
    int portInLightConfig = config.getPort();
    if (portInLightConfig == -1) {
      config.setPort(port);
    }
    else {
      if (portInLightConfig != port) {
        throw new ConfigurationException("cannot determine the server port");
      }
    }
  }

  @Override
  protected void initializeContext() {
    super.initializeContext();
    HTTPServer server = new HTTPServer(getPort());

    server.setPort(getPort());
    server.setExecutor(executor);
    server.setSocketTimeout(socketTimeout);

    this.httpHandler = new DispatcherHandler(obtainApplicationContext());
    server.setHttpHandler(httpHandler);

    server.setConfig(obtainHttpConfig());
    this.server = server;
  }

  @Override
  protected void contextInitialized() {
    super.contextInitialized();
    try {
      final WebServerApplicationLoader loader = new WebServerApplicationLoader(this::getMergedInitializers);
      loader.setDispatcher(httpHandler);
      loader.setApplicationContext(obtainApplicationContext());
      loader.onStartup(obtainApplicationContext());
    }
    catch (Throwable e) {
      throw new ConfigurationException(e);
    }
  }

  @Override
  public void start() {
    try {
      server.start();
    }
    catch (IOException e) {
      throw new WebServerException("Cannot start a web server", e);
    }
  }

  @Override
  public void stop() {
    server.stop();
  }
}
