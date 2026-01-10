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