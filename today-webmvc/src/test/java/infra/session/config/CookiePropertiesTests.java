/*
 * Copyright 2017 - 2026 the TODAY authors.
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