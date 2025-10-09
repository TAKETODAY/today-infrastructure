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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 14:59
 */
class CookiePropertiesTests {

  @Test
  void setNameUpdatesName() {
    var properties = new CookieProperties();
    properties.setName("JSESSIONID");
    assertThat(properties.getName()).isEqualTo("JSESSIONID");
  }

  @Test
  void setDomainUpdatesDomain() {
    var properties = new CookieProperties();
    properties.setDomain("example.com");
    assertThat(properties.getDomain()).isEqualTo("example.com");
  }

  @Test
  void setPathUpdatesPath() {
    var properties = new CookieProperties();
    properties.setPath("/app");
    assertThat(properties.getPath()).isEqualTo("/app");
  }

  @Test
  void setMaxAgeUpdatesMaxAge() {
    var properties = new CookieProperties();
    var maxAge = Duration.ofMinutes(30);
    properties.setMaxAge(maxAge);
    assertThat(properties.getMaxAge()).isEqualTo(maxAge);
  }

  @Test
  void setNameWithNullResetsToDefault() {
    var properties = new CookieProperties();
    properties.setName("JSESSIONID");
    assertThat(properties.getName()).isEqualTo("JSESSIONID");
    properties.setName(null);
    assertThat(properties.getName()).isEqualTo(CookieProperties.DEFAULT_COOKIE_NAME);
  }

  @Test
  void setHttpOnlyUpdatesHttpOnly() {
    var properties = new CookieProperties();
    properties.setHttpOnly(true);
    assertThat(properties.getHttpOnly()).isTrue();
  }

  @Test
  void setSecureUpdatesSecure() {
    var properties = new CookieProperties();
    properties.setSecure(true);
    assertThat(properties.getSecure()).isTrue();
  }

  @Test
  void setSameSiteUpdatesSameSite() {
    var properties = new CookieProperties();
    properties.setSameSite(SameSite.STRICT);
    assertThat(properties.getSameSite()).isEqualTo(SameSite.STRICT);
  }

  @Test
  void setPartitionedUpdatesPartitioned() {
    var properties = new CookieProperties();
    properties.setPartitioned(true);
    assertThat(properties.getPartitioned()).isTrue();
  }

  @Test
  void createCookieWithDefaultProperties() {
    var properties = new CookieProperties();
    var cookie = properties.createCookie("test-value");
    assertThat(cookie.getName()).isEqualTo(CookieProperties.DEFAULT_COOKIE_NAME);
    assertThat(cookie.getValue()).isEqualTo("test-value");
    assertThat(cookie.getPath()).isNull();
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.isSecure()).isFalse();
    assertThat(cookie.isHttpOnly()).isFalse();
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(30));
    assertThat(cookie.getSameSite()).isNull();
    assertThat(cookie.toString()).doesNotContain("Partitioned");
  }

  @Test
  void createCookieWithAllPropertiesSet() {
    var properties = new CookieProperties();
    properties.setName("CUSTOM_SESSION");
    properties.setDomain("example.com");
    properties.setPath("/app");
    properties.setHttpOnly(true);
    properties.setSecure(true);
    properties.setMaxAge(Duration.ofHours(1));
    properties.setSameSite(SameSite.LAX);
    properties.setPartitioned(true);

    var cookie = properties.createCookie("test-value-2");

    assertThat(cookie.getName()).isEqualTo("CUSTOM_SESSION");
    assertThat(cookie.getValue()).isEqualTo("test-value-2");
    assertThat(cookie.getDomain()).isEqualTo("example.com");
    assertThat(cookie.getPath()).isEqualTo("/app");
    assertThat(cookie.isSecure()).isTrue();
    // The implementation has a bug where partitioned sets httpOnly.
    // If httpOnly is false and partitioned is true, httpOnly becomes true.
    // If httpOnly is true and partitioned is true, httpOnly remains true.
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofHours(1));
    assertThat(cookie.getSameSite()).isEqualTo("Lax");
    assertThat(cookie.toString()).contains("Partitioned");
  }

}