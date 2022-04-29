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
package cn.taketoday.web;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.web.servlet.ServletContextAware;

/**
 * Interface to provide configuration for a web application. This is read-only while
 * the application is running, but may be reloaded if the implementation supports this.
 *
 * @author TODAY 2019-07-10 22:03
 */
public interface WebApplicationContext extends ApplicationContext {

  /**
   * Scope identifier for request scope: "request".
   * Supported in addition to the standard scopes "singleton" and "prototype".
   */
  String SCOPE_REQUEST = "request";

  /**
   * Scope identifier for session scope: "session".
   * Supported in addition to the standard scopes "singleton" and "prototype".
   */
  String SCOPE_SESSION = "session";

  /**
   * Scope identifier for the global web application scope: "application".
   * Supported in addition to the standard scopes "singleton" and "prototype".
   */
  String SCOPE_APPLICATION = "application";

  /**
   * Returns the portion of the request URI that indicates the context of the
   * request. The context path always comes first in a request URI. The path
   * starts with a "" character but does not end with a "" character. The
   * container does not decode this string.
   *
   * @return a <code>String</code> specifying the portion of the request URI that
   * indicates the context of the request
   */
  String getContextPath();
}
