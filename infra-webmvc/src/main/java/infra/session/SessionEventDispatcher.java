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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import infra.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/29 22:46
 */
public class SessionEventDispatcher {

  private final ArrayList<SessionListener> sessionListeners = new ArrayList<>();

  private final ArrayList<SessionAttributeListener> attributeListeners = new ArrayList<>();

  /**
   * Receives notification that a session has been created.
   */
  public void onSessionCreated(Session session) {
    for (SessionListener listener : sessionListeners) {
      listener.sessionCreated(session);
    }
  }

  /**
   * Receives notification that a session is about to be invalidated.
   */
  public void onSessionDestroyed(Session session) {
    for (SessionListener listener : sessionListeners) {
      listener.sessionDestroyed(session);
    }
  }

  /**
   * Notification that an attribute has been added to a session. Called after
   * the attribute is added.
   *
   * @param session web session to hold this attribute
   * @param attributeName name of attribute
   * @param value attribute value
   */
  public void attributeAdded(Session session, String attributeName, Object value) {
    for (SessionAttributeListener listener : attributeListeners) {
      listener.attributeAdded(session, attributeName, value);
    }
  }

  /**
   * Notification that an attribute has been removed from a session. Called
   * after the attribute is removed.
   *
   * @param session web session to hold this attribute
   * @param attributeName name of attribute
   * @param value attribute value
   */
  public void attributeRemoved(Session session, String attributeName, @Nullable Object value) {
    for (SessionAttributeListener listener : attributeListeners) {
      listener.attributeRemoved(session, attributeName, value);
    }
  }

  /**
   * Notification that an attribute has been replaced in a session. Called
   * after the attribute is replaced.
   * The default implementation is a NO-OP.
   *
   * @param session web session to hold this attribute
   * @param attributeName name of attribute
   * @param oldValue attribute value
   */
  public void attributeReplaced(Session session, String attributeName, Object oldValue, Object newValue) {
    for (SessionAttributeListener listener : attributeListeners) {
      listener.attributeReplaced(session, attributeName, oldValue, newValue);
    }
  }

  /**
   * add list of SessionAttributeListener
   *
   * @param array list to add
   * @throws NullPointerException input list is null
   */
  public void addAttributeListeners(SessionAttributeListener @Nullable ... array) {
    CollectionUtils.addAll(attributeListeners, array);
    attributeListeners.trimToSize();
  }

  /**
   * add list of SessionAttributeListener
   *
   * @param list list to add
   * @throws NullPointerException input list is null
   */
  public void addAttributeListeners(@Nullable Collection<SessionAttributeListener> list) {
    attributeListeners.addAll(list);
    attributeListeners.trimToSize();
  }

  /**
   * add list of SessionListener
   *
   * @param array array to add
   */
  public void addSessionListeners(SessionListener @Nullable ... array) {
    CollectionUtils.addAll(sessionListeners, array);
    sessionListeners.trimToSize();
  }

  /**
   * add list of SessionListener
   *
   * @param list list to add
   */
  public void addSessionListeners(@Nullable Collection<SessionListener> list) {
    sessionListeners.addAll(list);
    sessionListeners.trimToSize();
  }

  public List<SessionAttributeListener> getAttributeListeners() {
    return attributeListeners;
  }

  public List<SessionListener> getSessionListeners() {
    return sessionListeners;
  }

}
