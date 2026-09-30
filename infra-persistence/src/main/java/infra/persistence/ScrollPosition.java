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

import java.util.ArrayList;
import java.util.List;

import infra.util.Assert;

/**
 * Identifies an exact position within a scroll result. A scroll query treats the
 * position exclusively: results start <em>after</em> the given position.
 *
 * <p>A {@code null} position starts a scroll without additional filtering.
 * A position contains one entry for every keyset sort property, in sort order.
 * Each entry carries its property name,
 * value, and direction so that a position cannot silently be reused with a
 * different ordering.
 *
 * <p>An initial position may carry ordering with all values set to {@code null}.
 * A position used to resume scrolling has a non-null value for every entry. The
 * cursor list is copied on construction, but values are not converted.
 *
 * <p>This type is an internal query coordinate, not a transport DTO. Applications
 * should carry typed cursor properties in their example object and construct a
 * position through {@link ScrollPositionSource}, or annotate those properties
 * with {@link infra.persistence.annotation.OrderBy @OrderBy} for automatic
 * construction.
 *
 * @param cursor the non-empty ordered keyset entries
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ScrollPositionSource
 * @since 5.0
 */
public record ScrollPosition(List<Entry> cursor) {

  public ScrollPosition {
    Assert.notEmpty(cursor, "Cursor entries are required");
    cursor = List.copyOf(cursor);
    boolean initial = cursor.get(0).value() == null;
    for (Entry entry : cursor) {
      Assert.isTrue((entry.value() == null) == initial,
              "Keyset values must be either all null or all non-null");
    }
  }

  /**
   * Return whether this position only declares ordering and has no cursor values.
   *
   * @return {@code true} when no position filtering should be applied
   */
  public boolean isInitial() {
    return cursor.get(0).value() == null;
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
   * Create a builder for an ordered keyset position. Add keys in sort order.
   *
   * @return a new keyset position builder
   */
  public static Builder builder() {
    return new Builder();
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

  /**
   * Builds a keyset position from mapped property names, values, and directions.
   */
  public static final class Builder {

    private final ArrayList<Entry> entries = new ArrayList<>();

    private Builder() {
    }

    /**
     * Append an ascending key.
     *
     * @param property the mapped entity property name
     * @param value the non-null cursor value
     * @return this builder
     */
    public Builder asc(String property, @Nullable Object value) {
      return add(property, value, Order.ASC);
    }

    /**
     * Append a descending key.
     *
     * @param property the mapped entity property name
     * @param value the non-null cursor value
     * @return this builder
     */
    public Builder desc(String property, @Nullable Object value) {
      return add(property, value, Order.DESC);
    }

    /**
     * Append a key with the given direction.
     *
     * @param property the mapped entity property name
     * @param value the non-null cursor value
     * @param direction the sort direction
     * @return this builder
     */
    public Builder add(String property, @Nullable Object value, Order direction) {
      entries.add(new Entry(property, value, direction));
      return this;
    }

    /**
     * Create an immutable position from the keys in insertion order.
     *
     * @return the keyset position
     * @throws IllegalArgumentException if no keys are present
     */
    public ScrollPosition build() {
      return ScrollPosition.keyset(entries);
    }

  }

}
