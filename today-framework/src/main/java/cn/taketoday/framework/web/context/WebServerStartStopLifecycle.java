/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.framework.web.server.WebServer;

/**
 * {@link SmartLifecycle} to start and stop the {@link WebServer} in a
 * {@link GenericWebServerApplicationContext}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/3 17:30
 */
public class WebServerStartStopLifecycle implements SmartLifecycle {

  private volatile boolean running;
  private final WebServer webServer;
  private final WebServerApplicationContext applicationContext;

  public WebServerStartStopLifecycle(WebServerApplicationContext applicationContext, WebServer webServer) {
    this.applicationContext = applicationContext;
    this.webServer = webServer;
  }

  @Override
  public void start() {
    webServer.start();
    this.running = true;
    applicationContext.publishEvent(
            new WebServerInitializedEvent(webServer, applicationContext));
  }

  @Override
  public void stop() {
    this.running = false;
    this.webServer.stop();
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  @Override
  public int getPhase() {
    return WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE - 1;
  }

}
