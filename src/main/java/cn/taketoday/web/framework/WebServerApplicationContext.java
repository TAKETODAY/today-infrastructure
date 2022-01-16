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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.framework;

import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.framework.server.WebServer;

/**
 * @author TODAY <br>
 * 2019-01-17 15:57
 */
public interface WebServerApplicationContext extends WebApplicationContext {

  /**
   * Returns the {@link WebServer} that was created by the context or {@code null}
   * if the server has not yet been created.
   *
   * @return the web server
   */
  WebServer getWebServer();

}
