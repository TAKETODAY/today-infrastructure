/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
