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

package cn.taketoday.mock.web;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import cn.taketoday.core.style.ToStringCreator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.http.Cookie;

/**
 * Extension of {@code Cookie} with extra attributes, as defined in
 * <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>.
 *
 * @author Vedran Pavic
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.1
 */
public class MockCookie extends Cookie {

  private static final long serialVersionUID = 4312531139502726325L;

  @Nullable
  private ZonedDateTime expires;

  @Nullable
  private String sameSite;

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
   *
   * @since 5.1.11
   */
  public void setExpires(@Nullable ZonedDateTime expires) {
    this.expires = expires;
  }

  /**
   * Get the "Expires" attribute for this cookie.
   *
   * @return the "Expires" attribute for this cookie, or {@code null} if not set
   * @since 5.1.11
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
    this.sameSite = sameSite;
  }

  /**
   * Get the "SameSite" attribute for this cookie.
   *
   * @return the "SameSite" attribute for this cookie, or {@code null} if not set
   */
  @Nullable
  public String getSameSite() {
    return this.sameSite;
  }

  /**
   * Factory method that parses the value of the supplied "Set-Cookie" header.
   *
   * @param setCookieHeader the "Set-Cookie" value; never {@code null} or empty
   * @return the created cookie
   */
  public static MockCookie parse(String setCookieHeader) {
    Assert.notNull(setCookieHeader, "Set-Cookie header must not be null");
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
      else if (StringUtils.startsWithIgnoreCase(attribute, "Expires")) {
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
      else if (StringUtils.startsWithIgnoreCase(attribute, "SameSite")) {
        cookie.setSameSite(extractAttributeValue(attribute, setCookieHeader));
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
  public String toString() {
    return new ToStringCreator(this)
            .append("name", getName())
            .append("value", getValue())
            .append("Path", getPath())
            .append("Domain", getDomain())
            .append("Version", getVersion())
            .append("Comment", getComment())
            .append("Secure", getSecure())
            .append("HttpOnly", isHttpOnly())
            .append("SameSite", this.sameSite)
            .append("Max-Age", getMaxAge())
            .append("Expires", (this.expires != null ?
                                DateTimeFormatter.RFC_1123_DATE_TIME.format(this.expires) : null))
            .toString();
  }

}
