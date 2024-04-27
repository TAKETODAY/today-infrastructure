/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.testfixture.servlet;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.mock.http.Cookie;

/**
 * Extension of {@code Cookie} with extra attributes, as defined in
 * <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>.
 *
 * @author Vedran Pavic
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class MockCookie extends Cookie {

  private static final long serialVersionUID = 4312531139502726325L;

  private ZonedDateTime expires;

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
   * @since 4.0
   */
  public void setExpires(ZonedDateTime expires) {
    this.expires = expires;
  }

  /**
   * Get the "Expires" attribute for this cookie.
   *
   * @return the "Expires" attribute for this cookie, or {@code null} if not set
   * @since 4.0
   */

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
  public void setSameSite(String sameSite) {
    this.sameSite = sameSite;
  }

  /**
   * Get the "SameSite" attribute for this cookie.
   *
   * @return the "SameSite" attribute for this cookie, or {@code null} if not set
   */

  public String getSameSite() {
    return this.sameSite;
  }

  /**
   * Factory method that parses the value of the supplied "Set-Cookie" header.
   *
   * @param setCookieHeader the "Set-Cookie" value; never {@code null} or empty
   * @return the created cookie
   */
  @SuppressWarnings("removal")
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
      if (startsWithIgnoreCase(attribute, "Domain")) {
        cookie.setDomain(extractAttributeValue(attribute, setCookieHeader));
      }
      else if (startsWithIgnoreCase(attribute, "Max-Age")) {
        cookie.setMaxAge(Integer.parseInt(extractAttributeValue(attribute, setCookieHeader)));
      }
      else if (startsWithIgnoreCase(attribute, "Expires")) {
        try {
          cookie.setExpires(ZonedDateTime.parse(extractAttributeValue(attribute, setCookieHeader),
                  DateTimeFormatter.RFC_1123_DATE_TIME));
        }
        catch (DateTimeException ex) {
          // ignore invalid date formats
        }
      }
      else if (startsWithIgnoreCase(attribute, "Path")) {
        cookie.setPath(extractAttributeValue(attribute, setCookieHeader));
      }
      else if (startsWithIgnoreCase(attribute, "Secure")) {
        cookie.setSecure(true);
      }
      else if (startsWithIgnoreCase(attribute, "HttpOnly")) {
        cookie.setHttpOnly(true);
      }
      else if (startsWithIgnoreCase(attribute, "SameSite")) {
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

  /**
   * Test if the given {@code String} starts with the specified prefix,
   * ignoring upper/lower case.
   *
   * @param str the {@code String} to check
   * @param prefix the prefix to look for
   * @see java.lang.String#startsWith
   */
  public static boolean startsWithIgnoreCase(String str, String prefix) {
    return (str != null && prefix != null && str.length() >= prefix.length() &&
            str.regionMatches(true, 0, prefix, 0, prefix.length()));
  }

//  @Override
//  public String toString() {
//    return new ToStringBuilder(this)
//            .append("name", getName())
//            .append("value", getValue())
//            .append("Path", getPath())
//            .append("Domain", getDomain())
//            .append("Version", getVersion())
//            .append("Comment", getComment())
//            .append("Secure", getSecure())
//            .append("HttpOnly", isHttpOnly())
//            .append("SameSite", this.sameSite)
//            .append("Max-Age", getMaxAge())
//            .append("Expires", (this.expires != null ?
//                                DateTimeFormatter.RFC_1123_DATE_TIME.format(this.expires) : null))
//            .toString();
//  }

}
