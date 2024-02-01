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

package cn.taketoday.session;

import java.util.List;

import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * Contract for session id resolution strategies. Allows for session id
 * resolution through the request and for sending the session id or expiring
 * the session through the response.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-10-03 10:56
 */
public interface SessionIdResolver {

  String WRITTEN_SESSION_ID_ATTR = Conventions.getQualifiedAttributeName(
          CookieSessionIdResolver.class, "WRITTEN_SESSION_ID_ATTR");

  /**
   * Resolving session id from RequestContext
   * <p>
   * session id including {@link #setSessionId applied session id}
   *
   * @param exchange request context
   * @return session id
   */
  @Nullable
  String getSessionId(RequestContext exchange);

  /**
   * Send the given session id to the client.
   *
   * @param exchange the current context
   * @param sessionId the session id
   */
  void setSessionId(RequestContext exchange, String sessionId);

  /**
   * Instruct the client to end the current session.
   *
   * @param exchange the current exchange
   */
  void expireSession(RequestContext exchange);

  /**
   * for Composite SessionIdResolver
   */
  static SessionIdResolver forComposite(SessionIdResolver... resolvers) {
    return new Composite(List.of(resolvers));
  }

  /**
   * for Composite SessionIdResolver
   */
  static SessionIdResolver forComposite(List<SessionIdResolver> resolvers) {
    return new Composite(resolvers);
  }

  final class Composite implements SessionIdResolver {
    final List<SessionIdResolver> resolvers;

    public Composite(List<SessionIdResolver> resolvers) {
      this.resolvers = resolvers;
    }

    @Override
    public String getSessionId(RequestContext exchange) {
      for (SessionIdResolver resolver : resolvers) {
        String token = resolver.getSessionId(exchange);
        if (token != null) {
          return token;
        }
      }
      return null;
    }

    @Override
    public void setSessionId(RequestContext exchange, String session) {
      for (SessionIdResolver resolver : resolvers) {
        resolver.setSessionId(exchange, session);
      }
    }

    @Override
    public void expireSession(RequestContext context) {
      for (SessionIdResolver resolver : resolvers) {
        String token = resolver.getSessionId(context);
        if (token != null) {
          resolver.expireSession(context);
        }
      }
    }
  }

}
