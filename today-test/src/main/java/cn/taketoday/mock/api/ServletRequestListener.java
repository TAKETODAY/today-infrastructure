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

package cn.taketoday.mock.api;

import java.util.EventListener;

import cn.taketoday.mock.api.annotation.WebListener;

/**
 * Interface for receiving notification events about requests coming into and going out of scope of a web application.
 *
 * <p>
 * A ServletRequest is defined as coming into scope of a web application when it is about to enter the first servlet or
 * filter of the web application, and as going out of scope as it exits the last servlet or the first filter in the
 * chain.
 *
 * <p>
 * In order to receive these notification events, the implementation class must be either declared in the deployment
 * descriptor of the web application, annotated with {@link WebListener}, or registered via
 * one of the addListener methods defined on {@link MockContext}.
 *
 * <p>
 * Implementations of this interface are invoked at their {@link #requestInitialized} method in the order in which they
 * have been declared, and at their {@link #requestDestroyed} method in reverse order.
 *
 * @since Servlet 2.4
 */
public interface ServletRequestListener extends EventListener {

  /**
   * Receives notification that a ServletRequest is about to go out of scope of the web application.
   *
   * @param sre the ServletRequestEvent containing the ServletRequest and the ServletContext representing the web
   * application
   * @implSpec The default implementation takes no action.
   */
  default public void requestDestroyed(ServletRequestEvent sre) {
  }

  /**
   * Receives notification that a ServletRequest is about to come into scope of the web application.
   *
   * @param sre the ServletRequestEvent containing the ServletRequest and the ServletContext representing the web
   * application
   * @implSpec The default implementation takes no action.
   */
  default public void requestInitialized(ServletRequestEvent sre) {
  }
}
