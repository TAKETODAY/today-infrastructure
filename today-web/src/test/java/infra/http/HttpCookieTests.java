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

package infra.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 12:49
 */
class HttpCookieTests {

  @Test
  void constructorWithValidNameAndValue() {
    HttpCookie cookie = new HttpCookie("sessionId", "abc123");
    assertThat(cookie.getName()).isEqualTo("sessionId");
    assertThat(cookie.getValue()).isEqualTo("abc123");
  }

  @Test
  void constructorWithValidNameAndNullValue() {
    HttpCookie cookie = new HttpCookie("theme", null);
    assertThat(cookie.getName()).isEqualTo("theme");
    assertThat(cookie.getValue()).isEqualTo("");
  }

  @Test
  void constructorWithEmptyNameThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new HttpCookie("", "value"))
            .withMessage("'name' is required and must not be empty.");
  }

  @Test
  void constructorWithNullNameThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new HttpCookie(null, "value"))
            .withMessage("'name' is required and must not be empty.");
  }

  @Test
  void constructorWithWhitespaceName() {
    HttpCookie cookie = new HttpCookie(" session-id ", "abc123");
    assertThat(cookie.getName()).isEqualTo(" session-id ");
    assertThat(cookie.getValue()).isEqualTo("abc123");
  }

  @Test
  void getValueReturnsEmptyStringWhenNullValueProvided() {
    HttpCookie cookie = new HttpCookie("test", null);
    assertThat(cookie.getValue()).isEqualTo("");
  }

  @Test
  void getValueReturnsActualValueWhenProvided() {
    HttpCookie cookie = new HttpCookie("test", "value");
    assertThat(cookie.getValue()).isEqualTo("value");
  }

  @Test
  void getNameReturnsCorrectName() {
    HttpCookie cookie = new HttpCookie("JSESSIONID", "abc123");
    assertThat(cookie.getName()).isEqualTo("JSESSIONID");
  }

  @Test
  void equalsWithSameNameDifferentCase() {
    HttpCookie cookie1 = new HttpCookie("SESSION", "abc123");
    HttpCookie cookie2 = new HttpCookie("session", "def456");
    assertThat(cookie1).isEqualTo(cookie2);
  }

  @Test
  void equalsWithSameNameSameCase() {
    HttpCookie cookie1 = new HttpCookie("SESSION", "abc123");
    HttpCookie cookie2 = new HttpCookie("SESSION", "def456");
    assertThat(cookie1).isEqualTo(cookie2);
  }

  @Test
  void equalsWithDifferentNames() {
    HttpCookie cookie1 = new HttpCookie("SESSION", "abc123");
    HttpCookie cookie2 = new HttpCookie("THEME", "def456");
    assertThat(cookie1).isNotEqualTo(cookie2);
  }

  @Test
  void equalsWithSameObject() {
    HttpCookie cookie = new HttpCookie("SESSION", "abc123");
    assertThat(cookie).isEqualTo(cookie);
  }

  @Test
  void equalsWithNull() {
    HttpCookie cookie = new HttpCookie("SESSION", "abc123");
    assertThat(cookie).isNotEqualTo(null);
  }

  @Test
  void equalsWithNonHttpCookieObject() {
    HttpCookie cookie = new HttpCookie("SESSION", "abc123");
    assertThat(cookie).isNotEqualTo("SESSION=abc123");
  }

  @Test
  void hashCodeReturnsNameHashCode() {
    HttpCookie cookie = new HttpCookie("SESSION", "abc123");
    assertThat(cookie.hashCode()).isEqualTo("SESSION".hashCode());
  }

  @Test
  void toStringReturnsFormattedCookie() {
    HttpCookie cookie = new HttpCookie("SESSION", "abc123");
    assertThat(cookie.toString()).isEqualTo("SESSION=abc123");
  }

  @Test
  void toStringWithEmptyValue() {
    HttpCookie cookie = new HttpCookie("SESSION", "");
    assertThat(cookie.toString()).isEqualTo("SESSION=");
  }

  @Test
  void toStringWithNullValue() {
    HttpCookie cookie = new HttpCookie("SESSION", null);
    assertThat(cookie.toString()).isEqualTo("SESSION=");
  }

}