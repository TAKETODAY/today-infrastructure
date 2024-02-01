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

import javax.annotation.Nullable;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;

/**
 * For request parameter name {@link SessionIdResolver}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RequestParameterSessionIdResolver implements SessionIdResolver {
  private final String parameterName;

  public RequestParameterSessionIdResolver(String parameterName) {
    Assert.hasText(parameterName, "parameterName is required");
    this.parameterName = parameterName;
  }

  @Nullable
  @Override
  public String getSessionId(RequestContext exchange) {
    // find in request attribute
    Object attribute = exchange.getAttribute(WRITTEN_SESSION_ID_ATTR);
    if (attribute instanceof String sessionId) {
      return sessionId;
    }
    return exchange.getParameter(parameterName);
  }

  @Override
  public void setSessionId(RequestContext exchange, String sessionId) {
    Assert.notNull(sessionId, "sessionId is required");
    exchange.setAttribute(WRITTEN_SESSION_ID_ATTR, sessionId);
  }

  @Override
  public void expireSession(RequestContext exchange) {
    exchange.removeAttribute(WRITTEN_SESSION_ID_ATTR);
  }

}
