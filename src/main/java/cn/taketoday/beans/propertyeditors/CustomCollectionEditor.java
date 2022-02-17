/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * Property editor for Collections, converting any source Collection
 * to a given target Collection type.
 *
 * <p>By default registered for Set, SortedSet and List,
 * to automatically convert any given Collection to one of those
 * target types if the type does not match the target property.
 *
 * @author Juergen Hoeller
 * @see Collection
 * @see java.util.Set
 * @see SortedSet
 * @see List
 * @since 4.0
 */
public class CustomCollectionEditor extends PropertyEditorSupport {

  @SuppressWarnings("rawtypes")
  private final Class<? extends Collection> collectionType;

  private final boolean nullAsEmptyCollection;

  /**
   * Create a new CustomCollectionEditor for the given target type,
   * keeping an incoming {@code null} as-is.
   *
   * @param collectionType the target type, which needs to be a
   * sub-interface of Collection or a concrete Collection class
   * @see Collection
   * @see ArrayList
   * @see TreeSet
   * @see LinkedHashSet
   */
  @SuppressWarnings("rawtypes")
  public CustomCollectionEditor(Class<? extends Collection> collectionType) {
    this(collectionType, false);
  }

  /**
   * Create a new CustomCollectionEditor for the given target type.
   * <p>If the incoming value is of the given type, it will be used as-is.
   * If it is a different Collection type or an array, it will be converted
   * to a default implementation of the given Collection type.
   * If the value is anything else, a target Collection with that single
   * value will be created.
   * <p>The default Collection implementations are: ArrayList for List,
   * TreeSet for SortedSet, and LinkedHashSet for Set.
   *
   * @param collectionType the target type, which needs to be a
   * sub-interface of Collection or a concrete Collection class
   * @param nullAsEmptyCollection whether to convert an incoming {@code null}
   * value to an empty Collection (of the appropriate type)
   * @see Collection
   * @see ArrayList
   * @see TreeSet
   * @see LinkedHashSet
   */
  @SuppressWarnings("rawtypes")
  public CustomCollectionEditor(Class<? extends Collection> collectionType, boolean nullAsEmptyCollection) {
    Assert.notNull(collectionType, "Collection type is required");
    if (!Collection.class.isAssignableFrom(collectionType)) {
      throw new IllegalArgumentException(
              "Collection type [" + collectionType.getName() + "] does not implement [java.util.Collection]");
    }
    this.collectionType = collectionType;
    this.nullAsEmptyCollection = nullAsEmptyCollection;
  }

  /**
   * Convert the given text value to a Collection with a single element.
   */
  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    setValue(text);
  }

  /**
   * Convert the given value to a Collection of the target type.
   */
  @Override
  public void setValue(@Nullable Object value) {
    if (value == null && this.nullAsEmptyCollection) {
      super.setValue(createCollection(this.collectionType, 0));
    }
    else if (value == null || (this.collectionType.isInstance(value) && !alwaysCreateNewCollection())) {
      // Use the source value as-is, as it matches the target type.
      super.setValue(value);
    }
    else if (value instanceof Collection<?> source) {
      // Convert Collection elements.
      Collection<Object> target = createCollection(this.collectionType, source.size());
      for (Object elem : source) {
        target.add(convertElement(elem));
      }
      super.setValue(target);
    }
    else if (value.getClass().isArray()) {
      // Convert array elements to Collection elements.
      int length = Array.getLength(value);
      Collection<Object> target = createCollection(this.collectionType, length);
      for (int i = 0; i < length; i++) {
        target.add(convertElement(Array.get(value, i)));
      }
      super.setValue(target);
    }
    else {
      // A plain value: convert it to a Collection with a single element.
      Collection<Object> target = createCollection(this.collectionType, 1);
      target.add(convertElement(value));
      super.setValue(target);
    }
  }

  /**
   * Create a Collection of the given type, with the given
   * initial capacity (if supported by the Collection type).
   *
   * @param collectionType a sub-interface of Collection
   * @param initialCapacity the initial capacity
   * @return the new Collection instance
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected Collection<Object> createCollection(Class<? extends Collection> collectionType, int initialCapacity) {
    if (!collectionType.isInterface()) {
      try {
        return ReflectionUtils.accessibleConstructor(collectionType).newInstance();
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException(
                "Could not instantiate collection class: " + collectionType.getName(), ex);
      }
    }
    else if (List.class == collectionType) {
      return new ArrayList<>(initialCapacity);
    }
    else if (SortedSet.class == collectionType) {
      return new TreeSet<>();
    }
    else {
      return new LinkedHashSet<>(initialCapacity);
    }
  }

  /**
   * Return whether to always create a new Collection,
   * even if the type of the passed-in Collection already matches.
   * <p>Default is "false"; can be overridden to enforce creation of a
   * new Collection, for example to convert elements in any case.
   *
   * @see #convertElement
   */
  protected boolean alwaysCreateNewCollection() {
    return false;
  }

  /**
   * Hook to convert each encountered Collection/array element.
   * The default implementation simply returns the passed-in element as-is.
   * <p>Can be overridden to perform conversion of certain elements,
   * for example String to Integer if a String array comes in and
   * should be converted to a Set of Integer objects.
   * <p>Only called if actually creating a new Collection!
   * This is by default not the case if the type of the passed-in Collection
   * already matches. Override {@link #alwaysCreateNewCollection()} to
   * enforce creating a new Collection in every case.
   *
   * @param element the source element
   * @return the element to be used in the target Collection
   * @see #alwaysCreateNewCollection()
   */
  protected Object convertElement(Object element) {
    return element;
  }

  /**
   * This implementation returns {@code null} to indicate that
   * there is no appropriate text representation.
   */
  @Override
  @Nullable
  public String getAsText() {
    return null;
  }

}
