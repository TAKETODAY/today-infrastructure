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

import infra.http.HttpCookie;
import infra.http.ResponseCookie;
import infra.lang.Assert;
import infra.session.config.CookieProperties;
import infra.web.RequestContext;

/**
 * A {@link SessionIdResolver} that uses a cookie to obtain the session from.
 *
 * <pre>
 * HTTP/1.1 200 OK
 * Set-Cookie: SESSION=f81d4fae-7dec-11d0-a765-00a0c91e6bf6; Path=/context-root; Secure; HttpOnly
 * </pre>
 *
 * The client should now include the session in each request by specifying the same cookie
 * in their request. For example:
 *
 * <pre>
 * GET /messages/ HTTP/1.1
 * Host: example.com
 * Cookie: SESSION=f81d4fae-7dec-11d0-a765-00a0c91e6bf6
 * </pre>
 *
 * When the session is invalidated, the server will send an HTTP response that expires the
 * cookie. For example:
 *
 * <pre>
 * HTTP/1.1 200 OK
 * Set-Cookie: SESSION=; Expires=Thur, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-10-03 10:56
 */
public class CookieSessionIdResolver implements SessionIdResolver {

  private final String cookieName;

  private final CookieProperties config;

  public CookieSessionIdResolver() {
    this(new CookieProperties());
  }

  public CookieSessionIdResolver(String cookieName) {
    Assert.notNull(cookieName, "Cookie name is required");
    this.config = new CookieProperties();
    this.cookieName = cookieName;
    config.setName(cookieName);
  }

  public CookieSessionIdResolver(CookieProperties config) {
    Assert.notNull(config, "Cookie config is required");
    Assert.notNull(config.getName(), "Cookie name is required");
    this.config = config;
    this.cookieName = config.getName();
  }

  @Nullable
  @Override
  public String getSessionId(RequestContext exchange) {
    // find in request attribute
    Object attribute = exchange.getAttribute(WRITTEN_SESSION_ID_ATTR);
    if (attribute instanceof String sessionId) {
      return sessionId;
    }

    // find in request cookie
    HttpCookie cookie = exchange.getCookie(cookieName);
    if (cookie == null) {
      // fallback to response cookies
      if (exchange.hasResponseCookie()) {
        for (ResponseCookie httpCookie : exchange.responseCookies()) {
          if (cookieName.equals(httpCookie.getName())) {
            return httpCookie.getValue();
          }
        }
      }
      return null;
    }
    return cookie.getValue();
  }

  @Override
  public void setSessionId(RequestContext exchange, String sessionId) {
    if (!sessionId.equals(exchange.getAttribute(WRITTEN_SESSION_ID_ATTR))) {
      ResponseCookie cookie = createCookie(sessionId);
      exchange.addCookie(cookie);
      exchange.setAttribute(WRITTEN_SESSION_ID_ATTR, sessionId);
    }
  }

  @Override
  public void expireSession(RequestContext exchange) {
    exchange.removeCookie(cookieName);
    exchange.removeAttribute(WRITTEN_SESSION_ID_ATTR);
  }

  public String getCookieName() {
    return cookieName;
  }

  public ResponseCookie createCookie(String id) {
    return config.createCookie(id);
  }

}
