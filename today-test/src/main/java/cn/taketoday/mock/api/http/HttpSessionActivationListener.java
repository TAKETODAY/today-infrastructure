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

/**
 * Objects that are bound to a session may listen to container events notifying them that sessions will be passivated
 * and that session will be activated. A container that migrates session between VMs or persists sessions is required to
 * notify all attributes bound to sessions implementing HttpSessionActivationListener.
 */
public interface HttpSessionActivationListener extends EventListener {

  /**
   * Notification that the session is about to be passivated.
   *
   * @param se the {@link HttpSessionEvent} indicating the passivation of the session
   * @implSpec The default implementation takes no action.
   */
  default public void sessionWillPassivate(HttpSessionEvent se) {
  }

  /**
   * Notification that the session has just been activated.
   *
   * @param se the {@link HttpSessionEvent} indicating the activation of the session
   * @implSpec The default implementation takes no action.
   */
  default public void sessionDidActivate(HttpSessionEvent se) {
  }
}
