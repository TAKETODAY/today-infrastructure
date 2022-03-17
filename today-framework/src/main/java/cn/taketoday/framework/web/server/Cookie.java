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

package cn.taketoday.framework.web.server;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import cn.taketoday.format.annotation.DurationUnit;

/**
 * Cookie properties.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Weix Sun
 * @since 4.0
 */
public class Cookie {

  /**
   * Name for the cookie.
   */
  private String name;

  /**
   * Domain for the cookie.
   */
  private String domain;

  /**
   * Path of the cookie.
   */
  private String path;

  /**
   * Whether to use "HttpOnly" cookies for the cookie.
   */
  private Boolean httpOnly;

  /**
   * Whether to always mark the cookie as secure.
   */
  private Boolean secure;

  /**
   * Maximum age of the cookie. If a duration suffix is not specified, seconds will be
   * used. A positive value indicates when the cookie expires relative to the current
   * time. A value of 0 means the cookie should expire immediately. A negative value
   * means no "Max-Age".
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration maxAge;

  /**
   * SameSite setting for the cookie.
   */
  private SameSite sameSite;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDomain() {
    return this.domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Boolean getHttpOnly() {
    return this.httpOnly;
  }

  public void setHttpOnly(Boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  public Boolean getSecure() {
    return this.secure;
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  public Duration getMaxAge() {
    return this.maxAge;
  }

  public void setMaxAge(Duration maxAge) {
    this.maxAge = maxAge;
  }

  public SameSite getSameSite() {
    return this.sameSite;
  }

  public void setSameSite(SameSite sameSite) {
    this.sameSite = sameSite;
  }

  /**
   * SameSite values.
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

}
