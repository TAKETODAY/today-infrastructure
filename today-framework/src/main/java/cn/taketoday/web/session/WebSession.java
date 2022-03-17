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
package cn.taketoday.web.session;

import cn.taketoday.core.AttributeAccessor;

/**
 * Provides a way to identify a user across more than one-page request
 * or visit to a Website and to store information about that user.
 *
 * <p>
 * The container uses this interface to create a session between an
 * HTTP client and an HTTP server. The session persists for a specified
 * time period, across more than one connection or page request from the user.
 * A session usually corresponds to one user, who may visit a site many times.
 * The server can maintain a session in many ways such as using cookies or rewriting URLs.
 *
 * <p>
 * This interface allows to
 * <ul>
 * <li>View and manipulate information about a session, such as the
 * session identifier, creation time, and last accessed
 * time
 * <li>Bind objects to sessions, allowing user information to persist
 * across multiple user connections
 * </ul>
 *
 * <p>
 * Session information is scoped only to the current web application
 * so information stored in one context will not be directly visible in another.
 *
 * @author TODAY <br>
 * @since 2019-09-27 20:16
 */
public interface WebSession extends AttributeAccessor {

  /**
   * Session Id
   */
  String getId();

  /**
   * remove this session, clear all attributes
   */
  void invalidate();

  long getCreationTime();
}
