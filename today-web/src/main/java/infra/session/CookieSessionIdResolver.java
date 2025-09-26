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

import infra.http.HttpCookie;
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
        for (HttpCookie httpCookie : exchange.responseCookies()) {
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
      HttpCookie cookie = createCookie(sessionId);
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

  public HttpCookie createCookie(String id) {
    return config.createCookie(id);
  }

}
