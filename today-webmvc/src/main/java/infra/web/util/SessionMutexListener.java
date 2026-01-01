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

package infra.web.util;

import java.io.Serial;
import java.io.Serializable;

import infra.session.Session;
import infra.session.SessionListener;

/**
 * SessionListener that automatically exposes the session mutex
 * when a Session gets created.
 *
 * <p>The session mutex is guaranteed to be the same object during
 * the entire lifetime of the session, available under the key defined
 * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
 * safe reference to synchronize on for locking on the current session.
 *
 * <p>In many cases, the Session reference itself is a safe mutex
 * as well, since it will always be the same object reference for the
 * same active logical session. However, this is not guaranteed across
 * different containers; the only 100% safe way is a session mutex.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebUtils#SESSION_MUTEX_ATTRIBUTE
 * @see WebUtils#getSessionMutex(Session)
 * @see infra.web.handler.mvc.AbstractController#setSynchronizeOnSession
 * @since 4.0 2022/4/9 09:58
 */
public class SessionMutexListener implements SessionListener {

  @Override
  public void sessionCreated(Session session) {
    session.setAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, new Mutex());
  }

  @Override
  public void sessionDestroyed(Session session) {
    session.removeAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE);
  }

  /**
   * The mutex to be registered.
   * Doesn't need to be anything but a plain Object to synchronize on.
   * Should be serializable to allow for Session persistence.
   */
  private static final class Mutex implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

  }

}
