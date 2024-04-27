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

package cn.taketoday.web.mock.http;

import java.util.EventListener;

import cn.taketoday.web.mock.annotation.WebListener;

/**
 * Interface for receiving notification events about HttpSession lifecycle changes.
 *
 * <p>
 * In order to receive these notification events, the implementation class must be either declared in the deployment
 * descriptor of the web application, annotated with {@link WebListener}, or registered via
 * one of the addListener methods defined on {@link cn.taketoday.web.mock.ServletContext}.
 *
 * <p>
 * Implementations of this interface are invoked at their {@link #sessionCreated} method in the order in which they have
 * been declared, and at their {@link #sessionDestroyed} method in reverse order.
 *
 * @see HttpSessionEvent
 * @since Servlet 2.3
 */
public interface HttpSessionListener extends EventListener {

  /**
   * Receives notification that a session has been created.
   *
   * @param se the HttpSessionEvent containing the session
   * @implSpec The default implementation takes no action.
   */
  default public void sessionCreated(HttpSessionEvent se) {
  }

  /**
   * Receives notification that a session is about to be invalidated.
   *
   * @param se the HttpSessionEvent containing the session
   * @implSpec The default implementation takes no action.
   */
  default public void sessionDestroyed(HttpSessionEvent se) {
  }
}
