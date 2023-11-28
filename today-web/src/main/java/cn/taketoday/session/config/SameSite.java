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

package cn.taketoday.session.config;

/**
 * SameSite values.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/11 16:49
 */
public enum SameSite {

  /**
   * Cookies are sent in both first-party and cross-origin requests.
   */
  NONE("None"),

  /**
   * Cookies are sent in a first-party context, also when following a link to the
   * origin site.
   */
  LAX("Lax"),

  /**
   * Cookies are only sent in a first-party context (i.e. not when following a link
   * to the origin site).
   */
  STRICT("Strict");

  private final String attributeValue;

  SameSite(String attributeValue) {
    this.attributeValue = attributeValue;
  }

  public String attributeValue() {
    return this.attributeValue;
  }

}
