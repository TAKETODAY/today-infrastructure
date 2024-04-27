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
 * Interface for receiving notification events about HttpSession id changes.
 *
 * <p>
 * In order to receive these notification events, the implementation class must be either declared in the deployment
 * descriptor of the web application, annotated with {@link WebListener}, or registered via
 * one of the addListener methods defined on {@link cn.taketoday.web.mock.ServletContext}.
 *
 * <p>
 * The order in which implementations of this interface are invoked is unspecified.
 *
 * @since Servlet 3.1
 */
public interface HttpSessionIdListener extends EventListener {

  /**
   * Receives notification that session id has been changed in a session.
   *
   * @param event the HttpSessionBindingEvent containing the session and the name and (old) value of the attribute that
   * was replaced
   * @param oldSessionId the old session id
   */
  public void sessionIdChanged(HttpSessionEvent event, String oldSessionId);

}
