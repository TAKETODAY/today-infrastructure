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

package cn.taketoday.framework.web.context;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.framework.web.server.WebServer;

/**
 * Event to be published when the {@link WebServer} is ready. Useful for obtaining the
 * local port of a running server.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class WebServerInitializedEvent extends ApplicationEvent {

  protected WebServerInitializedEvent(WebServer webServer) {
    super(webServer);
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
  public abstract WebServerApplicationContext getApplicationContext();

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
