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

package cn.taketoday.web.servlet;

import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.ResponseCookie.ResponseCookieBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;

/**
 * Helper class for cookie generation, carrying cookie descriptor settings
 * as bean properties and being able to add and remove cookie to/from a
 * given response.
 *
 * @author Juergen Hoeller
 * @see #addCookie
 * @see #removeCookie
 * @since 4.0
 */
public class CookieGenerator {
  /**
   * Default path that cookies will be visible to: "/", i.e. the entire server.
   */
  public static final String DEFAULT_COOKIE_PATH = "/";

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private String cookieName;

  @Nullable
  private String cookieDomain;

  private String cookiePath = DEFAULT_COOKIE_PATH;

  @Nullable
  private Integer cookieMaxAge;

  private boolean cookieSecure = false;

  private boolean cookieHttpOnly = false;

  /**
   * Use the given name for cookies created by this generator.
   *
   * @see ResponseCookieBuilder#getName()
   */
  public void setCookieName(@Nullable String cookieName) {
    this.cookieName = cookieName;
  }

  /**
   * Return the given name for cookies created by this generator.
   */
  @Nullable
  public String getCookieName() {
    return this.cookieName;
  }

  /**
   * Use the given domain for cookies created by this generator.
   * The cookie is only visible to servers in this domain.
   *
   * @see ResponseCookieBuilder#domain
   */
  public void setCookieDomain(@Nullable String cookieDomain) {
    this.cookieDomain = cookieDomain;
  }

  /**
   * Return the domain for cookies created by this generator, if any.
   */
  @Nullable
  public String getCookieDomain() {
    return this.cookieDomain;
  }

  /**
   * Use the given path for cookies created by this generator.
   * The cookie is only visible to URLs in this path and below.
   *
   * @see ResponseCookieBuilder#path
   */
  public void setCookiePath(String cookiePath) {
    this.cookiePath = cookiePath;
  }

  /**
   * Return the path for cookies created by this generator.
   */
  public String getCookiePath() {
    return this.cookiePath;
  }

  /**
   * Use the given maximum age (in seconds) for cookies created by this generator.
   * Useful special value: -1 ... not persistent, deleted when client shuts down.
   * <p>Default is no specific maximum age at all, using the Servlet container's
   * default.
   *
   * @see ResponseCookieBuilder#maxAge
   */
  public void setCookieMaxAge(@Nullable Integer cookieMaxAge) {
    this.cookieMaxAge = cookieMaxAge;
  }

  /**
   * Return the maximum age for cookies created by this generator.
   */
  @Nullable
  public Integer getCookieMaxAge() {
    return this.cookieMaxAge;
  }

  /**
   * Set whether the cookie should only be sent using a secure protocol,
   * such as HTTPS (SSL). This is an indication to the receiving browser,
   * not processed by the HTTP server itself.
   * <p>Default is "false".
   *
   * @see ResponseCookieBuilder#secure
   */
  public void setCookieSecure(boolean cookieSecure) {
    this.cookieSecure = cookieSecure;
  }

  /**
   * Return whether the cookie should only be sent using a secure protocol,
   * such as HTTPS (SSL).
   */
  public boolean isCookieSecure() {
    return this.cookieSecure;
  }

  /**
   * Set whether the cookie is supposed to be marked with the "HttpOnly" attribute.
   * <p>Default is "false".
   *
   * @see ResponseCookieBuilder#httpOnly
   */
  public void setCookieHttpOnly(boolean cookieHttpOnly) {
    this.cookieHttpOnly = cookieHttpOnly;
  }

  /**
   * Return whether the cookie is supposed to be marked with the "HttpOnly" attribute.
   */
  public boolean isCookieHttpOnly() {
    return this.cookieHttpOnly;
  }

  /**
   * Add a cookie with the given value to the response,
   * using the cookie descriptor settings of this generator.
   * <p>Delegates to {@link #createCookie} for cookie creation.
   *
   * @param response the HTTP response to add the cookie to
   * @param cookieValue the value of the cookie to add
   * @see #setCookieName
   * @see #setCookieDomain
   * @see #setCookiePath
   * @see #setCookieMaxAge
   */
  public void addCookie(RequestContext response, String cookieValue) {
    Assert.notNull(response, "RequestContext is required");
    ResponseCookieBuilder cookie = createCookie(cookieValue);
    Integer maxAge = getCookieMaxAge();
    if (maxAge != null) {
      cookie.maxAge(maxAge);
    }
    if (isCookieSecure()) {
      cookie.secure(true);
    }
    if (isCookieHttpOnly()) {
      cookie.httpOnly(true);
    }
    response.addCookie(cookie.build());
    if (logger.isTraceEnabled()) {
      logger.trace("Added cookie [{}={}]", getCookieName(), cookieValue);
    }
  }

  /**
   * Remove the cookie that this generator describes from the response.
   * Will generate a cookie with empty value and max age 0.
   * <p>Delegates to {@link #createCookie} for cookie creation.
   *
   * @param response the HTTP response to remove the cookie from
   * @see #setCookieName
   * @see #setCookieDomain
   * @see #setCookiePath
   */
  public void removeCookie(RequestContext response) {
    Assert.notNull(response, "HttpServletResponse must not be null");
    ResponseCookieBuilder cookie = createCookie("");
    cookie.maxAge(0);
    if (isCookieSecure()) {
      cookie.secure(true);
    }
    if (isCookieHttpOnly()) {
      cookie.httpOnly(true);
    }
    response.addCookie(cookie.build());
    if (logger.isTraceEnabled()) {
      logger.trace("Removed cookie '{}'", getCookieName());
    }
  }

  /**
   * Create a cookie with the given value, using the cookie descriptor
   * settings of this generator (except for "cookieMaxAge").
   *
   * @param cookieValue the value of the cookie to crate
   * @return the cookie
   * @see #setCookieName
   * @see #setCookieDomain
   * @see #setCookiePath
   */
  protected ResponseCookieBuilder createCookie(String cookieValue) {
    ResponseCookieBuilder cookieBuilder = ResponseCookie.from(getCookieName(), cookieValue);
    if (getCookieDomain() != null) {
      cookieBuilder.domain(getCookieDomain());
    }
    cookieBuilder.path(getCookiePath());
    return cookieBuilder;
  }

}
