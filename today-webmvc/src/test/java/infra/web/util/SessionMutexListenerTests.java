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

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import infra.session.Session;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:30
 */
class SessionMutexListenerTests {

  @Test
  void shouldSetSessionMutexAttributeOnSessionCreated() {
    // given
    SessionMutexListener listener = new SessionMutexListener();
    Session session = mock(Session.class);

    // when
    listener.sessionCreated(session);

    // then
    verify(session).setAttribute(eq(WebUtils.SESSION_MUTEX_ATTRIBUTE),
            any(Serializable.class));
  }

  @Test
  void shouldRemoveSessionMutexAttributeOnSessionDestroyed() {
    // given
    SessionMutexListener listener = new SessionMutexListener();
    Session session = mock(Session.class);

    // when
    listener.sessionDestroyed(session);

    // then
    verify(session).removeAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE);
  }

}