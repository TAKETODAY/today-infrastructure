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

package cn.taketoday.framework.web.servlet.context;

import cn.taketoday.framework.web.context.WebServerInitializedEvent;
import cn.taketoday.framework.web.server.WebServer;

/**
 * Event to be published after the {@link WebServer} is ready. Useful for obtaining the
 * local port of a running server.
 *
 * <p>
 * Normally it will have been started, but listeners are free to inspect the server and
 * stop and start it if they want to.
 *
 * @author Dave Syer
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ServletWebServerInitializedEvent extends WebServerInitializedEvent {

  private final ServletWebServerApplicationContext applicationContext;

  public ServletWebServerInitializedEvent(
          WebServer webServer, ServletWebServerApplicationContext applicationContext) {
    super(webServer);
    this.applicationContext = applicationContext;
  }

  /**
   * Access the application context that the server was created in. Sometimes it is
   * prudent to check that this matches expectations (like being equal to the current
   * context) before acting on the server itself.
   *
   * @return the applicationContext that the server was created from
   */
  @Override
  public ServletWebServerApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

}
