/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.session.config;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import infra.format.annotation.DurationUnit;
import infra.http.ResponseCookie;

/**
 * Cookie properties.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Weix Sun
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CookieProperties {

  public static final String DEFAULT_COOKIE_NAME = "SESSION";

  /**
   * Name for the cookie.
   */
  private String name = DEFAULT_COOKIE_NAME;

  /**
   * Domain for the cookie.
   */
  @Nullable
  private String domain;

  /**
   * Path of the cookie.
   */
  @Nullable
  private String path;

  /**
   * Whether to use "HttpOnly" cookies for the cookie.
   */
  @Nullable
  private Boolean httpOnly;

  /**
   * Whether to always mark the cookie as secure.
   */
  @Nullable
  private Boolean secure;

  /**
   * Maximum age of the cookie. If a duration suffix is not specified, seconds will be
   * used. A positive value indicates when the cookie expires relative to the current
   * time. A value of 0 means the cookie should expire immediately. A negative value
   * means no "Max-Age".
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration maxAge = Duration.ofMinutes(30);

  /**
   * SameSite setting for the cookie.
   */
  @Nullable
  private SameSite sameSite;

  /**
   * Whether the generated cookie carries the Partitioned attribute.
   */
  @Nullable
  private Boolean partitioned;

  public String getName() {
    return this.name;
  }

  public void setName(@Nullable String name) {
    this.name = name == null ? DEFAULT_COOKIE_NAME : name;
  }

  @Nullable
  public String getDomain() {
    return this.domain;
  }

  public void setDomain(@Nullable String domain) {
    this.domain = domain;
  }

  @Nullable
  public String getPath() {
    return this.path;
  }

  public void setPath(@Nullable String path) {
    this.path = path;
  }

  @Nullable
  public Boolean getHttpOnly() {
    return this.httpOnly;
  }

  public void setHttpOnly(@Nullable Boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  @Nullable
  public Boolean getSecure() {
    return this.secure;
  }

  public void setSecure(@Nullable Boolean secure) {
    this.secure = secure;
  }

  public Duration getMaxAge() {
    return this.maxAge;
  }

  public void setMaxAge(Duration maxAge) {
    this.maxAge = maxAge;
  }

  @Nullable
  public SameSite getSameSite() {
    return this.sameSite;
  }

  public void setSameSite(@Nullable SameSite sameSite) {
    this.sameSite = sameSite;
  }

  @Nullable
  public Boolean getPartitioned() {
    return this.partitioned;
  }

  public void setPartitioned(@Nullable Boolean partitioned) {
    this.partitioned = partitioned;
  }

  public ResponseCookie createCookie(String value) {
    return ResponseCookie.from(name, value)
            .path(path)
            .domain(domain)
            .secure(Boolean.TRUE.equals(secure))
            .httpOnly(Boolean.TRUE.equals(httpOnly))
            .maxAge(maxAge)
            .sameSite(sameSite == null ? null : sameSite.attributeValue())
            .httpOnly(Boolean.TRUE.equals(partitioned))
            .build();
  }

}
