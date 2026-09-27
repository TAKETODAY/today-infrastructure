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
 * A {@link ScrollPosition} that resumes scrolling from the sort keys of the last
 * element in a {@link Scroll}.
 *
 * <p>The map keys are the mapped entity property names used by the keyset order,
 * and the values are that last element's values for those properties. Values must
 * be non-null: keyset comparison relies on the database's ordering operators,
 * which handle {@code null} inconsistently.
 *
 * <p>An empty map denotes the {@linkplain #initial() initial} position. The map is
 * copied on construction, but its values are not converted; callers that persist
 * and restore a position are responsible for restoring values to types accepted
 * by the mapped properties.
 *
 * @param keys the keyset values mapped by property name, never {@code null}
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public record KeysetScrollPosition(Map<String, Object> keys) implements ScrollPosition {

  private static final KeysetScrollPosition INITIAL = new KeysetScrollPosition(Map.of());

  public KeysetScrollPosition {
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
  public static KeysetScrollPosition initial() {
    return INITIAL;
  }

  /**
   * Return a keyset position for the given key values.
   *
   * @param keys the keyset values mapped by property name
   * @return a keyset position, or the initial position when the map is empty
   */
  public static KeysetScrollPosition of(Map<String, Object> keys) {
    return keys.isEmpty() ? INITIAL : new KeysetScrollPosition(keys);
  }

  @Override
  public boolean isInitial() {
    return keys.isEmpty();
  }
}
