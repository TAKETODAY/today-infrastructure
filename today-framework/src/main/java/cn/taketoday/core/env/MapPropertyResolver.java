/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.env;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Map PropertyResolver implementation
 *
 * @author TODAY 2021/10/3 15:04
 * @since 4.0
 */
public class MapPropertyResolver
        extends TypedPropertyResolver implements IterablePropertyResolver {

  private static final Logger log = LoggerFactory.getLogger(MapPropertyResolver.class);

  @Nullable
  private final Map<String, Object> keyValues;

  /**
   * Create a new resolver against the given Map.
   *
   * @param keyValues the Map source (without {@code null} values in order to get
   * consistent {@link #getProperty} and {@link #containsProperty} behavior)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public MapPropertyResolver(@Nullable Map keyValues) {
    this.keyValues = keyValues;
  }

  @Override
  public boolean containsProperty(String key) {
    if (keyValues != null) {
      return keyValues.containsKey(key);
    }
    return false;
  }

  @Nullable
  public <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
    if (this.keyValues != null) {
      if (log.isTraceEnabled()) {
        log.trace("Searching for key '{}' in map '{}'", key, keyValues);
      }
      Object value = keyValues.get(key);
      if (value != null) {
        if (resolveNestedPlaceholders && value instanceof String) {
          value = resolveNestedPlaceholders((String) value);
        }
        logKeyFound(key, value);
        return convertValueIfNecessary(value, targetValueType);
      }
    }
    if (log.isTraceEnabled()) {
      log.trace("Could not find key '{}' in any property source", key);
    }
    return null;
  }

  /**
   * Log the given key as found in the given {@link Map}, resulting in
   * the given value.
   * <p>The default implementation writes a debug log message with key and source.
   * this does not log the value anymore in order to avoid accidental logging of
   * sensitive settings. Subclasses may override this method to change the log
   * level and/or log message, including the property's value if desired.
   *
   * @param key the key found
   * @param value the corresponding value
   */
  protected void logKeyFound(String key, Object value) {
    if (log.isDebugEnabled()) {
      log.debug("Found key '{}' with value of type {}", key, value.getClass().getSimpleName());
    }
  }

  //---------------------------------------------------------------------
  // Implementation of IterablePropertyResolver interface
  //---------------------------------------------------------------------

  @Override
  public Iterator<String> iterator() {
    if (keyValues != null) {
      return keyValues.keySet().iterator();
    }
    return Collections.emptyIterator();
  }

  @Override
  public void forEach(Consumer<? super String> action) {
    if (keyValues != null) {
      for (String key : keyValues.keySet()) {
        action.accept(key);
      }
    }
  }

  @Override
  public Spliterator<String> spliterator() {
    if (keyValues != null) {
      return keyValues.keySet().spliterator();
    }
    return Spliterators.emptySpliterator();
  }
}
