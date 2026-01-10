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
