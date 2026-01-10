/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.session;

import org.jspecify.annotations.Nullable;

import java.util.EventListener;

/**
 * This listener interface can be implemented in order to get notifications of
 * changes to the attribute lists of sessions within this web application.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/30 14:37
 */
public interface SessionAttributeListener extends EventListener {

  /**
   * Notification that an attribute has been added to a session. Called after
   * the attribute is added.
   * The default implementation is a NO-OP.
   *
   * @param session web session to hold this attribute
   * @param attributeName name of attribute
   * @param value attribute value
   */
  default void attributeAdded(Session session, String attributeName, Object value) {
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
  default void attributeRemoved(Session session, String attributeName, @Nullable Object value) {
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
  default void attributeReplaced(Session session, String attributeName, Object oldValue, Object newValue) {
  }

}
