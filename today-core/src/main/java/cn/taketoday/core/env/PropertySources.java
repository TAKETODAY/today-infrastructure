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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;

/**
 * Holder containing one or more {@link PropertySource} objects.
 * <p>
 * Allows manipulation of contained property sources and provides a constructor
 * for copying an existing {@code PropertySources} instance.
 *
 * <p>Where <em>precedence</em> is mentioned in methods such as {@link #addFirst}
 * and {@link #addLast}, this is with regard to the order in which property sources
 * will be searched when resolving a given property with a {@link PropertyResolver}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author TODAY
 * @see PropertySourcesPropertyResolver
 * @see PropertySource
 * @since 4.0
 */
public class PropertySources implements Iterable<PropertySource<?>> {

  private final CopyOnWriteArrayList<PropertySource<?>> propertySourceList
          = new CopyOnWriteArrayList<>();

  /**
   * Create a new {@link PropertySources} object.
   */
  public PropertySources() { }

  /**
   * Create a new {@code PropertySources} from the given propertySources
   * object, preserving the original order of contained {@code PropertySource} objects.
   */
  public PropertySources(PropertySources propertySources) {
    for (PropertySource<?> propertySource : propertySources) {
      addLast(propertySource);
    }
  }

  @Override
  public Iterator<PropertySource<?>> iterator() {
    return this.propertySourceList.iterator();
  }

  @Override
  public Spliterator<PropertySource<?>> spliterator() {
    return propertySourceList.spliterator();
  }

  /**
   * Return a sequential {@link Stream} containing the property sources.
   */
  public Stream<PropertySource<?>> stream() {
    return this.propertySourceList.stream();
  }

  /**
   * Return whether a property source with the given name is contained.
   *
   * @param name the {@linkplain PropertySource#getName() name of the property source} to find
   */
  public boolean contains(String name) {
    for (PropertySource<?> propertySource : this.propertySourceList) {
      if (propertySource.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the property source with the given name, {@code null} if not found.
   *
   * @param name the {@linkplain PropertySource#getName() name of the property source} to find
   */
  @Nullable
  public PropertySource<?> get(String name) {
    for (PropertySource<?> propertySource : this.propertySourceList) {
      if (propertySource.getName().equals(name)) {
        return propertySource;
      }
    }
    return null;
  }

  /**
   * Add the given property source object with highest precedence.
   */
  public void addFirst(PropertySource<?> propertySource) {
    synchronized(this.propertySourceList) {
      removeIfPresent(propertySource);
      this.propertySourceList.add(0, propertySource);
    }
  }

  /**
   * Add the given property source object with lowest precedence.
   */
  public void addLast(PropertySource<?> propertySource) {
    synchronized(this.propertySourceList) {
      removeIfPresent(propertySource);
      this.propertySourceList.add(propertySource);
    }
  }

  /**
   * Add the given property source object with precedence immediately higher
   * than the named relative property source.
   */
  public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
    assertLegalRelativeAddition(relativePropertySourceName, propertySource);
    synchronized(this.propertySourceList) {
      removeIfPresent(propertySource);
      int index = assertPresentAndGetIndex(relativePropertySourceName);
      addAtIndex(index, propertySource);
    }
  }

  /**
   * Add the given property source object with precedence immediately lower
   * than the named relative property source.
   */
  public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
    assertLegalRelativeAddition(relativePropertySourceName, propertySource);
    synchronized(this.propertySourceList) {
      removeIfPresent(propertySource);
      int index = assertPresentAndGetIndex(relativePropertySourceName);
      addAtIndex(index + 1, propertySource);
    }
  }

  /**
   * Return the precedence of the given property source, {@code -1} if not found.
   */
  public int precedenceOf(PropertySource<?> propertySource) {
    return this.propertySourceList.indexOf(propertySource);
  }

  /**
   * Remove and return the property source with the given name, {@code null} if not found.
   *
   * @param name the name of the property source to find and remove
   */
  @Nullable
  public PropertySource<?> remove(String name) {
    synchronized(this.propertySourceList) {
      int index = this.propertySourceList.indexOf(PropertySource.named(name));
      return (index != -1 ? this.propertySourceList.remove(index) : null);
    }
  }

  /**
   * Replace the property source with the given name with the given property source object.
   *
   * @param name the name of the property source to find and replace
   * @param propertySource the replacement property source
   * @throws IllegalArgumentException if no property source with the given name is present
   * @see #contains
   */
  public void replace(String name, PropertySource<?> propertySource) {
    synchronized(this.propertySourceList) {
      int index = assertPresentAndGetIndex(name);
      this.propertySourceList.set(index, propertySource);
    }
  }

  /**
   * Return the number of {@link PropertySource} objects contained.
   */
  public int size() {
    return this.propertySourceList.size();
  }

  @Override
  public String toString() {
    return this.propertySourceList.toString();
  }

  /**
   * Ensure that the given property source is not being added relative to itself.
   */
  protected void assertLegalRelativeAddition(
          String relativePropertySourceName, PropertySource<?> propertySource) {
    String newPropertySourceName = propertySource.getName();
    if (relativePropertySourceName.equals(newPropertySourceName)) {
      throw new IllegalArgumentException(
              "PropertySource named '" + newPropertySourceName + "' cannot be added relative to itself");
    }
  }

  /**
   * Remove the given property source if it is present.
   */
  protected void removeIfPresent(PropertySource<?> propertySource) {
    this.propertySourceList.remove(propertySource);
  }

  /**
   * Add the given property source at a particular index in the list.
   */
  private void addAtIndex(int index, PropertySource<?> propertySource) {
    removeIfPresent(propertySource);
    this.propertySourceList.add(index, propertySource);
  }

  /**
   * Assert that the named property source is present and return its index.
   *
   * @param name {@linkplain PropertySource#getName() name of the property source} to find
   * @throws IllegalArgumentException if the named property source is not present
   */
  private int assertPresentAndGetIndex(String name) {
    int index = this.propertySourceList.indexOf(PropertySource.named(name));
    if (index == -1) {
      throw new IllegalArgumentException("PropertySource named '" + name + "' does not exist");
    }
    return index;
  }

}
