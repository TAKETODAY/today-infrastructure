/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.util;

import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of {@link MultiValueMap} that wraps a {@link java.util.LinkedHashMap},
 * storing multiple values in an {@link java.util.ArrayList}.
 *
 * <p>This Map implementation is generally not thread-safe. It is primarily designed
 * for data structures exposed from request objects, for use in a single thread only.
 *
 * @param <K> the key type
 * @param <V> the value element type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/17 16:33
 */
public class LinkedMultiValueMap<K, V> extends MultiValueMapAdapter<K, V> {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Create a new LinkedMultiValueMap that wraps a {@link LinkedHashMap}.
   */
  public LinkedMultiValueMap() {
    super(new LinkedHashMap<>());
  }

  /**
   * Create a new LinkedMultiValueMap that wraps a {@link LinkedHashMap}
   * with an initial capacity that can accommodate the specified number of
   * elements without any immediate resize/rehash operations to be expected.
   *
   * @param expectedSize the expected number of elements (with a corresponding
   * capacity to be derived so that no resize/rehash operations are needed)
   * @see CollectionUtils#newLinkedHashMap(int)
   */
  public LinkedMultiValueMap(int expectedSize) {
    super(CollectionUtils.newLinkedHashMap(expectedSize));
  }

  /**
   * Copy constructor: Create a new LinkedMultiValueMap with the same mappings as
   * the specified Map. Note that this will be a shallow copy; its value-holding
   * List entries will get reused and therefore cannot get modified independently.
   *
   * @param otherMap the Map whose mappings are to be placed in this Map
   * @see #clone()
   * @see #deepCopy()
   */
  public LinkedMultiValueMap(Map<K, List<V>> otherMap) {
    super(new LinkedHashMap<>(otherMap));
  }

  /**
   * Create a deep copy of this Map.
   *
   * @return a copy of this Map, including a copy of each value-holding List entry
   * (consistently using an independent modifiable {@link ArrayList} for each entry)
   * along the lines of {@code MultiValueMap.addAll} semantics
   * @see #addAll(Map)
   * @since 2.1.7
   */
  public LinkedMultiValueMap<K, V> deepCopy() {
    LinkedMultiValueMap<K, V> ret = new LinkedMultiValueMap<>(targetMap.size());
    for (Entry<K, List<V>> entry : targetMap.entrySet()) {
      ret.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return ret;
  }

}
