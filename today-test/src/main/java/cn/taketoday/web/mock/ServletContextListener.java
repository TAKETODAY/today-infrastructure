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

package cn.taketoday.web.mock;

import java.util.EventListener;

import cn.taketoday.web.mock.annotation.WebListener;

/**
 * Interface for receiving notification events about ServletContext lifecycle changes.
 *
 * <p>
 * In order to receive these notification events, the implementation class must be either declared in the deployment
 * descriptor of the web application, annotated with {@link WebListener}, or registered via
 * one of the addListener methods defined on {@link ServletContext}.
 *
 * <p>
 * Implementations of this interface are invoked at their {@link #contextInitialized} method in the order in which they
 * have been declared, and at their {@link #contextDestroyed} method in reverse order.
 *
 * @see ServletContextEvent
 * @since Servlet 2.3
 */
public interface ServletContextListener extends EventListener {

  /**
   * Receives notification that the web application initialization process is starting.
   *
   * <p>
   * All ServletContextListeners are notified of context initialization before any filters or servlets in the web
   * application are initialized.
   *
   * @param sce the ServletContextEvent containing the ServletContext that is being initialized
   * @implSpec The default implementation takes no action.
   */
  default public void contextInitialized(ServletContextEvent sce) {
  }

  /**
   * Receives notification that the ServletContext is about to be shut down.
   *
   * <p>
   * All servlets and filters will have been destroyed before any ServletContextListeners are notified of context
   * destruction.
   *
   * @param sce the ServletContextEvent containing the ServletContext that is being destroyed
   * @implSpec The default implementation takes no action.
   */
  default public void contextDestroyed(ServletContextEvent sce) {
  }
}
