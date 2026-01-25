/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server;

/**
 * Factory interface that can be used to create a {@link WebServer}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/25 16:46
 */
public interface GenericWebServerFactory extends WebServerFactory {

  /**
   * Gets a new fully configured but paused {@link WebServer} instance. Clients should
   * not be able to connect to the returned server until {@link WebServer#start()} is
   * called (which happens when the {@code ApplicationContext} has been fully
   * refreshed).
   *
   * @return a fully configured and started {@link WebServer}
   * @see WebServer#stop()
   */
  WebServer createWebServer();

}
