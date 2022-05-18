/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.session;

import java.util.ArrayList;

import cn.taketoday.core.Conventions;
import cn.taketoday.framework.web.session.Cookie;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

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
 * @author TODAY <br>
 * 2019-10-03 10:56
 */
public class CookieSessionIdResolver implements SessionIdResolver {
  private static final String WRITTEN_SESSION_ID_ATTR = Conventions.getQualifiedAttributeName(
          CookieSessionIdResolver.class, "WRITTEN_SESSION_ID_ATTR");

  private final String cookieName;
  private final Cookie config;

  public CookieSessionIdResolver() {
    this(Cookie.DEFAULT_COOKIE_NAME);
  }

  public CookieSessionIdResolver(String cookieName) {
    this.config = new Cookie();
    this.cookieName = cookieName;
    config.setName(cookieName);
  }

  public CookieSessionIdResolver(Cookie config) {
    Assert.notNull(config, "Cookie is required");
    Assert.notNull(config.getName(), "Cookie name is required");
    this.config = config;
    this.cookieName = config.getName();
  }

  @Nullable
  @Override
  public String getSessionId(RequestContext context) {
    // find in request attribute
    Object attribute = context.getAttribute(WRITTEN_SESSION_ID_ATTR);
    if (attribute instanceof String sessionId) {
      return sessionId;
    }

    // find in request cookie
    HttpCookie cookie = context.getCookie(cookieName);
    if (cookie == null) {
      // fallback to response cookies
      ArrayList<HttpCookie> httpCookies = context.responseCookies();
      for (HttpCookie httpCookie : httpCookies) {
        if (cookieName.equals(httpCookie.getName())) {
          return httpCookie.getValue();
        }
      }
      return null;
    }
    return cookie.getValue();
  }

  @Override
  public void setSessionId(RequestContext context, String sessionId) {
    if (!sessionId.equals(context.getAttribute(WRITTEN_SESSION_ID_ATTR))) {
      HttpCookie cookie = createCookie(sessionId);
      context.addCookie(cookie);
      context.setAttribute(WRITTEN_SESSION_ID_ATTR, sessionId);
    }
  }

  @Override
  public void expireSession(RequestContext exchange) {
    exchange.addCookie(
            ResponseCookie.from(cookieName, "")
                    .maxAge(0)
                    .build()
    );
  }

  public String getCookieName() {
    return cookieName;
  }

  public HttpCookie createCookie(String id) {
    return config.createCookie(id);
  }

}
