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

import infra.lang.Assert;
import infra.util.StringUtils;
import infra.web.RequestContext;

/**
 * Default implementation of {@link SessionManager} delegating to a
 * {@link SessionIdResolver} for session id resolution and to a
 * {@link SessionRepository}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-09-27 19:58
 */
public class DefaultSessionManager implements SessionManager {

  private SessionIdResolver sessionIdResolver;

  private SessionRepository sessionRepository;

  @SuppressWarnings("NullAway")
  public DefaultSessionManager(SessionRepository sessionRepository, @Nullable SessionIdResolver sessionIdResolver) {
    if (sessionIdResolver == null) {
      sessionIdResolver = new CookieSessionIdResolver();
    }

    setSessionRepository(sessionRepository);
    setSessionIdResolver(sessionIdResolver);
  }

  public void setSessionRepository(SessionRepository sessionRepository) {
    Assert.notNull(sessionRepository, "sessionRepository is required");
    this.sessionRepository = sessionRepository;
  }

  public void setSessionIdResolver(SessionIdResolver sessionIdResolver) {
    Assert.notNull(sessionIdResolver, "sessionIdResolver is required");
    this.sessionIdResolver = sessionIdResolver;
  }

  @Override
  public Session createSession() {
    return sessionRepository.createSession();
  }

  @Override
  public Session createSession(RequestContext context) {
    Session session = sessionRepository.createSession();
    session.start();
    session.save();
    sessionIdResolver.setSessionId(context, session.getId());
    return session;
  }

  @Nullable
  @Override
  public Session getSession(@Nullable String sessionId) {
    if (StringUtils.hasText(sessionId)) {
      return sessionRepository.retrieveSession(sessionId);
    }
    return null;
  }

  @Override
  @SuppressWarnings("NullAway")
  public Session getSession(RequestContext context) {
    return getSession(context, true);
  }

  @Nullable
  @Override
  public Session getSession(RequestContext context, boolean create) {
    String sessionId = sessionIdResolver.getSessionId(context);
    if (StringUtils.hasText(sessionId)) {
      Session session = sessionRepository.retrieveSession(sessionId);
      if (session == null && create) {
        // create a new session
        session = createSession(context);
      }
      return session;
    }
    else if (create) {
      // no session id
      // create a new session
      return createSession(context);
    }
    return null;
  }

}
