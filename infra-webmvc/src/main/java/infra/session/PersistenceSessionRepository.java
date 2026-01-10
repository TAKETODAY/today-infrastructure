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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import infra.beans.factory.DisposableBean;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.StringUtils;

/**
 * SessionRepository implementation for session persistence
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/27 21:35
 */
public class PersistenceSessionRepository implements SessionRepository, DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(PersistenceSessionRepository.class);

  private final SessionRepository delegate;

  private final SessionPersister sessionPersister;

  public PersistenceSessionRepository(SessionPersister sessionPersister, SessionRepository delegate) {
    Assert.notNull(sessionPersister, "SessionPersister is required");
    Assert.notNull(delegate, "SessionRepository is required");
    this.sessionPersister = sessionPersister;
    this.delegate = delegate;
  }

  @Override
  public Session createSession() {
    return delegate.createSession();
  }

  @Override
  public Session createSession(String id) {
    return delegate.createSession(id);
  }

  @Nullable
  @Override
  public Session retrieveSession(String sessionId) {
    Session session = delegate.retrieveSession(sessionId);
    if (session == null) {
      synchronized(sessionId.intern()) {
        session = delegate.retrieveSession(sessionId);
        if (session == null) {
          try {
            session = sessionPersister.findById(sessionId);
          }
          catch (ClassNotFoundException | IOException e) {
            log.error("Unable to get session from SessionPersister: {}", sessionPersister, e);
          }
        }
      }
    }
    return session;
  }

  @Override
  public void removeSession(Session session) {
    removeSession(session.getId());
  }

  @Nullable
  @Override
  public Session removeSession(String sessionId) {
    Session ret = delegate.removeSession(sessionId);
    removePersister(sessionId, sessionPersister);
    return ret;
  }

  @Override
  public void updateLastAccessTime(Session session) {
    delegate.updateLastAccessTime(session);
  }

  @Override
  public boolean contains(String id) {
    return delegate.contains(id) || sessionPersister.contains(id);
  }

  @Override
  public int getSessionCount() {
    return getIdentifiers().length;
  }

  @Override
  public String[] getIdentifiers() {
    HashSet<String> identifiers = new HashSet<>();
    Collections.addAll(identifiers, delegate.getIdentifiers());
    Collections.addAll(identifiers, sessionPersister.keys());
    return StringUtils.toStringArray(identifiers);
  }

  /**
   * Persist all session to underlying {@link SessionPersister}
   *
   * @since 5.0
   */
  public void persistSessions() {
    for (String identifier : delegate.getIdentifiers()) {
      Session session = delegate.retrieveSession(identifier);
      if (session != null) {
        try {
          sessionPersister.persist(session);
        }
        catch (IOException e) {
          log.error("Unable to persist session: '{}' from SessionPersister: {}",
                  session, sessionPersister, e);
        }
      }
    }
  }

  /**
   * Application shutdown
   */
  @Override
  public void destroy() {
    persistSessions();
  }

  private static void removePersister(String sessionId, SessionPersister sessionPersister) {
    try {
      synchronized(sessionId.intern()) {
        sessionPersister.remove(sessionId);
      }
    }
    catch (IOException e) {
      log.error("Unable to remove session from SessionPersister: {}", sessionPersister, e);
    }
  }

  /**
   * for Session destroy event
   */
  public SessionListener createDestructionCallback() {
    return createDestructionCallback(sessionPersister);
  }

  /**
   * for Session destroy event
   */
  public static SessionListener createDestructionCallback(SessionPersister sessionPersister) {
    Assert.notNull(sessionPersister, "No SessionPersister");
    return new PersisterDestructionCallback(sessionPersister);
  }

  static class PersisterDestructionCallback implements SessionListener {
    final SessionPersister sessionPersister;

    public PersisterDestructionCallback(SessionPersister sessionPersister) {
      this.sessionPersister = sessionPersister;
    }

    @Override
    public void sessionDestroyed(Session session) {
      removePersister(session.getId(), sessionPersister);
    }

  }
}
