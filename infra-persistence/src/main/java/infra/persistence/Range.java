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

package infra.persistence;

import infra.persistence.annotation.Between;

/**
 * An inclusive lower/upper bound pair used by {@link Between @Between} example
 * conditions to render a {@code BETWEEN ? AND ?} predicate.
 *
 * @param lower the lower bound, inclusive
 * @param upper the upper bound, inclusive
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Between
 * @since 5.0
 */
public record Range(Object lower, Object upper) {

  /**
   * Create a range.
   *
   * @param lower the lower bound, inclusive
   * @param upper the upper bound, inclusive
   * @return the range
   */
  public static Range of(Object lower, Object upper) {
    return new Range(lower, upper);
  }

}