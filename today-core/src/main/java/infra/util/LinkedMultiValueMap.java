/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
