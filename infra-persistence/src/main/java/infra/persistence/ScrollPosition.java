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

import java.util.Map;
import java.util.Objects;

import infra.util.Assert;

/**
 * Identifies an exact position within a scroll result. A scroll query treats the
 * position exclusively: results start <em>after</em> the given position.
 *
 * <p>An {@linkplain #isInitial() initial} position has an empty key map and applies
 * no additional filtering. Positions are pure coordinates:
 * they do not carry the page size or the sort order, both of which are supplied
 * separately to the scroll request.
 *
 * <p>Keys are mapped entity property names used by the keyset order. Values must
 * be non-null for keyset comparison. The map is copied on construction, but
 * values are not converted; callers restoring a position must supply values of
 * types accepted by the mapped properties.
 *
 * <p>This type is intentionally opaque at the transport layer. It is a
 * structured value and is not encoded into a token or string; clients that need
 * to pass a position across process boundaries should persist the values
 * themselves.
 *
 * @param keys the keyset values mapped by property name, never {@code null}
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public record ScrollPosition(Map<String, Object> keys) {

  private static final ScrollPosition INITIAL = new ScrollPosition(Map.of());

  public ScrollPosition {
    Assert.notNull(keys, "Keys are required");
    if (keys.values().stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Keyset values must not be null");
    }
    keys = Map.copyOf(keys);
  }

  /**
   * Return the position marking the start of a keyset scroll operation.
   *
   * @return an initial keyset position
   */
  public static ScrollPosition keyset() {
    return INITIAL;
  }

  /**
   * Return a keyset position for the given key values.
   *
   * @param keys the ordered keyset values, mapped by property name
   * @return a keyset position
   */
  public static ScrollPosition keyset(Map<String, Object> keys) {
    Assert.notNull(keys, "Keys are required");
    return keys.isEmpty() ? INITIAL : new ScrollPosition(keys);
  }

  /**
   * Return whether this position marks the start of a scroll operation.
   *
   * @return {@code true} when no position filtering should be applied
   */
  public boolean isInitial() {
    return keys.isEmpty();
  }

}
