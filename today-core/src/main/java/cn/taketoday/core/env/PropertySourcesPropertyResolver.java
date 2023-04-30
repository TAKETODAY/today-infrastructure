/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * {@link PropertyResolver} implementation that resolves property values against
 * an underlying set of {@link PropertySources}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertySource
 * @see PropertySources
 * @see AbstractEnvironment
 * @since 4.0
 */
public class PropertySourcesPropertyResolver
        extends TypedPropertyResolver implements IterablePropertyResolver {

  private static final Logger log = LoggerFactory.getLogger(PropertySourcesPropertyResolver.class);

  @Nullable
  private final PropertySources propertySources;

  /**
   * Create a new resolver against the given property sources.
   *
   * @param propertySources the set of {@link PropertySource} objects to use
   */
  public PropertySourcesPropertyResolver(@Nullable PropertySources propertySources) {
    this.propertySources = propertySources;
  }

  @Override
  public boolean containsProperty(String key) {
    if (this.propertySources != null) {
      for (PropertySource<?> propertySource : this.propertySources) {
        if (propertySource.containsProperty(key)) {
          return true;
        }
      }
    }
    return false;
  }

  @Nullable
  public <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
    boolean traceEnabled = log.isTraceEnabled();
    if (this.propertySources != null) {
      for (PropertySource<?> propertySource : this.propertySources) {
        if (traceEnabled) {
          log.trace("Searching for key '{}' in PropertySource '{}'", key, propertySource.getName());
        }
        Object value = propertySource.getProperty(key);
        if (value != null) {
          if (resolveNestedPlaceholders && value instanceof String) {
            value = resolveNestedPlaceholders((String) value);
          }
          logKeyFound(key, propertySource, value);
          return convertValueIfNecessary(value, targetValueType);
        }
      }
    }
    if (traceEnabled) {
      log.trace("Could not find key '{}' in any property source", key);
    }
    return null;
  }

  /**
   * Log the given key as found in the given {@link PropertySource}, resulting in
   * the given value.
   * <p>The default implementation writes a debug log message with key and source.
   * this does not log the value anymore in order to avoid accidental logging of
   * sensitive settings. Subclasses may override this method to change the log
   * level and/or log message, including the property's value if desired.
   *
   * @param key the key found
   * @param propertySource the {@code PropertySource} that the key has been found in
   * @param value the corresponding value
   */
  protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
    if (log.isDebugEnabled()) {
      log.debug("Found key '{}' in PropertySource '{}' with value of type {}",
              key, propertySource.getName(), value.getClass().getSimpleName());
    }
  }

  public ArrayList<String> getPropertyNames() {
    ArrayList<String> ret = new ArrayList<>();
    if (propertySources != null) {
      for (PropertySource<?> propertySource : propertySources) {
        if (propertySource instanceof EnumerablePropertySource<?> source) {
          String[] propertyNames = source.getPropertyNames();
          CollectionUtils.addAll(ret, propertyNames);
        }
      }
    }
    return ret;
  }

  //---------------------------------------------------------------------
  // Implementation of IterablePropertyResolver interface
  //---------------------------------------------------------------------

  @Override
  public Iterator<String> iterator() {
    return getPropertyNames().iterator();
  }

  @Override
  public void forEach(Consumer<? super String> action) {
    for (String key : getPropertyNames()) {
      action.accept(key);
    }
  }

  @Override
  public Spliterator<String> spliterator() {
    return getPropertyNames().spliterator();
  }

}
