/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import java.util.ArrayList;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 * 2019-10-03 10:56
 */
public class CookieTokenResolver implements TokenResolver {

  private final String cookieName;
  private final SessionCookieConfig config;

  public CookieTokenResolver() {
    this(HttpHeaders.AUTHORIZATION);
  }

  public CookieTokenResolver(String cookieName) {
    this.config = new SessionCookieConfig();
    this.cookieName = cookieName;
    config.setName(cookieName);
  }

  public CookieTokenResolver(SessionCookieConfig config) {
    Assert.notNull(config, "cookieConfiguration must not be null");
    this.config = config;
    this.cookieName = config.getName();
  }

  @Override
  public String getToken(RequestContext context) {
    final String cookieName = this.cookieName;
    final HttpCookie cookie = context.getCookie(cookieName);
    if (cookie == null) {
      final ArrayList<HttpCookie> httpCookies = context.responseCookies();
      for (final HttpCookie httpCookie : httpCookies) {
        if (cookieName.equals(httpCookie.getName())) {
          return httpCookie.getValue();
        }
      }
      return null;
    }
    return cookie.getValue();
  }

  @Override
  public void saveToken(RequestContext context, WebSession session) {
    HttpCookie cookie = buildCookie(session.getId());
    context.addCookie(cookie);
  }

  public String getCookieName() {
    return cookieName;
  }

  public HttpCookie buildCookie(String id) {
    return config.createCookie(id);
  }

}
