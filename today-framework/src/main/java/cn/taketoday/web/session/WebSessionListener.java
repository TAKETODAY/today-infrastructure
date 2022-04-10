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

package cn.taketoday.web.session;

import java.util.EventListener;

/**
 * Interface for receiving notification events about HttpSession lifecycle changes.
 *
 * <p>
 * In order to receive these notification events, the implementation
 * class must be either declared in the deployment
 * descriptor of the web application
 *
 * <p>
 * Implementations of this interface are invoked at their {@link #sessionCreated}
 * method in the order in which they have been declared, and at their {@link #sessionDestroyed}
 * method in reverse order.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebSessionEvent
 * @since 4.0 2022/4/9 09:56
 */
public interface WebSessionListener extends EventListener {

  /**
   * Receives notification that a session has been created.
   *
   * @param se the HttpSessionEvent containing the session
   * @implSpec The default implementation takes no action.
   */
  default void sessionCreated(WebSessionEvent se) {
    // default do nothing
  }

  /**
   * Receives notification that a session is about to be invalidated.
   *
   * @param se the HttpSessionEvent containing the session
   * @implSpec The default implementation takes no action.
   */
  default void sessionDestroyed(WebSessionEvent se) {
    // default do nothing
  }

}
