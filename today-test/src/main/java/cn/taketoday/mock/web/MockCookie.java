/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.mock.web;

import java.io.Serial;
import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.mock.api.http.Cookie;

/**
 * Extension of {@code Cookie} with extra attributes, as defined in
 * <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>.
 *
 * @author Vedran Pavic
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("removal")
public class MockCookie extends Cookie {
  @Serial
  private static final long serialVersionUID = 1L;

  private static final String SAME_SITE = "SameSite";
  private static final String EXPIRES = "Expires";

  @Nullable
  private ZonedDateTime expires;

  /**
   * Construct a new {@link MockCookie} with the supplied name and value.
   *
   * @param name the name
   * @param value the value
   * @see Cookie#Cookie(String, String)
   */
  public MockCookie(String name, String value) {
    super(name, value);
  }

  /**
   * Set the "Expires" attribute for this cookie.
   */
  public void setExpires(@Nullable ZonedDateTime expires) {
    setAttribute(EXPIRES, (expires != null ? expires.format(DateTimeFormatter.RFC_1123_DATE_TIME) : null));
  }

  /**
   * Get the "Expires" attribute for this cookie.
   *
   * @return the "Expires" attribute for this cookie, or {@code null} if not set
   */
  @Nullable
  public ZonedDateTime getExpires() {
    return this.expires;
  }

  /**
   * Set the "SameSite" attribute for this cookie.
   * <p>This limits the scope of the cookie such that it will only be attached
   * to same-site requests if the supplied value is {@code "Strict"} or cross-site
   * requests if the supplied value is {@code "Lax"}.
   *
   * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
   */
  public void setSameSite(@Nullable String sameSite) {
    setAttribute(SAME_SITE, sameSite);
  }

  /**
   * Get the "SameSite" attribute for this cookie.
   *
   * @return the "SameSite" attribute for this cookie, or {@code null} if not set
   */
  @Nullable
  public String getSameSite() {
    return getAttribute(SAME_SITE);
  }

  /**
   * Factory method that parses the value of the supplied "Set-Cookie" header.
   *
   * @param setCookieHeader the "Set-Cookie" value; never {@code null} or empty
   * @return the created cookie
   */
  public static MockCookie parse(String setCookieHeader) {
    Assert.notNull(setCookieHeader, "Set-Cookie header is required");
    String[] cookieParts = setCookieHeader.split("\\s*=\\s*", 2);
    Assert.isTrue(cookieParts.length == 2, () -> "Invalid Set-Cookie header '" + setCookieHeader + "'");

    String name = cookieParts[0];
    String[] valueAndAttributes = cookieParts[1].split("\\s*;\\s*", 2);
    String value = valueAndAttributes[0];
    String[] attributes =
            (valueAndAttributes.length > 1 ? valueAndAttributes[1].split("\\s*;\\s*") : new String[0]);

    MockCookie cookie = new MockCookie(name, value);
    for (String attribute : attributes) {
      if (StringUtils.startsWithIgnoreCase(attribute, "Domain")) {
        cookie.setDomain(extractAttributeValue(attribute, setCookieHeader));
      }
      else if (StringUtils.startsWithIgnoreCase(attribute, "Max-Age")) {
        cookie.setMaxAge(Integer.parseInt(extractAttributeValue(attribute, setCookieHeader)));
      }
      else if (StringUtils.startsWithIgnoreCase(attribute, EXPIRES)) {
        try {
          cookie.setExpires(ZonedDateTime.parse(extractAttributeValue(attribute, setCookieHeader),
                  DateTimeFormatter.RFC_1123_DATE_TIME));
        }
        catch (DateTimeException ex) {
          // ignore invalid date formats
        }
      }
      else if (StringUtils.startsWithIgnoreCase(attribute, "Path")) {
        cookie.setPath(extractAttributeValue(attribute, setCookieHeader));
      }
      else if (StringUtils.startsWithIgnoreCase(attribute, "Secure")) {
        cookie.setSecure(true);
      }
      else if (StringUtils.startsWithIgnoreCase(attribute, "HttpOnly")) {
        cookie.setHttpOnly(true);
      }
      else if (StringUtils.startsWithIgnoreCase(attribute, SAME_SITE)) {
        cookie.setSameSite(extractAttributeValue(attribute, setCookieHeader));
      }
      else if (StringUtils.startsWithIgnoreCase(attribute, "Comment")) {
        cookie.setComment(extractAttributeValue(attribute, setCookieHeader));
      }
    }
    return cookie;
  }

  private static String extractAttributeValue(String attribute, String header) {
    String[] nameAndValue = attribute.split("=");
    Assert.isTrue(nameAndValue.length == 2,
            () -> "No value in attribute '" + nameAndValue[0] + "' for Set-Cookie header '" + header + "'");
    return nameAndValue[1];
  }

  @Override
  public void setAttribute(String name, @Nullable String value) {
    if (EXPIRES.equalsIgnoreCase(name)) {
      this.expires = (value != null ? ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME) : null);
    }
    super.setAttribute(name, value);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("name", getName())
            .append("value", getValue())
            .append("Path", getPath())
            .append("Domain", getDomain())
            .append("Version", getVersion())
            .append("Comment", getComment())
            .append("Secure", getSecure())
            .append("HttpOnly", isHttpOnly())
            .append(SAME_SITE, getSameSite())
            .append("Max-Age", getMaxAge())
            .append(EXPIRES, getAttribute(EXPIRES))
            .toString();
  }

}
