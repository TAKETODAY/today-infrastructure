/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.session;

import java.util.EventListener;

/**
 * Interface for receiving notification events about Session lifecycle changes.
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
 * @see Session
 * @since 4.0 2022/4/9 09:56
 */
public interface SessionListener extends EventListener {

  /**
   * Receives notification that a session has been created.
   *
   * <p>
   * The default implementation takes no action.
   *
   * @param session the session
   */
  default void sessionCreated(Session session) {
    // default do nothing
  }

  /**
   * Receives notification that a session is about to be invalidated.
   * <p>
   * The default implementation takes no action.
   *
   * @param session the session
   */
  default void sessionDestroyed(Session session) {
    // default do nothing
  }

}
