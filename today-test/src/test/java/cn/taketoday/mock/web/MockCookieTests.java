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

package cn.taketoday.mock.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MockCookie}.
 *
 * @author Vedran Pavic
 * @author Sam Brannen
 * @since 4.0
 */
class MockCookieTests {

  @Test
  void constructCookie() {
    MockCookie cookie = new MockCookie("SESSION", "123");

    assertCookie(cookie, "SESSION", "123");
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getMaxAge()).isEqualTo(-1);
    assertThat(cookie.getPath()).isNull();
    assertThat(cookie.isHttpOnly()).isFalse();
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getSameSite()).isNull();
  }

  @Test
  void setSameSite() {
    MockCookie cookie = new MockCookie("SESSION", "123");
    cookie.setSameSite("Strict");

    assertThat(cookie.getSameSite()).isEqualTo("Strict");
  }

  @Test
  void parseHeaderWithoutAttributes() {
    MockCookie cookie = MockCookie.parse("SESSION=123");
    assertCookie(cookie, "SESSION", "123");

    cookie = MockCookie.parse("SESSION=123;");
    assertCookie(cookie, "SESSION", "123");
  }

  @SuppressWarnings("removal")
  @Test
  void parseHeaderWithAttributes() {
    MockCookie cookie = MockCookie.parse("SESSION=123; Domain=example.com; Max-Age=60; " +
            "Expires=Tue, 8 Oct 2019 19:50:00 GMT; Path=/; Secure; HttpOnly; SameSite=Lax");

    assertCookie(cookie, "SESSION", "123");
    assertThat(cookie.getDomain()).isEqualTo("example.com");
    assertThat(cookie.getMaxAge()).isEqualTo(60);
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.getSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getExpires()).isEqualTo(ZonedDateTime.parse("Tue, 8 Oct 2019 19:50:00 GMT",
            DateTimeFormatter.RFC_1123_DATE_TIME));
    assertThat(cookie.getSameSite()).isEqualTo("Lax");
    assertThat(cookie.getComment()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "bogus" })
  void parseHeaderWithInvalidExpiresAttribute(String expiresValue) {
    MockCookie cookie = MockCookie.parse("SESSION=123; Expires=" + expiresValue);

    assertCookie(cookie, "SESSION", "123");
    assertThat(cookie.getExpires()).isNull();
  }

  private void assertCookie(MockCookie cookie, String name, String value) {
    assertThat(cookie.getName()).isEqualTo(name);
    assertThat(cookie.getValue()).isEqualTo(value);
  }

  @Test
  void parseNullHeader() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> MockCookie.parse(null))
            .withMessageContaining("Set-Cookie header is required");
  }

  @Test
  void parseInvalidHeader() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> MockCookie.parse("BOOM"))
            .withMessageContaining("Invalid Set-Cookie header 'BOOM'");
  }

  @Test
  void parseInvalidAttribute() {
    String header = "SESSION=123; Path=";

    assertThatIllegalArgumentException()
            .isThrownBy(() -> MockCookie.parse(header))
            .withMessageContaining("No value in attribute 'Path' for Set-Cookie header '" + header + "'");
  }

  @Test
  void parseHeaderWithAttributesCaseSensitivity() {
    MockCookie cookie = MockCookie.parse("SESSION=123; domain=example.com; max-age=60; " +
            "expires=Tue, 8 Oct 2019 19:50:00 GMT; path=/; secure; httponly; samesite=Lax");

    assertCookie(cookie, "SESSION", "123");
    assertThat(cookie.getDomain()).isEqualTo("example.com");
    assertThat(cookie.getMaxAge()).isEqualTo(60);
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.getSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getExpires()).isEqualTo(ZonedDateTime.parse("Tue, 8 Oct 2019 19:50:00 GMT",
            DateTimeFormatter.RFC_1123_DATE_TIME));
    assertThat(cookie.getSameSite()).isEqualTo("Lax");
  }

  @Test
  void setSameSiteShouldSetAttribute() {
    MockCookie cookie = new MockCookie("SESSION", "123");
    cookie.setSameSite("Strict");

    assertThat(cookie.getAttribute("samesite")).isEqualTo("Strict");
  }

  @Test
  void setExpiresShouldSetAttribute() {
    String expiresText = "Tue, 8 Oct 2019 19:50:00 GMT";
    MockCookie cookie = new MockCookie("SESSION", "123");
    cookie.setExpires(ZonedDateTime.parse(expiresText, DateTimeFormatter.RFC_1123_DATE_TIME));

    assertThat(cookie.getAttribute("expires")).isEqualTo(expiresText);
  }

  @Test
  void setSameSiteNullShouldClear() {
    MockCookie cookie = new MockCookie("SESSION", "123");
    cookie.setSameSite("Strict");
    assertThat(cookie.getSameSite()).isEqualTo("Strict");

    cookie.setSameSite(null);
    assertThat(cookie.getSameSite()).isNull();
    assertThat(cookie.getAttribute("samesite")).isNull();
  }

  @Test
  void setExpiresNullShouldClear() {
    ZonedDateTime expiresDateTime = ZonedDateTime.parse("Tue, 8 Oct 2019 19:50:00 GMT",
            DateTimeFormatter.RFC_1123_DATE_TIME);
    MockCookie cookie = new MockCookie("SESSION", "123");
    cookie.setExpires(expiresDateTime);
    assertThat(cookie.getExpires()).isEqualTo(expiresDateTime);

    cookie.setExpires(null);
    assertThat(cookie.getExpires()).isNull();
    assertThat(cookie.getAttribute("expires")).isNull();
  }

  @Test
  void setAttributeSameSiteShouldSetSameSite() {
    MockCookie cookie = new MockCookie("SESSION", "123");
    cookie.setAttribute("samesite", "Lax");

    assertThat(cookie.getSameSite()).isEqualTo("Lax");
  }

  @Test
  void setAttributeExpiresShouldSetExpires() {
    String expiresText = "Tue, 8 Oct 2019 19:50:00 GMT";
    MockCookie cookie = new MockCookie("SESSION", "123");
    cookie.setAttribute("expires", expiresText);

    assertThat(cookie.getExpires()).isEqualTo(
            ZonedDateTime.parse(expiresText, DateTimeFormatter.RFC_1123_DATE_TIME));
  }

  @Test
  void setInvalidAttributeExpiresShouldThrow() {
    MockCookie cookie = new MockCookie("SESSION", "123");
    assertThatThrownBy(() -> cookie.setAttribute("expires", "12345")).isInstanceOf(DateTimeParseException.class);
  }

}
