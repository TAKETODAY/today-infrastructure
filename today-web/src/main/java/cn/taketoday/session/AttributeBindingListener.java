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

package cn.taketoday.session;

import java.util.EventListener;

/**
 * Causes an object to be notified when it is bound to or unbound from a session.
 * The object is notified by an {@link WebSession} object.
 * This may be as a result of a programmer explicitly unbinding an
 * attribute from a session, due to a session being invalidated, or due to a session timing out.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebSession
 * @see WebSession#setAttribute(String, Object)
 * @since 4.0 2022/10/30 14:17
 */
public interface AttributeBindingListener extends EventListener {

  /**
   * Notifies the object that it is being bound to a session and identifies the session.
   *
   * @param session web session
   * @param attributeName attribute name
   * @implSpec The default implementation takes no action.
   * @see #valueUnbound
   * @see WebSession#setAttribute(String, Object)
   */
  default void valueBound(WebSession session, String attributeName) {

  }

  /**
   * Notifies the object that it is being unbound from a session and identifies the session.
   *
   * @param session session
   * @param attributeName attribute name
   * @implSpec The default implementation takes no action.
   * @see #valueBound
   * @see WebSession#setAttribute(String, Object)
   */
  default void valueUnbound(WebSession session, String attributeName) {

  }

}
