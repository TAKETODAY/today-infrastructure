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

package cn.taketoday.web.session;

import java.util.Collection;

import cn.taketoday.util.ArrayHolder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/29 22:46
 */
public class SessionEventDispatcher {

  private final ArrayHolder<WebSessionListener> sessionListeners =
          ArrayHolder.forGenerator(WebSessionListener[]::new);

  public SessionEventDispatcher() { }

  public SessionEventDispatcher(Collection<WebSessionListener> webSessionListeners) {
    addSessionListeners(webSessionListeners);
  }

  public void onSessionCreated(WebSession session) {
    var event = new WebSessionEvent(this, session);
    for (WebSessionListener sessionListener : sessionListeners) {
      sessionListener.sessionCreated(event);
    }
  }

  public void onSessionDestroyed(WebSession session) {
    var event = new WebSessionEvent(this, session);
    for (WebSessionListener sessionListener : sessionListeners) {
      sessionListener.sessionDestroyed(event);
    }
  }

  public void addSessionListeners(WebSessionListener... array) {
    sessionListeners.add(array);
  }

  /**
   * add list of WebSessionListener
   *
   * @param list list to add
   * @throws NullPointerException input list is null
   */
  public void addSessionListeners(Collection<WebSessionListener> list) {
    sessionListeners.addAll(list);
  }

  public ArrayHolder<WebSessionListener> getSessionListeners() {
    return sessionListeners;
  }

}
