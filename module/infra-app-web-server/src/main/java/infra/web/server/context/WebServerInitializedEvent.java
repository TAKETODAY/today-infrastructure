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

package infra.web.server.context;

import java.io.Serial;

import infra.context.ApplicationEvent;
import infra.web.server.WebServer;

/**
 * Event to be published when the {@link WebServer} is ready. Useful for obtaining the
 * local port of a running server.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebServerInitializedEvent extends ApplicationEvent {

  @Serial
  private static final long serialVersionUID = 1L;

  private final WebServerApplicationContext applicationContext;

  public WebServerInitializedEvent(WebServer webServer, WebServerApplicationContext applicationContext) {
    super(webServer);
    this.applicationContext = applicationContext;
  }

  /**
   * Access the {@link WebServer}.
   *
   * @return the embedded web server
   */
  public WebServer getWebServer() {
    return getSource();
  }

  /**
   * Access the application context that the server was created in. Sometimes it is
   * prudent to check that this matches expectations (like being equal to the current
   * context) before acting on the server itself.
   *
   * @return the applicationContext that the server was created from
   */
  public WebServerApplicationContext getApplicationContext() {
    return applicationContext;
  }

  /**
   * Access the source of the event (an {@link WebServer}).
   *
   * @return the embedded web server
   */
  @Override
  public WebServer getSource() {
    return (WebServer) super.getSource();
  }

}
