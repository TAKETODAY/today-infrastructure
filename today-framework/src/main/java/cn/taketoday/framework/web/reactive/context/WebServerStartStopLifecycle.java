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

package cn.taketoday.framework.web.reactive.context;

import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.framework.web.context.WebServerGracefulShutdownLifecycle;
import cn.taketoday.framework.web.server.WebServer;

/**
 * {@link SmartLifecycle} to start and stop the {@link WebServer} in a
 * {@link ReactiveWebServerApplicationContext}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class WebServerStartStopLifecycle implements SmartLifecycle {

  private final WebServerManager weServerManager;

  private volatile boolean running;

  WebServerStartStopLifecycle(WebServerManager weServerManager) {
    this.weServerManager = weServerManager;
  }

  @Override
  public void start() {
    this.weServerManager.start();
    this.running = true;
  }

  @Override
  public void stop() {
    this.running = false;
    this.weServerManager.stop();
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  @Override
  public int getPhase() {
    return WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE - 1024;
  }

}
