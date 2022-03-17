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

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 * 2019-10-03 10:56
 */
public class HeaderTokenResolver implements TokenResolver {

  private boolean exposeHeaders = true;

  private String authorizationHeader = HttpHeaders.AUTHORIZATION;

  // X-Required-Authorization
  private String requiredAuthorizationHeader = X_REQUIRED_AUTHORIZATION;

  public boolean isExposeHeaders() {
    return exposeHeaders;
  }

  public void setExposeHeaders(boolean exposeHeaders) {
    this.exposeHeaders = exposeHeaders;
  }

  public String getAuthorizationHeader() {
    return authorizationHeader;
  }

  public void setAuthorizationHeader(String authorizationHeader) {
    this.authorizationHeader = authorizationHeader;
  }

  public String getRequiredAuthorizationHeader() {
    return requiredAuthorizationHeader;
  }

  public void setRequiredAuthorizationHeader(String requiredAuthorizationHeader) {
    this.requiredAuthorizationHeader = requiredAuthorizationHeader;
  }

  @Override
  public String getToken(final RequestContext context) {
    String token = context.requestHeaders().getFirst(getAuthorizationHeader());

    if (StringUtils.isEmpty(token)) { // has already set the header on the current request
      token = context.responseHeaders().getFirst(getRequiredAuthorizationHeader());
    }
    return token;
  }

  @Override
  public void saveToken(RequestContext context, WebSession session) {
    final String requiredAuthorizationHeader = getRequiredAuthorizationHeader();
    final HttpHeaders responseHeaders = context.responseHeaders();
    responseHeaders.set(requiredAuthorizationHeader, session.getId());

    if (isExposeHeaders()) {
      responseHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, requiredAuthorizationHeader);
    }
  }

}
