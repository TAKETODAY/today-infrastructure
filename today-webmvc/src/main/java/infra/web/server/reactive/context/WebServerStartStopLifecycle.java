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

import infra.context.SmartLifecycle;
import infra.web.server.WebServer;
import infra.web.server.context.WebServerGracefulShutdownLifecycle;

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

  @Override
  public boolean isPausable() {
    return false;
  }
}
