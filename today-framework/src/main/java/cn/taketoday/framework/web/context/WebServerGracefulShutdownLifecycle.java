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

package cn.taketoday.framework.web.context;

import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.framework.web.server.WebServer;

/**
 * {@link SmartLifecycle} to trigger {@link WebServer} graceful shutdown.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class WebServerGracefulShutdownLifecycle implements SmartLifecycle {

  /**
   * {@link SmartLifecycle#getPhase() SmartLifecycle phase} in which graceful shutdown
   * of the web server is performed.
   */
  public static final int SMART_LIFECYCLE_PHASE = SmartLifecycle.DEFAULT_PHASE - 1024;

  private final WebServer webServer;

  private volatile boolean running;

  /**
   * Creates a new {@code WebServerGracefulShutdownLifecycle} that will gracefully shut
   * down the given {@code webServer}.
   *
   * @param webServer web server to shut down gracefully
   */
  public WebServerGracefulShutdownLifecycle(WebServer webServer) {
    this.webServer = webServer;
  }

  @Override
  public void start() {
    this.running = true;
  }

  @Override
  public void stop() {
    throw new UnsupportedOperationException("Stop must not be invoked directly");
  }

  @Override
  public void stop(Runnable callback) {
    this.running = false;
    this.webServer.shutDownGracefully(result -> callback.run());
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  @Override
  public int getPhase() {
    return SMART_LIFECYCLE_PHASE;
  }

}
