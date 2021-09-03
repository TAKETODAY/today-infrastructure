/*
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
import java.time.Duration;

import cn.taketoday.web.http.HttpHeaders;

/**
 * Session cookie properties.
 *
 * @author TODAY(taketoday @ foxmail.com) https://taketoday.cn <br>
 * 2019-01-26 17:16
 */
public class SessionCookieConfiguration {

  private String name = HttpHeaders.AUTHORIZATION;
  private String path = "/";
  private String domain;
  private String comment;
  private boolean secure;
  private Duration maxAge = Duration.ofMinutes(30);
  private boolean httpOnly = true;
  private int version = 1;    // Version=1 ... RFC 2965 style

  public HttpCookie toHttpCookie() {
    final HttpCookie httpCookie = new HttpCookie(name, null);

    httpCookie.setPath(path);
    httpCookie.setDomain(domain);
    httpCookie.setSecure(secure);
    httpCookie.setComment(comment);
    httpCookie.setHttpOnly(httpOnly);
    httpCookie.setMaxAge(maxAge.getSeconds());
    return httpCookie;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public Duration getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(Duration maxAge) {
    this.maxAge = maxAge;
  }

  public boolean isHttpOnly() {
    return httpOnly;
  }

  public void setHttpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
