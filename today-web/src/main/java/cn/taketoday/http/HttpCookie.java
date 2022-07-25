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

package cn.taketoday.http;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Represents an HTTP cookie as a name-value pair consistent with the content of
 * the "Cookie" request header. The {@link ResponseCookie} sub-class has the
 * additional attributes expected in the "Set-Cookie" response header.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 * @since 4.0
 */
public class HttpCookie {

  private final String name;
  private final String value;

  public HttpCookie(String name, @Nullable String value) {
    Assert.hasLength(name, "'name' is required and must not be empty.");
    this.name = name;
    this.value = value != null ? value : "";
  }

  /**
   * Return the cookie name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the cookie value or an empty string (never {@code null}).
   */
  public String getValue() {
    return this.value;
  }

  @Override
  public int hashCode() {
    return this.name.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof HttpCookie otherCookie) {
      return this.name.equalsIgnoreCase(otherCookie.getName());
    }
    return false;
  }

  @Override
  public String toString() {
    return this.name + '=' + this.value;
  }

}
