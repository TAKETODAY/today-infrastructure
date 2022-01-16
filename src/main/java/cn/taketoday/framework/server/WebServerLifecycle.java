/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.framework.server;

import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.web.framework.server.WebServer;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 22:58
 */
public class WebServerLifecycle implements SmartLifecycle {
  public static final String BEAN_NAME = "webServerLifecycle";

  private final WebServer webServer;

  private volatile boolean running;

  public WebServerLifecycle(WebServer webServer) {
    this.webServer = webServer;
  }

  @Override
  public void start() {
    this.webServer.start();
    this.running = true;
  }

  @Override
  public void stop() {
    this.webServer.stop();
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE - 1;
  }

}
