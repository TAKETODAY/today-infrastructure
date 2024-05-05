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

package cn.taketoday.mock.api.http;

import java.util.EventListener;

import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.api.annotation.WebListener;

/**
 * Interface for receiving notification events about HttpSession attribute changes.
 *
 * <p>
 * In order to receive these notification events, the implementation class must be either declared in the deployment
 * descriptor of the web application, annotated with {@link WebListener}, or registered via
 * one of the addListener methods defined on {@link MockContext}.
 *
 * <p>
 * The order in which implementations of this interface are invoked is unspecified.
 */
public interface HttpSessionAttributeListener extends EventListener {

  /**
   * Receives notification that an attribute has been added to a session.
   *
   * @param event the HttpSessionBindingEvent containing the session and the name and value of the attribute that was
   * added
   */
  default public void attributeAdded(HttpSessionBindingEvent event) {
  }

  /**
   * Receives notification that an attribute has been removed from a session.
   *
   * @param event the HttpSessionBindingEvent containing the session and the name and value of the attribute that was
   * removed
   */
  default public void attributeRemoved(HttpSessionBindingEvent event) {
  }

  /**
   * Receives notification that an attribute has been replaced in a session.
   *
   * @param event the HttpSessionBindingEvent containing the session and the name and (old) value of the attribute that
   * was replaced
   */
  default public void attributeReplaced(HttpSessionBindingEvent event) {
  }

}
