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

package infra.session;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.web.RequestContext;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 23:28
 */
final class CompositeSessionIdResolver implements SessionIdResolver {

  final List<SessionIdResolver> resolvers;

  public CompositeSessionIdResolver(List<SessionIdResolver> resolvers) {
    this.resolvers = resolvers;
  }

  @Nullable
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

