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

package cn.taketoday.framework.web.reactive.context;

import java.io.Serial;

import cn.taketoday.framework.web.context.WebServerInitializedEvent;
import cn.taketoday.framework.web.server.WebServer;

/**
 * Event to be published after the {@link WebServer} is ready. Useful for obtaining the
 * local port of a running server.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactiveWebServerInitializedEvent extends WebServerInitializedEvent {
  @Serial
  private static final long serialVersionUID = 1L;

  public ReactiveWebServerInitializedEvent(WebServer webServer, ReactiveWebServerApplicationContext applicationContext) {
    super(webServer, applicationContext);
  }

}
