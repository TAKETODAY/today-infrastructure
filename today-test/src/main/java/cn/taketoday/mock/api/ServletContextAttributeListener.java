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
 * Interface for receiving notification events about ServletContext attribute changes.
 *
 * <p>
 * In order to receive these notification events, the implementation class must be either declared in the deployment
 * descriptor of the web application, annotated with {@link WebListener}, or registered via
 * one of the addListener methods defined on {@link MockContext}.
 *
 * <p>
 * The order in which implementations of this interface are invoked is unspecified.
 *
 * @see ServletContextAttributeEvent
 * @since Servlet 2.3
 */
public interface ServletContextAttributeListener extends EventListener {

  /**
   * Receives notification that an attribute has been added to the ServletContext.
   *
   * @param event the ServletContextAttributeEvent containing the ServletContext to which the attribute was added, along
   * with the attribute name and value
   * @implSpec The default implementation takes no action.
   */
  default void attributeAdded(ServletContextAttributeEvent event) {
  }

  /**
   * Receives notification that an attribute has been removed from the ServletContext.
   *
   * @param event the ServletContextAttributeEvent containing the ServletContext from which the attribute was removed,
   * along with the attribute name and value
   * @implSpec The default implementation takes no action.
   */
  default void attributeRemoved(ServletContextAttributeEvent event) {
  }

  /**
   * Receives notification that an attribute has been replaced in the ServletContext.
   *
   * @param event the ServletContextAttributeEvent containing the ServletContext in which the attribute was replaced,
   * along with the attribute name and its old value
   * @implSpec The default implementation takes no action.
   */
  default void attributeReplaced(ServletContextAttributeEvent event) {
  }
}
