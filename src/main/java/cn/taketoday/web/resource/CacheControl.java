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
package cn.taketoday.web.resource;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import cn.taketoday.web.Constant;

/**
 * CacheControl can be used to create the value for the Cache-Control header.
 * The Cache-Control general-header field is used to specify directives for
 * caching mechanisms in both requests and responses. Caching directives are
 * unidirectional, meaning that a given directive in a request is not implying
 * that the same directive is to be given in the response.
 *<p>
 * -- From Mozilla Wiki.
 *
 * @since 2.3.7 @off
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control">Mozilla Cache-Control</a> @on
 */
public class CacheControl implements Serializable {

  private static final long serialVersionUID = 1L;

  private final StringBuilder cacheControl = new StringBuilder();

  public static CacheControl create() {
    return new CacheControl();
  }

  /**
   * The cache must verify the status of the stale resources before using it and
   * expired ones should not be used.
   */
  public CacheControl mustRevalidate() {
    return appendSettings(Constant.MUST_REVALIDATE);
  }

  /**
   * Forces caches to submit the request to the origin server for validation
   * before releasing a cached copy.
   */
  public CacheControl noCache() {
    return appendSettings(Constant.NO_CACHE);
  }

  /**
   * The cache should not store anything about the client request or server
   * response.
   */
  public CacheControl noStore() {
    return appendSettings(Constant.NO_STORE);
  }

  /**
   * No transformations or conversions should be made to the resource. The
   * Content-Encoding, Content-Range, Content-Type headers must not be modified by
   * a proxy. A non- transparent proxy might, for example, convert between image
   * formats in order to save cache space or to reduce the amount of traffic on a
   * slow link. The no-transform directive disallows this.
   */
  public CacheControl noTransform() {
    return appendSettings(Constant.NO_TRANSFORM);
  }

  /**
   * Indicates that the response may be cached by any cache, even if the response
   * would normally be non-cacheable (e.g. if the response does not contain a
   * max-age directive or the Expires header).
   */
  public CacheControl publicCache() {
    return appendSettings(Constant.PUBLIC);
  }

  /**
   * Indicates that the response is intended for a single user and must not be
   * stored by a shared cache. A private cache may store the response.
   */
  public CacheControl privateCache() {
    return appendSettings(Constant.PRIVATE);
  }

  /**
   * Same as must-revalidate, but it only applies to shared caches (e.g., proxies)
   * and is ignored by a private cache.
   */
  public CacheControl proxyRevalidate() {
    return appendSettings(Constant.PROXY_REVALIDATE);
  }

  /**
   * Specifies the maximum amount of time a resource will be considered fresh.
   * Contrary to Expires, this directive is relative to the time of the request.
   *
   * @param duration
   *         duration
   * @param unit
   *         time unit
   */
  public CacheControl maxAge(long duration, TimeUnit unit) {
    return appendSettings(Constant.MAX_AGE, duration, unit);
  }

  /**
   * Takes precedence over max-age or the Expires header, but it only applies to
   * shared caches (e.g., proxies) and is ignored by a private cache.
   *
   * @param duration
   *         duration
   * @param unit
   *         time unit
   */
  public CacheControl sMaxAge(long duration, TimeUnit unit) {
    return appendSettings(Constant.S_MAXAGE, duration, unit);
  }

  @Override
  public String toString() {
    return cacheControl.toString();
  }

  /**
   * Returns {@code true} if no cache-control was added.
   *
   * @return {@code true} if it is empty
   */
  public boolean isEmpty() {
    return cacheControl.length() == 0;
  }

  protected CacheControl appendSettings(String key, long duration, TimeUnit unit) {

    cacheControl().append(key)
            .append('=')
            .append(unit.toSeconds(duration));

    return this;
  }

  protected final StringBuilder cacheControl() {
    final StringBuilder cacheControl = this.cacheControl;
    if (cacheControl.length() != 0) {
      cacheControl.append(", ");
    }
    return cacheControl;
  }

  protected CacheControl appendSettings(String key) {
    cacheControl().append(key);
    return this;
  }

}
