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

package infra.core.env;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Map PropertyResolver implementation
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/10/3 15:04
 */
public class MapPropertyResolver extends TypedPropertyResolver implements IterablePropertyResolver {

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

  @Override
  public <T> @Nullable T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
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
