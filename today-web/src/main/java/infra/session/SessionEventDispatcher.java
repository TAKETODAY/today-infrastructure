/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.session;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

import infra.util.ArrayHolder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/29 22:46
 */
public class SessionEventDispatcher {

  private final ArrayHolder<SessionListener> sessionListeners =
          ArrayHolder.forGenerator(SessionListener[]::new);

  private final ArrayHolder<SessionAttributeListener> attributeListeners =
          ArrayHolder.forGenerator(SessionAttributeListener[]::new);

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
  public void attributeReplaced(Session session,
          String attributeName, Object oldValue, Object newValue) {
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
    attributeListeners.addAll(array);
  }

  /**
   * add list of SessionAttributeListener
   *
   * @param list list to add
   * @throws NullPointerException input list is null
   */
  public void addAttributeListeners(@Nullable Collection<SessionAttributeListener> list) {
    attributeListeners.addAll(list);
  }

  /**
   * add list of SessionListener
   *
   * @param array array to add
   */
  public void addSessionListeners(SessionListener @Nullable ... array) {
    sessionListeners.addAll(array);
  }

  /**
   * add list of SessionListener
   *
   * @param list list to add
   */
  public void addSessionListeners(@Nullable Collection<SessionListener> list) {
    sessionListeners.addAll(list);
  }

  public ArrayHolder<SessionAttributeListener> getAttributeListeners() {
    return attributeListeners;
  }

  public ArrayHolder<SessionListener> getSessionListeners() {
    return sessionListeners;
  }

}
