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

/**
 * Identifies an exact position within a scroll result. A scroll query treats the
 * position exclusively: results start <em>after</em> the given position.
 *
 * <p>An {@linkplain #isInitial() initial} position marks the start of a scroll
 * operation and applies no additional filtering. Positions are pure coordinates:
 * they do not carry the page size or the sort order, both of which are supplied
 * separately to the scroll request.
 *
 * <p>This type is intentionally opaque at the transport layer. It is a
 * structured value and is not encoded into a token or string; clients that need
 * to pass a position across process boundaries should persist the values
 * themselves.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see KeysetScrollPosition
 * @since 5.0
 */
public interface ScrollPosition {

  /**
   * Return the position marking the start of a keyset scroll operation.
   *
   * @return an initial keyset position
   */
  static KeysetScrollPosition keyset() {
    return KeysetScrollPosition.initial();
  }

  /**
   * Return a keyset position for the given key values.
   *
   * @param keys the ordered keyset values, mapped by property name
   * @return a keyset position
   */
  static KeysetScrollPosition keyset(Map<String, Object> keys) {
    return KeysetScrollPosition.of(keys);
  }

  /**
   * Return whether this position marks the start of a scroll operation.
   *
   * @return {@code true} when no position filtering should be applied
   */
  boolean isInitial();

}
