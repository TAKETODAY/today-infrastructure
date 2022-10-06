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

import java.time.Duration;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * An {@code HttpCookie} subclass with the additional attributes allowed in
 * the "Set-Cookie" response header. To build an instance use the {@link #from}
 * static method.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 * @since 4.0
 */
public final class ResponseCookie extends HttpCookie {

  private final Duration maxAge;

  @Nullable
  private final String domain;

  @Nullable
  private final String path;

  private final boolean secure;

  private final boolean httpOnly;

  @Nullable
  private final String sameSite;

  /**
   * Private constructor. See {@link #from(String, String)}.
   */
  private ResponseCookie(String name, @Nullable String value, Duration maxAge, @Nullable String domain,
          @Nullable String path, boolean secure, boolean httpOnly, @Nullable String sameSite) {
    super(name, value);
    Assert.notNull(maxAge, "Max age is required");

    this.maxAge = maxAge;
    this.domain = domain;
    this.path = path;
    this.secure = secure;
    this.httpOnly = httpOnly;
    this.sameSite = sameSite;

    Rfc6265Utils.validateCookieName(name);
    Rfc6265Utils.validateCookieValue(value);
    Rfc6265Utils.validateDomain(domain);
    Rfc6265Utils.validatePath(path);
  }

  /**
   * Return the cookie "Max-Age" attribute in seconds.
   * <p>A positive value indicates when the cookie expires relative to the
   * current time. A value of 0 means the cookie should expire immediately.
   * A negative value means no "Max-Age" attribute in which case the cookie
   * is removed when the browser is closed.
   */
  public Duration getMaxAge() {
    return this.maxAge;
  }

  /**
   * Return the cookie "Domain" attribute, or {@code null} if not set.
   */
  @Nullable
  public String getDomain() {
    return this.domain;
  }

  /**
   * Return the cookie "Path" attribute, or {@code null} if not set.
   */
  @Nullable
  public String getPath() {
    return this.path;
  }

  /**
   * Return {@code true} if the cookie has the "Secure" attribute.
   */
  public boolean isSecure() {
    return this.secure;
  }

  /**
   * Return {@code true} if the cookie has the "HttpOnly" attribute.
   *
   * @see <a href="https://www.owasp.org/index.php/HTTPOnly">https://www.owasp.org/index.php/HTTPOnly</a>
   */
  public boolean isHttpOnly() {
    return this.httpOnly;
  }

  /**
   * Return the cookie "SameSite" attribute, or {@code null} if not set.
   * <p>This limits the scope of the cookie such that it will only be attached to
   * same site requests if {@code "Strict"} or cross-site requests if {@code "Lax"}.
   *
   * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
   */
  @Nullable
  public String getSameSite() {
    return this.sameSite;
  }

  /**
   * Return a builder pre-populated with values from {@code "this"} instance.
   */
  public ResponseCookieBuilder mutate() {
    return new DefaultResponseCookieBuilder(getName(), getValue(), false)
            .maxAge(maxAge)
            .domain(domain)
            .path(path)
            .secure(secure)
            .httpOnly(httpOnly)
            .sameSite(sameSite);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof ResponseCookie otherCookie) {
      return getName().equalsIgnoreCase(otherCookie.getName())
              && ObjectUtils.nullSafeEquals(this.path, otherCookie.getPath())
              && ObjectUtils.nullSafeEquals(this.domain, otherCookie.getDomain());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.domain);
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.path);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getName()).append('=').append(getValue());
    if (StringUtils.hasText(getPath())) {
      sb.append("; Path=").append(getPath());
    }
    if (StringUtils.hasText(domain)) {
      sb.append("; Domain=").append(domain);
    }
    if (!maxAge.isNegative()) {
      sb.append("; Max-Age=").append(maxAge.getSeconds());
      sb.append("; Expires=");
      long millis = maxAge.getSeconds() > 0
                    ? System.currentTimeMillis() + maxAge.toMillis()
                    : 0;
      sb.append(HttpHeaders.formatDate(millis));
    }
    if (secure) {
      sb.append("; Secure");
    }
    if (httpOnly) {
      sb.append("; HttpOnly");
    }
    if (StringUtils.hasText(sameSite)) {
      sb.append("; SameSite=").append(sameSite);
    }
    return sb.toString();
  }

  /**
   * Factory method to obtain a builder for a server-defined cookie, given its
   * name only, and where the value as well as other attributes can be set
   * later via builder methods.
   *
   * @param name the cookie name
   * @return a builder to create the cookie with
   */
  public static ResponseCookieBuilder from(final String name) {
    return new DefaultResponseCookieBuilder(name, null, false);
  }

  /**
   * Factory method to obtain a builder for a server-defined cookie that starts
   * with a name-value pair and may also include attributes.
   *
   * @param name the cookie name
   * @param value the cookie value
   * @return a builder to create the cookie with
   */
  public static ResponseCookieBuilder from(final String name, final String value) {
    return new DefaultResponseCookieBuilder(name, value, false);
  }

  /**
   * Factory method to obtain a builder for a server-defined cookie. Unlike
   * {@link #from(String, String)} this option assumes input from a remote
   * server, which can be handled more leniently, e.g. ignoring an empty domain
   * name with double quotes.
   *
   * @param name the cookie name
   * @param value the cookie value
   * @return a builder to create the cookie with
   */
  public static ResponseCookieBuilder fromClientResponse(final String name, final String value) {
    return new DefaultResponseCookieBuilder(name, value, true);
  }

  /**
   * A builder for a server-defined HttpCookie with attributes.
   */
  public interface ResponseCookieBuilder {

    /**
     * Set the cookie value.
     */
    ResponseCookieBuilder value(@Nullable String value);

    /**
     * Set the cookie "Max-Age" attribute.
     *
     * <p>A positive value indicates when the cookie should expire relative
     * to the current time. A value of 0 means the cookie should expire
     * immediately. A negative value results in no "Max-Age" attribute in
     * which case the cookie is removed when the browser is closed.
     */
    ResponseCookieBuilder maxAge(Duration maxAge);

    /**
     * Variant of {@link #maxAge(Duration)} accepting a value in seconds.
     */
    ResponseCookieBuilder maxAge(long maxAgeSeconds);

    /**
     * Set the cookie "Path" attribute.
     */
    ResponseCookieBuilder path(@Nullable String path);

    /**
     * Set the cookie "Domain" attribute.
     */
    ResponseCookieBuilder domain(@Nullable String domain);

    /**
     * Add the "Secure" attribute to the cookie.
     */
    ResponseCookieBuilder secure(boolean secure);

    /**
     * Add the "HttpOnly" attribute to the cookie.
     *
     * @see <a href="https://www.owasp.org/index.php/HTTPOnly">https://www.owasp.org/index.php/HTTPOnly</a>
     */
    ResponseCookieBuilder httpOnly(boolean httpOnly);

    /**
     * Add the "SameSite" attribute to the cookie.
     * <p>This limits the scope of the cookie such that it will only be
     * attached to same site requests if {@code "Strict"} or cross-site
     * requests if {@code "Lax"}.
     *
     * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
     */
    ResponseCookieBuilder sameSite(@Nullable String sameSite);

    /**
     * Create the HttpCookie.
     */
    ResponseCookie build();
  }

  private static class Rfc6265Utils {

    private static final String SEPARATOR_CHARS = new String(new char[] {
            '(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}', ' '
    });

    private static final String DOMAIN_CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-";

    public static void validateCookieName(String name) {
      for (int i = 0; i < name.length(); i++) {
        char c = name.charAt(i);
        // CTL = <US-ASCII control chars (octets 0 - 31) and DEL (127)>
        if (c <= 0x1F || c == 0x7F) {
          throw new IllegalArgumentException(
                  name + ": RFC2616 token cannot have control chars");
        }
        if (SEPARATOR_CHARS.indexOf(c) >= 0) {
          throw new IllegalArgumentException(
                  name + ": RFC2616 token cannot have separator chars such as '" + c + "'");
        }
        if (c >= 0x80) {
          throw new IllegalArgumentException(
                  name + ": RFC2616 token can only have US-ASCII: 0x" + Integer.toHexString(c));
        }
      }
    }

    public static void validateCookieValue(@Nullable String value) {
      if (value == null) {
        return;
      }
      int start = 0;
      int end = value.length();
      if (end > 1 && value.charAt(0) == '"' && value.charAt(end - 1) == '"') {
        start = 1;
        end--;
      }
      for (int i = start; i < end; i++) {
        char c = value.charAt(i);
        if (c < 0x21 || c == 0x22 || c == 0x2c || c == 0x3b || c == 0x5c || c == 0x7f) {
          throw new IllegalArgumentException(
                  "RFC2616 cookie value cannot have '" + c + "'");
        }
        if (c >= 0x80) {
          throw new IllegalArgumentException(
                  "RFC2616 cookie value can only have US-ASCII chars: 0x" + Integer.toHexString(c));
        }
      }
    }

    public static void validateDomain(@Nullable String domain) {
      if (StringUtils.isEmpty(domain)) {
        return;
      }
      int char1 = domain.charAt(0);
      int length = domain.length();
      int charN = domain.charAt(length - 1);
      if (char1 == '-' || charN == '.' || charN == '-') {
        throw new IllegalArgumentException("Invalid first/last char in cookie domain: " + domain);
      }
      for (int i = 0, c = -1; i < length; i++) {
        int p = c;
        c = domain.charAt(i);
        if (DOMAIN_CHARS.indexOf(c) == -1 || (p == '.' && (c == '.' || c == '-')) || (p == '-' && c == '.')) {
          throw new IllegalArgumentException(domain + ": invalid cookie domain char '" + c + "'");
        }
      }
    }

    public static void validatePath(@Nullable String path) {
      if (path == null) {
        return;
      }
      for (int i = 0; i < path.length(); i++) {
        char c = path.charAt(i);
        if (c < 0x20 || c > 0x7E || c == ';') {
          throw new IllegalArgumentException(path + ": Invalid cookie path char '" + c + "'");
        }
      }
    }
  }

  /**
   * Default implementation of {@link ResponseCookieBuilder}.
   */
  private static class DefaultResponseCookieBuilder implements ResponseCookieBuilder {

    private final String name;

    @Nullable
    private String value;

    private final boolean lenient;

    private Duration maxAge = Duration.ofSeconds(-1);

    @Nullable
    private String domain;

    @Nullable
    private String path;

    private boolean secure;

    private boolean httpOnly;

    @Nullable
    private String sameSite;

    public DefaultResponseCookieBuilder(String name, @Nullable String value, boolean lenient) {
      this.name = name;
      this.value = value;
      this.lenient = lenient;
    }

    @Override
    public ResponseCookieBuilder value(@Nullable String value) {
      this.value = value;
      return this;
    }

    @Override
    public ResponseCookieBuilder maxAge(Duration maxAge) {
      this.maxAge = maxAge;
      return this;
    }

    @Override
    public ResponseCookieBuilder maxAge(long maxAgeSeconds) {
      this.maxAge = (maxAgeSeconds >= 0 ? Duration.ofSeconds(maxAgeSeconds) : Duration.ofSeconds(-1));
      return this;
    }

    @Override
    public ResponseCookieBuilder domain(@Nullable String domain) {
      this.domain = initDomain(domain);
      return this;
    }

    @Nullable
    private String initDomain(@Nullable String domain) {
      if (lenient && StringUtils.isNotEmpty(domain)) {
        String str = domain.trim();
        if (str.startsWith("\"") && str.endsWith("\"")) {
          if (str.substring(1, str.length() - 1).trim().isEmpty()) {
            return null;
          }
        }
      }
      return domain;
    }

    @Override
    public ResponseCookieBuilder path(@Nullable String path) {
      this.path = path;
      return this;
    }

    @Override
    public ResponseCookieBuilder secure(boolean secure) {
      this.secure = secure;
      return this;
    }

    @Override
    public ResponseCookieBuilder httpOnly(boolean httpOnly) {
      this.httpOnly = httpOnly;
      return this;
    }

    @Override
    public ResponseCookieBuilder sameSite(@Nullable String sameSite) {
      this.sameSite = sameSite;
      return this;
    }

    @Override
    public ResponseCookie build() {
      return new ResponseCookie(this.name, this.value, this.maxAge,
              this.domain, this.path, this.secure, this.httpOnly, this.sameSite);
    }
  }

}
