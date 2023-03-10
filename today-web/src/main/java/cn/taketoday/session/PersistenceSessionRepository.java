/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.session;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

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

  public PersistenceSessionRepository(
          SessionPersister sessionPersister, SessionRepository delegate) {
    Assert.notNull(sessionPersister, "SessionPersister is required");
    Assert.notNull(delegate, "SessionRepository is required");
    this.sessionPersister = sessionPersister;
    this.delegate = delegate;
  }

  @Override
  public WebSession createSession() {
    return delegate.createSession();
  }

  @Override
  public WebSession createSession(String id) {
    return delegate.createSession(id);
  }

  @Nullable
  @Override
  public WebSession retrieveSession(String sessionId) {
    WebSession session = delegate.retrieveSession(sessionId);
    if (session == null) {
      synchronized(sessionId.intern()) {
        session = delegate.retrieveSession(sessionId);
        if (session == null) {
          try {
            session = sessionPersister.load(sessionId);
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
  public void removeSession(WebSession session) {
    removeSession(session.getId());
  }

  @Nullable
  @Override
  public WebSession removeSession(String sessionId) {
    WebSession ret = delegate.removeSession(sessionId);
    try {
      sessionPersister.remove(sessionId);
    }
    catch (IOException e) {
      log.error("Unable to remove session from SessionPersister: {}", sessionPersister, e);
    }
    return ret;
  }

  @Override
  public void updateLastAccessTime(WebSession webSession) {
    delegate.updateLastAccessTime(webSession);
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
   * Application shutdown
   */
  @Override
  public void destroy() {
    for (String identifier : delegate.getIdentifiers()) {
      WebSession session = delegate.retrieveSession(identifier);
      if (session != null) {
        try {
          sessionPersister.save(session);
        }
        catch (IOException e) {
          log.error("Unable to persist session: '{}' from SessionPersister: {}",
                  session, sessionPersister, e);
        }
      }
    }
  }

}
