/*
 * Copyright 2002-present the original author or authors.
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

package infra.http;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;

/**
 * Represents an HTTP cookie as a name-value pair consistent with the content of
 * the "Cookie" request header. The {@link ResponseCookie} sub-class has the
 * additional attributes expected in the "Set-Cookie" response header.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 * @since 4.0
 */
public class HttpCookie {

  private final String name;

  private final String value;

  public HttpCookie(String name, @Nullable String value) {
    Assert.hasLength(name, "'name' is required and must not be empty.");
    this.name = name;
    this.value = value != null ? value : "";
  }

  /**
   * Return the cookie name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the cookie value or an empty string (never {@code null}).
   */
  public String getValue() {
    return this.value;
  }

  @Override
  public int hashCode() {
    return this.name.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof HttpCookie otherCookie) {
      return this.name.equalsIgnoreCase(otherCookie.getName());
    }
    return false;
  }

  @Override
  public String toString() {
    return this.name + '=' + this.value;
  }

}
