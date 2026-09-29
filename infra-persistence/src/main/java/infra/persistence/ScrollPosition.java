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

import java.util.List;

import infra.util.Assert;

/**
 * Identifies an exact position within a scroll result. A scroll query treats the
 * position exclusively: results start <em>after</em> the given position.
 *
 * <p>An {@linkplain #isInitial() initial} position has no cursor and applies no
 * additional filtering. A non-initial position contains one entry for every
 * keyset sort property, in sort order. Each entry carries its property name,
 * value, and direction so that a position cannot silently be reused with a
 * different ordering.
 *
 * <p>An initial position may carry ordering with all values set to {@code null}.
 * A position used to resume scrolling has a non-null value for every entry. The
 * cursor list is copied on construction, but values are not converted; callers
 * restoring a position must supply values of types accepted by the mapped properties.
 *
 * @param cursor the ordered keyset entries, or {@code null} for the initial position
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public record ScrollPosition(@Nullable List<Entry> cursor) {

  public static final ScrollPosition INITIAL = new ScrollPosition(null);

  public ScrollPosition {
    if (cursor != null) {
      Assert.notEmpty(cursor, "Cursor entries are required");
      cursor = List.copyOf(cursor);
      boolean initial = cursor.get(0).value() == null;
      for (Entry entry : cursor) {
        Assert.isTrue((entry.value() == null) == initial,
                "Keyset values must be either all null or all non-null");
      }
    }
  }

  /**
   * Return a keyset position containing the given ordered cursor entries.
   *
   * @param cursor entries in keyset sort order
   * @return a keyset position
   */
  public static ScrollPosition keyset(List<Entry> cursor) {
    Assert.notNull(cursor, "Cursor entries are required");
    return new ScrollPosition(cursor);
  }

  /**
   * Return whether this position marks the start of a scroll operation.
   *
   * @return {@code true} when no position filtering should be applied
   */
  public boolean isInitial() {
    return cursor == null || cursor.get(0).value() == null;
  }

  /**
   * A single value in a keyset position.
   *
   * @param property the mapped entity property name
   * @param value the property value, or {@code null} for an initial position
   * @param direction the ordering direction
   */
  public record Entry(String property, @Nullable Object value, Order direction) {

    public Entry {
      Assert.hasText(property, "Keyset property is required");
      Assert.notNull(direction, "Keyset direction is required");
    }

  }

}
