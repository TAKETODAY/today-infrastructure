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

package infra.lang;

/**
 * Provides a human-readable description for display, logging, or diagnostics.
 *
 * <p>For example, a product can describe itself by name:
 * <pre>{@code
 * record Product(String name) implements Descriptive {
 *   @Override
 *   public String getDescription() {
 *     return "Product: " + name;
 *   }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Enumerable
 * @since 4.0 2022/8/1 17:18
 */
public interface Descriptive {

  /**
   * Return a human-readable description of this object. The implementation
   * determines the content and format, so callers should not rely on a
   * particular format unless it is documented by that implementation.
   *
   * @return the description (never {@code null})
   */
  String getDescription();

}
