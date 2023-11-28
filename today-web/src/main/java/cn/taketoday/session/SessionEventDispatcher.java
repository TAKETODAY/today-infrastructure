/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.Collection;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ArrayHolder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/29 22:46
 */
public class SessionEventDispatcher {

  private final ArrayHolder<WebSessionListener> sessionListeners =
          ArrayHolder.forGenerator(WebSessionListener[]::new);

  private final ArrayHolder<WebSessionAttributeListener> attributeListeners =
          ArrayHolder.forGenerator(WebSessionAttributeListener[]::new);

  /**
   * Receives notification that a session has been created.
   */
  public void onSessionCreated(WebSession session) {
    var event = new WebSessionEvent(this, session);
    for (WebSessionListener listener : sessionListeners) {
      listener.sessionCreated(event);
    }
  }

  /**
   * Receives notification that a session is about to be invalidated.
   */
  public void onSessionDestroyed(WebSession session) {
    var event = new WebSessionEvent(this, session);
    for (WebSessionListener listener : sessionListeners) {
      listener.sessionDestroyed(event);
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
  public void attributeAdded(WebSession session, String attributeName, Object value) {
    for (WebSessionAttributeListener listener : attributeListeners) {
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
  public void attributeRemoved(WebSession session, String attributeName, @Nullable Object value) {
    for (WebSessionAttributeListener listener : attributeListeners) {
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
  public void attributeReplaced(WebSession session,
          String attributeName, Object oldValue, Object newValue) {
    for (WebSessionAttributeListener listener : attributeListeners) {
      listener.attributeReplaced(session, attributeName, oldValue, newValue);
    }
  }

  /**
   * add list of WebSessionAttributeListener
   *
   * @param array list to add
   * @throws NullPointerException input list is null
   */
  public void addAttributeListeners(@Nullable WebSessionAttributeListener... array) {
    attributeListeners.add(array);
  }

  /**
   * add list of WebSessionAttributeListener
   *
   * @param list list to add
   * @throws NullPointerException input list is null
   */
  public void addAttributeListeners(@Nullable Collection<WebSessionAttributeListener> list) {
    attributeListeners.addAll(list);
  }

  /**
   * add list of WebSessionListener
   *
   * @param array array to add
   * @throws NullPointerException input list is null
   */
  public void addSessionListeners(@Nullable WebSessionListener... array) {
    sessionListeners.add(array);
  }

  /**
   * add list of WebSessionListener
   *
   * @param list list to add
   * @throws NullPointerException input list is null
   */
  public void addSessionListeners(@Nullable Collection<WebSessionListener> list) {
    sessionListeners.addAll(list);
  }

  public ArrayHolder<WebSessionAttributeListener> getAttributeListeners() {
    return attributeListeners;
  }

  public ArrayHolder<WebSessionListener> getSessionListeners() {
    return sessionListeners;
  }

}
