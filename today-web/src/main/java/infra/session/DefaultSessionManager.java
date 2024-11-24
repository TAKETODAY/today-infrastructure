/*
 * Copyright 2017 - 2024 the original author or authors.
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

import infra.lang.Assert;
import infra.lang.Nullable;
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
  public WebSession createSession() {
    return sessionRepository.createSession();
  }

  @Override
  public WebSession createSession(RequestContext context) {
    WebSession session = sessionRepository.createSession();
    session.start();
    session.save();
    sessionIdResolver.setSessionId(context, session.getId());
    return session;
  }

  @Nullable
  @Override
  public WebSession getSession(@Nullable String sessionId) {
    if (StringUtils.hasText(sessionId)) {
      return sessionRepository.retrieveSession(sessionId);
    }
    return null;
  }

  @Override
  public WebSession getSession(RequestContext context) {
    return getSession(context, true);
  }

  @Nullable
  @Override
  public WebSession getSession(RequestContext context, boolean create) {
    String sessionId = sessionIdResolver.getSessionId(context);
    if (StringUtils.hasText(sessionId)) {
      WebSession session = sessionRepository.retrieveSession(sessionId);
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
