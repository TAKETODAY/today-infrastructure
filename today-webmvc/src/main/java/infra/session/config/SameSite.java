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

/**
 * SameSite values.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/11 16:49
 */
public enum SameSite {

  /**
   * Cookies are sent in both first-party and cross-origin requests.
   */
  NONE("None"),

  /**
   * Cookies are sent in a first-party context, also when following a link to the
   * origin site.
   */
  LAX("Lax"),

  /**
   * Cookies are only sent in a first-party context (i.e. not when following a link
   * to the origin site).
   */
  STRICT("Strict");

  private final String attributeValue;

  SameSite(String attributeValue) {
    this.attributeValue = attributeValue;
  }

  public String attributeValue() {
    return this.attributeValue;
  }

}
