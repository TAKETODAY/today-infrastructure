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
   * @param propertySource the PropertySource to add
   */
  public void addPropertySource(PropertySource<?> propertySource) {
    this.propertySources.add(propertySource);
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
    return getClass().getSimpleName() +
            " {name='" + this.name + "', propertySources=" + this.propertySources + "}";
  }

}
