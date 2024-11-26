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
import infra.web.RequestContext;

/**
 * A {@link HeaderSessionIdResolver} that uses a header to resolve the session id.
 * Specifically, this implementation will allow specifying a header name using
 * {@link #HeaderSessionIdResolver(String)}. Convenience factory methods for creating
 * instances that use common header names, such as "X-Auth-Token" and
 * "Authentication-Info", are available as well.
 * <p>
 * When a session is created, the HTTP response will have a response header of the
 * specified name and the value of the session id. For example:
 *
 * <pre>
 * HTTP/1.1 200 OK
 * X-Auth-Token: f81d4fae-7dec-11d0-a765-00a0c91e6bf6
 * </pre>
 *
 * The client should now include the session in each request by specifying the same header
 * in their request. For example:
 *
 * <pre>
 * GET /messages/ HTTP/1.1
 * Host: example.com
 * X-Auth-Token: f81d4fae-7dec-11d0-a765-00a0c91e6bf6
 * </pre>
 *
 * When the session is invalidated, the server will send an HTTP response that has the
 * header name and a blank value. For example:
 *
 * <pre>
 * HTTP/1.1 200 OK
 * X-Auth-Token:
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-10-03 10:56
 */
public class HeaderSessionIdResolver implements SessionIdResolver {

  private final String headerName;

  /**
   * The name of the header to obtain the session id from.
   *
   * @param headerName the name of the header to obtain the session id from.
   */
  public HeaderSessionIdResolver(String headerName) {
    Assert.hasText(headerName, "headerName is required");
    this.headerName = headerName;
  }

  @Nullable
  @Override
  public String getSessionId(RequestContext exchange) {
    // find in request attribute
    Object attribute = exchange.getAttribute(WRITTEN_SESSION_ID_ATTR);
    if (attribute instanceof String sessionId) {
      return sessionId;
    }
    return exchange.requestHeaders().getFirst(headerName);
  }

  @Override
  public void setSessionId(RequestContext exchange, String sessionId) {
    Assert.notNull(sessionId, "sessionId is required");
    exchange.setHeader(headerName, sessionId);
    exchange.setAttribute(WRITTEN_SESSION_ID_ATTR, sessionId);
  }

  @Override
  public void expireSession(RequestContext exchange) {
    exchange.removeHeader(headerName);
    exchange.removeAttribute(WRITTEN_SESSION_ID_ATTR);
  }

}
