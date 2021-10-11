/*
 * Copyright 2002-2017 the original author or authors.
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

package cn.taketoday.core.env;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * Composite {@link PropertySource} implementation that iterates over a set of
 * {@link PropertySource} instances. Necessary in cases where multiple property sources
 * share the same name, e.g. when multiple values are supplied to {@code @PropertySource}.
 *
 * <p> instead of plain {@link PropertySource}, exposing {@link #getPropertyNames()} based on the
 * accumulated property names from all contained sources (as far as possible).
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 4.0
 */
public class CompositePropertySource extends EnumerablePropertySource<Object> {

  private final LinkedHashSet<PropertySource<?>> propertySources = new LinkedHashSet<>();

  /**
   * Create a new {@code CompositePropertySource}.
   *
   * @param name
   *         the name of the property source
   */
  public CompositePropertySource(String name) {
    super(name);
  }

  @Override
  @Nullable
  public Object getProperty(String name) {
    for (PropertySource<?> propertySource : this.propertySources) {
      Object candidate = propertySource.getProperty(name);
      if (candidate != null) {
        return candidate;
      }
    }
    return null;
  }

  @Override
  public boolean containsProperty(String name) {
    for (PropertySource<?> propertySource : this.propertySources) {
      if (propertySource.containsProperty(name)) {
        return true;
      }
    }
    return false;
  }

  @NonNull
  @Override
  public LinkedHashSet<String> getPropertyNames() {
    LinkedHashSet<String> names = new LinkedHashSet<>();
    for (PropertySource<?> propertySource : this.propertySources) {
      if (!(propertySource instanceof EnumerablePropertySource)) {
        throw new IllegalStateException(
                "Failed to enumerate property names due to non-enumerable property source: " + propertySource);
      }
      Collection<String> propertyNames = ((EnumerablePropertySource<?>) propertySource).getPropertyNames();
      names.addAll(propertyNames);
    }
    return names;
  }

  /**
   * Add the given {@link PropertySource} to the end of the chain.
   *
   * @param propertySource
   *         the PropertySource to add
   */
  public void addPropertySource(PropertySource<?> propertySource) {
    this.propertySources.add(propertySource);
  }

  /**
   * Add the given {@link PropertySource} to the start of the chain.
   *
   * @param propertySource
   *         the PropertySource to add
   */
  public void addFirstPropertySource(PropertySource<?> propertySource) {
    ArrayList<PropertySource<?>> existing = new ArrayList<>(this.propertySources);
    this.propertySources.clear();
    this.propertySources.add(propertySource);
    this.propertySources.addAll(existing);
  }

  /**
   * Return all property sources that this composite source holds.
   */
  public LinkedHashSet<PropertySource<?>> getPropertySources() {
    return this.propertySources;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
            " {name='" + this.name + "', propertySources=" + this.propertySources + "}";
  }

}
