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

import cn.taketoday.lang.Nullable;

/**
 * This listener interface can be implemented in order to get notifications of
 * changes to the attribute lists of sessions within this web application.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/30 14:37
 */
public interface WebSessionAttributeListener extends EventListener {

  /**
   * Notification that an attribute has been added to a session. Called after
   * the attribute is added.
   * The default implementation is a NO-OP.
   *
   * @param session web session to hold this attribute
   * @param attributeName name of attribute
   * @param value attribute value
   */
  default void attributeAdded(WebSession session, String attributeName, Object value) {

  }

  /**
   * Notification that an attribute has been removed from a session. Called
   * after the attribute is removed.
   * The default implementation is a NO-OP.
   *
   * @param session web session to hold this attribute
   * @param attributeName name of attribute
   * @param value attribute value
   */
  default void attributeRemoved(WebSession session, String attributeName, @Nullable Object value) {

  }

  /**
   * Notification that an attribute has been replaced in a session. Called
   * after the attribute is replaced.
   * The default implementation is a NO-OP.
   *
   * @param session web session to hold this attribute
   * @param attributeName name of attribute
   * @param oldValue old attribute value
   * @param newValue new attribute value
   */
  default void attributeReplaced(WebSession session, String attributeName, Object oldValue, Object newValue) {

  }

}
