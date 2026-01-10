/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
  private @Nullable String domain;

  /**
   * Path of the cookie.
   */
  private @Nullable String path;

  /**
   * Whether to use "HttpOnly" cookies for the cookie.
   */
  private @Nullable Boolean httpOnly;

  /**
   * Whether to always mark the cookie as secure.
   */
  private @Nullable Boolean secure;

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
  private @Nullable SameSite sameSite;

  /**
   * Whether the generated cookie carries the Partitioned attribute.
   */

  private @Nullable Boolean partitioned;

  public String getName() {
    return this.name;
  }

  public void setName(@Nullable String name) {
    this.name = name == null ? DEFAULT_COOKIE_NAME : name;
  }

  public @Nullable String getDomain() {
    return this.domain;
  }

  public void setDomain(@Nullable String domain) {
    this.domain = domain;
  }

  public @Nullable String getPath() {
    return this.path;
  }

  public void setPath(@Nullable String path) {
    this.path = path;
  }

  public @Nullable Boolean getHttpOnly() {
    return this.httpOnly;
  }

  public void setHttpOnly(@Nullable Boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  public @Nullable Boolean getSecure() {
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

  public @Nullable SameSite getSameSite() {
    return this.sameSite;
  }

  public void setSameSite(@Nullable SameSite sameSite) {
    this.sameSite = sameSite;
  }

  public @Nullable Boolean getPartitioned() {
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
            .partitioned(Boolean.TRUE.equals(partitioned))
            .build();
  }

}
