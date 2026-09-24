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

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A forward-only keyset page request. The named entity property is ordered first;
 * the entity ID is appended as a unique tie-breaker. Cursor values must be non-null.
 *
 * @param property the entity property to sort by
 * @param order the sort direction
 * @param pageSize the maximum number of rows, greater than zero
 * @param cursor values returned by {@link KeysetPage#nextCursor()}, or null for the first page
 * @since 5.0
 */
public record KeysetPageable(String property, Order order, int pageSize, @Nullable Map<String, ?> cursor) {

  public KeysetPageable {
    if (property == null || property.isBlank() || order == null || pageSize < 1 || pageSize == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("A property, direction and page size between 1 and Integer.MAX_VALUE - 1 are required");
    }
    if (cursor != null) {
      cursor = Map.copyOf(cursor);
    }
  }

  /** Return a request for the first page. */
  public static KeysetPageable first(String property, Order order, int pageSize) {
    return new KeysetPageable(property, order, pageSize, null);
  }

  /** Return a request for the page following the given cursor. */
  public KeysetPageable after(@Nullable Map<String, ?> cursor) {
    return new KeysetPageable(property, order, pageSize, cursor);
  }
}
