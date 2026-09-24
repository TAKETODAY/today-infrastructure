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
 * A forward-only keyset page request. Ordering comes from the query condition
 * (or entity metadata); when present, the entity ID is appended as a unique
 * tie-breaker. Without an ID, the declared ordering must uniquely identify rows.
 *
 * <p>Cursor values are the non-null entity property values returned by
 * {@link KeysetPage#nextCursor()}. Keep the same ordering and filtering conditions
 * when following a cursor. The cursor map is copied,
 * but its values are not converted or copied; callers transferring cursors across
 * process boundaries must restore values to types accepted by the mapped properties.
 *
 * @param pageSize the maximum number of rows, greater than zero
 * @param cursor values returned by {@link KeysetPage#nextCursor()}, or {@code null} for the first page
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public record KeysetPageable(int pageSize, @Nullable Map<String, ?> cursor) {

  public KeysetPageable {
    if (pageSize < 1 || pageSize == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Page size must be between 1 and Integer.MAX_VALUE - 1");
    }
    if (cursor != null) {
      if (cursor.isEmpty() || cursor.values().stream().anyMatch(value -> value == null)) {
        throw new IllegalArgumentException("Cursor must contain non-null property values");
      }
      cursor = Map.copyOf(cursor);
    }
  }

  /**
   * Return a request for the first page.
   *
   * @param pageSize the number of rows per page
   * @return a request without a cursor
   */
  public static KeysetPageable first(int pageSize) {
    return new KeysetPageable(pageSize, null);
  }

  /**
   * Return a request for the page following the given cursor.
   *
   * @param cursor the non-null cursor returned by a page with more rows
   * @return a request for the next page
   * @throws IllegalArgumentException if the cursor is {@code null}
   */
  public KeysetPageable after(@Nullable Map<String, ?> cursor) {
    if (cursor == null) {
      throw new IllegalArgumentException("No next cursor is available");
    }
    return new KeysetPageable(pageSize, cursor);
  }
}
