/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import java.net.HttpCookie;
import java.util.Objects;

import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 *         2019-10-03 10:56
 */
public class CookieTokenResolver implements TokenResolver {

  private HttpCookie sessionCookie;

  private final String cookieName;

  public CookieTokenResolver() {
    this(Constant.AUTHORIZATION);
  }

  public CookieTokenResolver(String cookieName) {
    this(new HttpCookie(cookieName, null));
    sessionCookie.setPath("/");
    sessionCookie.setMaxAge(3600);
    sessionCookie.setHttpOnly(true);
  }

  public CookieTokenResolver(HttpCookie sessionCookie) {
    this.sessionCookie = sessionCookie;
    this.cookieName = Objects.requireNonNull(sessionCookie.getName(), "'cookie name' could not be null");
  }

  @Override
  public String getToken(RequestContext context) {

    final HttpCookie cookie = context.cookie(cookieName);
    if (cookie == null) {
      return null;
    }
    return cookie.getValue();
  }

  @Override
  public void saveToken(RequestContext context, WebSession session) {

    final HttpCookie cookie = (HttpCookie) getSessionCookie().clone();
    cookie.setValue(session.getId().toString());
    context.addCookie(cookie);
  }

  public HttpCookie getSessionCookie() {
    return sessionCookie;
  }

  public void setSessionCookie(HttpCookie sessionCookie) {
    this.sessionCookie = sessionCookie;
  }
}
