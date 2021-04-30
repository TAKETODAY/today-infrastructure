/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import cn.taketoday.context.utils.Assert;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 * 2019-10-03 10:56
 */
public class CookieTokenResolver implements TokenResolver {

  private final String cookieName;
  private HttpCookie sessionCookie;

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
    Assert.notNull(sessionCookie, "sessionCookie must not be null");
    this.sessionCookie = sessionCookie;
    this.cookieName = sessionCookie.getName();
  }

  public CookieTokenResolver(SessionCookieConfiguration config) {
    Assert.notNull(config, "cookieConfiguration must not be null");
    setSessionCookie(config.toHttpCookie());
    this.cookieName = sessionCookie.getName();
  }

  @Override
  public String getToken(RequestContext context) {
    final HttpCookie cookie = context.getCookie(cookieName);
    if (cookie == null) {
      return null;
    }
    return cookie.getValue();
  }

  @Override
  public void saveToken(RequestContext context, WebSession session) {
    final HttpCookie cookie = cloneSessionCookie();
    cookie.setValue(session.getId());
    context.addCookie(cookie);
  }

  public HttpCookie getSessionCookie() {
    return sessionCookie;
  }

  public HttpCookie cloneSessionCookie() {
    return (HttpCookie) sessionCookie.clone();
  }

  public void setSessionCookie(HttpCookie sessionCookie) {
    this.sessionCookie = sessionCookie;
  }
}
