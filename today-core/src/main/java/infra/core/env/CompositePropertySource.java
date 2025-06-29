/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core.env;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * Composite {@link PropertySource} implementation that iterates over a set of
 * {@link PropertySource} instances. Necessary in cases where multiple property sources
 * share the same name, e.g. when multiple values are supplied to {@code @PropertySource}.
 *
 * this class extends {@link EnumerablePropertySource} instead
 * of plain {@link PropertySource}, exposing {@link #getPropertyNames()} based on the
 * accumulated property names from all contained sources - and failing with an
 * {@code IllegalStateException} against any non-{@code EnumerablePropertySource}.
 * <b>When used through the {@code EnumerablePropertySource} contract, all contained
 * sources are expected to be of type {@code EnumerablePropertySource} as well.</b>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CompositePropertySource extends EnumerablePropertySource<Object> {

  private final LinkedHashSet<PropertySource<?>> propertySources = new LinkedHashSet<>();

  /**
   * Create a new {@code CompositePropertySource}.
   *
   * @param name the name of the property source
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

  @Override
  public String[] getPropertyNames() {
    ArrayList<String[]> namesList = new ArrayList<>(this.propertySources.size());
    int total = 0;
    for (PropertySource<?> propertySource : this.propertySources) {
      if (!(propertySource instanceof EnumerablePropertySource<?> enumerable)) {
        throw new IllegalStateException(
                "Failed to enumerate property names due to non-enumerable property source: " + propertySource);
      }
      String[] names = enumerable.getPropertyNames();
      namesList.add(names);
      total += names.length;
    }
    LinkedHashSet<String> allNames = new LinkedHashSet<>(total);
    for (String[] names : namesList) {
      for (String name : names) {
        allNames.add(name);
      }
    }
    return StringUtils.toStringArray(allNames);
  }

  /**
   * Add the given {@link PropertySource} to the end of the chain.
   *
   * @param propertySource the PropertySource to add
   */
  public void addPropertySource(@Nullable PropertySource<?> propertySource) {
    if (propertySource != null) {
      this.propertySources.add(propertySource);
    }
  }

  /**
   * Add the given {@link PropertySource} to the start of the chain.
   *
   * @param propertySource the PropertySource to add
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
    return "%s {name='%s', propertySources=%s}".formatted(getClass().getSimpleName(), this.name, this.propertySources);
  }

}
