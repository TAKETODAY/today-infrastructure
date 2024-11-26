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
package infra.retry.policy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import infra.retry.RetryContext;

/**
 * Map-based implementation of {@link RetryContextCache}. The map backing the cache of
 * contexts is synchronized.
 *
 * @author Dave Syer
 * @since 4.0
 */
public class MapRetryContextCache implements RetryContextCache {

  /**
   * Default value for maximum capacity of the cache. This is set to a reasonably low
   * value (4096) to avoid users inadvertently filling the cache with item keys that are
   * inconsistent.
   */
  public static final int DEFAULT_CAPACITY = 4096;

  private final Map<Object, RetryContext> map = Collections.synchronizedMap(new HashMap<>());

  private int capacity;

  /**
   * Create a {@link MapRetryContextCache} with default capacity.
   */
  public MapRetryContextCache() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * @param defaultCapacity the default capacity
   */
  public MapRetryContextCache(int defaultCapacity) {
    super();
    this.capacity = defaultCapacity;
  }

  /**
   * Public setter for the capacity. Prevents the cache from growing unboundedly if
   * items that fail are misidentified and two references to an identical item actually
   * do not have the same key. This can happen when users implement equals and hashCode
   * based on mutable fields, for instance.
   *
   * @param capacity the capacity to set
   */
  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  public RetryContext get(Object key) {
    return map.get(key);
  }

  public void put(Object key, RetryContext context) {
    if (map.size() >= capacity) {
      throw new RetryCacheCapacityExceededException("Retry cache capacity limit breached. "
              + "Do you need to re-consider the implementation of the key generator, "
              + "or the equals and hashCode of the items that failed?");
    }
    map.put(key, context);
  }

  public void remove(Object key) {
    map.remove(key);
  }

}
