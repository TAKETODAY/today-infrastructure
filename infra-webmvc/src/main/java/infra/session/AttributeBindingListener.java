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

import java.util.EventListener;

/**
 * Causes an object to be notified when it is bound to or unbound from a session.
 * The object is notified by an {@link Session} object.
 * This may be as a result of a programmer explicitly unbinding an
 * attribute from a session, due to a session being invalidated, or due to a session timing out.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Session
 * @see Session#setAttribute(String, Object)
 * @since 4.0 2022/10/30 14:17
 */
public interface AttributeBindingListener extends EventListener {

  /**
   * Notifies the object that it is being bound to a session and identifies the session.
   * <p> The default implementation takes no action.
   * <p> When this session is recover from SessionPersister, all
   * serializable attribute will notify this event
   *
   * @param session web session
   * @param attributeName attribute name
   * @see #valueUnbound
   * @see Session#setAttribute(String, Object)
   */
  default void valueBound(Session session, String attributeName) {
  }

  /**
   * Notifies the object that it is being unbound from a session and identifies the session.
   *
   * <p>
   * The default implementation takes no action.
   *
   * @param session session
   * @param attributeName attribute name
   * @see #valueBound
   * @see Session#setAttribute(String, Object)
   */
  default void valueUnbound(Session session, String attributeName) {
  }

}
