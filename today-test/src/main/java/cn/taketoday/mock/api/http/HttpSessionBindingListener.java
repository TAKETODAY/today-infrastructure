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
 * Causes an object to be notified when it is bound to or unbound from a session. The object is notified by an
 * {@link HttpSessionBindingEvent} object. This may be as a result of a servlet programmer explicitly unbinding an
 * attribute from a session, due to a session being invalidated, or due to a session timing out.
 *
 * @author Various
 * @see HttpSession
 * @see HttpSessionBindingEvent
 */
public interface HttpSessionBindingListener extends EventListener {

  /**
   * Notifies the object that it is being bound to a session and identifies the session.
   *
   * @param event the event that identifies the session
   * @implSpec The default implementation takes no action.
   * @see #valueUnbound
   */
  default public void valueBound(HttpSessionBindingEvent event) {
  }

  /**
   * Notifies the object that it is being unbound from a session and identifies the session.
   *
   * @param event the event that identifies the session
   * @implSpec The default implementation takes no action.
   * @see #valueBound
   */
  default public void valueUnbound(HttpSessionBindingEvent event) {
  }
}
