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
package cn.taketoday.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Holder containing one or more {@link PropertyValue} objects,
 * typically comprising one update for a specific target bean.
 * <p>
 * this implementation Allows simple manipulation of properties,
 * and provides constructors to support deep copy and construction from a Map.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/1 14:49
 */
public class PropertyValues implements Iterable<PropertyValue> {

  @Nullable
  private ArrayList<PropertyValue> propertyValues;

  /**
   * Creates a new empty PropertyValues object.
   * <p>Property values can be added with the {@code add} method.
   *
   * @see #add(String, Object)
   */
  public PropertyValues() { }

  /**
   * Deep copy constructor. Guarantees PropertyValue references
   * are independent, although it can't deep copy objects currently
   * referenced by individual PropertyValue objects.
   *
   * @param original the PropertyValues to copy
   * @see #set(PropertyValues)
   */
  public PropertyValues(@Nullable PropertyValues original) {
    // We can optimize this because it's all new:
    // There is no replacement of existing property values.
    if (original != null) {
      set(original);
    }
  }

  /**
   * Construct a new PropertyValues object from a Map.
   *
   * @param original a Map with property values keyed by property name Strings
   * @see #set(Map)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public PropertyValues(@Nullable Map original) {
    set(original);
  }

  /**
   * Construct a new PropertyValues object using the given List of
   * PropertyValue objects as-is.
   * <p>This is a constructor for advanced usage scenarios.
   * It is not intended for typical programmatic use.
   *
   * @param propertyValueList a List of PropertyValue objects
   */
  public PropertyValues(@Nullable List<PropertyValue> propertyValueList) {
    set(propertyValueList);
  }

  /**
   * Return the number of PropertyValue entries in the list.
   */
  public int size() {
    return propertyValues == null ? 0 : propertyValues().size();
  }

  /**
   * Remove the given PropertyValue, if contained.
   *
   * @param pv the PropertyValue to remove
   */
  public void remove(PropertyValue pv) {
    if (propertyValues != null) {
      propertyValues.remove(pv);
    }
  }

  /**
   * Overloaded version of {@code removePropertyValue} that takes a property name.
   *
   * @param propertyName name of the property
   * @see #remove(PropertyValue)
   */
  public void remove(String propertyName) {
    if (propertyValues != null) {
      PropertyValue propertyValue = get(propertyName);
      if (propertyValue != null) {
        propertyValues.remove(propertyValue);
      }
    }
  }

  /**
   * @param propertyName the name to search for
   * @return the raw property value, or {@code null} if none found
   * @see #getPropertyValue(String)
   * @see PropertyValue#getValue()
   */
  @Nullable
  public Object getPropertyValue(String propertyName) {
    PropertyValue pv = get(propertyName);
    return pv != null ? pv.getValue() : null;
  }

  /**
   * Get the raw property value, if any.
   *
   * @param propertyName the name to search for
   * @return the raw property value, or {@code null} if none found
   * @see #getPropertyValue(String)
   * @see PropertyValue#getValue()
   */
  @Nullable
  public PropertyValue get(String propertyName) {
    if (propertyValues != null) {
      for (PropertyValue pv : propertyValues) {
        if (pv.getName().equals(propertyName)) {
          return pv;
        }
      }
    }
    return null;
  }

  /**
   * Return the changes since the previous PropertyValues.
   * Subclasses should also override {@code equals}.
   *
   * @param old the old property values
   * @return the updated or new properties.
   * Return empty PropertyValues if there are no changes.
   * @see Object#equals
   */
  public PropertyValues changesSince(PropertyValues old) {
    PropertyValues changes = new PropertyValues();
    if (old != this && propertyValues != null) {
      // for each property value in the new set
      for (PropertyValue newPv : propertyValues) {
        // if there wasn't an old one, add it
        PropertyValue pvOld = old.get(newPv.getName());
        if (pvOld == null || !pvOld.equals(newPv)) {
          changes.add(newPv);
        }
      }
    }
    return changes;
  }

  /**
   * Is there a property value (or other processing entry) for this property?
   *
   * @param propertyName the name of the property we're interested in
   * @return whether there is a property value for this property
   */
  public boolean contains(String propertyName) {
    return propertyValues != null && getPropertyValue(propertyName) != null;
  }

  /**
   * Does this holder not contain any PropertyValue objects at all?
   */
  public boolean isEmpty() {
    return propertyValues == null || propertyValues.isEmpty();
  }

  /**
   * Add a PropertyValue object, replacing any existing one for the
   * corresponding property or getting merged with it (if applicable).
   *
   * @param propertyName name of the property
   * @param propertyValue value of the property
   * @return this in order to allow for adding multiple property values in a chain
   */
  public PropertyValues add(String propertyName, @Nullable Object propertyValue) {
    Assert.notNull(propertyName, "propertyName must not be null");
    add(new PropertyValue(propertyName, propertyValue));
    return this;
  }

  /**
   * Add a PropertyValue object, replacing any existing one for the
   * corresponding property or getting merged with it (if applicable).
   *
   * @param pvs the PropertyValue array object to add
   * @return this in order to allow for adding multiple property values in a chain
   */
  public PropertyValues add(@Nullable PropertyValue... pvs) {
    if (ObjectUtils.isNotEmpty(pvs)) {
      ArrayList<PropertyValue> propertyValues = propertyValues();
      outer:
      for (PropertyValue pv : pvs) {
        int i = 0;
        for (PropertyValue currentPv : propertyValues) {
          if (currentPv.getName().equals(pv.getName())) {
            pv = mergeIfRequired(pv, currentPv);
            setAt(pv, i);
            continue outer;
          }
          i++;
        }
        propertyValues.add(pv);
      }
    }
    return this;
  }

  /**
   * Merges the value of the supplied 'new' {@link PropertyValue} with that of
   * the current {@link PropertyValue} if merging is supported and enabled.
   *
   * @see Mergeable
   */
  private PropertyValue mergeIfRequired(PropertyValue newPv, PropertyValue currentPv) {
    Object value = newPv.getValue();
    if (value instanceof Mergeable mergeable) {
      if (mergeable.isMergeEnabled()) {
        Object merged = mergeable.merge(currentPv.getValue());
        return new PropertyValue(newPv.getName(), merged);
      }
    }
    return newPv;
  }

  /**
   * Add all property values from the given Map.
   *
   * @param other a Map with property values keyed by property name,
   * which must be a String
   * @return this in order to allow for adding multiple property values in a chain
   * @see Map#putAll(Map)
   */
  public PropertyValues add(@Nullable Map<String, Object> other) {
    if (CollectionUtils.isNotEmpty(other)) {
      ArrayList<PropertyValue> propertyValues = propertyValues();
      for (Map.Entry<String, Object> entry : other.entrySet()) {
        propertyValues.add(new PropertyValue(entry));
      }
    }
    return this;
  }

  private ArrayList<PropertyValue> propertyValues() {
    if (this.propertyValues == null) {
      this.propertyValues = new ArrayList<>();
    }
    return propertyValues;
  }

  /**
   * Copy all given PropertyValues into this object. Guarantees PropertyValue
   * references are independent, although it can't deep copy objects currently
   * referenced by individual PropertyValue objects.
   *
   * @param other the PropertyValues to copy
   * @return this in order to allow for adding multiple property values in a chain
   * @see Map#putAll(Map)
   */
  public PropertyValues add(@Nullable PropertyValues other) {
    if (other != null && CollectionUtils.isNotEmpty(other.propertyValues)) {
      for (PropertyValue pv : other.propertyValues) {
        add(new PropertyValue(pv));
      }
    }
    return this;
  }

  /**
   * Clear and copy all given PropertyValues into this object. Guarantees PropertyValue
   * references are independent, although it can't deep copy objects currently
   * referenced by individual PropertyValue objects.
   *
   * @param other the PropertyValues to copy
   * @return this in order to allow for adding multiple property values in a chain
   * @see Map#putAll(Map)
   */
  public PropertyValues set(PropertyValues other) {
    if (this.propertyValues == null) {
      if (other != null && CollectionUtils.isNotEmpty(other.propertyValues)) {
        this.propertyValues = new ArrayList<>();
        add(other);
      }
    }
    else {
      this.propertyValues.clear();
      add(other);
    }
    return this;
  }

  /**
   * Apply bean' {@link PropertyValue}s
   *
   * @param propertyValues The array of the bean's PropertyValue s
   */
  public PropertyValues set(PropertyValue... propertyValues) {
    if (this.propertyValues == null) {
      if (ObjectUtils.isNotEmpty(propertyValues)) {
        this.propertyValues = new ArrayList<>();
        add(propertyValues);
      }
    }
    else {
      this.propertyValues.clear();
      add(propertyValues);
    }
    return this;
  }

  public PropertyValues set(@Nullable Collection<PropertyValue> propertyValues) {
    if (this.propertyValues != null) {
      this.propertyValues.clear();
    }
    if (CollectionUtils.isNotEmpty(propertyValues)) {
      this.propertyValues = new ArrayList<>(propertyValues);
    }
    return this;
  }

  /** @since 4.0 */
  public PropertyValues set(@Nullable Map<String, Object> propertyValues) {
    if (CollectionUtils.isEmpty(propertyValues)) {
      if (this.propertyValues != null) {
        this.propertyValues.clear();
      }
    }
    else {
      if (this.propertyValues == null) {
        this.propertyValues = new ArrayList<>();
      }
      else {
        this.propertyValues.clear();
      }
      for (Map.Entry<String, Object> entry : propertyValues.entrySet()) {
        this.propertyValues.add(new PropertyValue(entry));
      }
    }
    return this;
  }

  /**
   * Modify a PropertyValue object held in this object.
   * Indexed from 0.
   */
  public void setAt(PropertyValue pv, int i) {
    if (propertyValues == null) {
      propertyValues = new ArrayList<>();
    }
    propertyValues.set(i, pv);
  }

  public void clear() {
    if (propertyValues != null) {
      propertyValues.clear();
    }
  }

  /**
   * Return the underlying map of property-values. The returned Map
   * can be modified directly, although this is not recommended.
   */
  @Nullable
  public Map<String, Object> asMap() {
    if (CollectionUtils.isEmpty(propertyValues)) {
      return Collections.emptyMap();
    }
    LinkedHashMap<String, Object> ret = new LinkedHashMap<>();
    for (PropertyValue propertyValue : propertyValues) {
      ret.put(propertyValue.getName(), propertyValue.getValue());
    }
    return ret;
  }

  public PropertyValue[] toArray() {
    if (CollectionUtils.isEmpty(propertyValues)) {
      return new PropertyValue[0];
    }
    return propertyValues().toArray(new PropertyValue[0]);
  }

  /**
   * Return the underlying List of PropertyValue objects in its raw form.
   */
  public List<PropertyValue> asList() {
    return propertyValues();
  }

  @Override
  public Iterator<PropertyValue> iterator() {
    if (CollectionUtils.isEmpty(propertyValues)) {
      return Collections.emptyIterator();
    }
    return propertyValues.iterator();
  }

  @Override
  public Spliterator<PropertyValue> spliterator() {
    if (CollectionUtils.isEmpty(propertyValues)) {
      return Spliterators.emptySpliterator();
    }
    return propertyValues.spliterator();
  }

  public Stream<PropertyValue> stream() {
    if (CollectionUtils.isEmpty(propertyValues)) {
      return Stream.empty();
    }
    return propertyValues.stream();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(propertyValues);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return this == other
            || (other instanceof PropertyValues propertyValues && Objects.equals(this.propertyValues, propertyValues.propertyValues));
  }

  @Override
  public String toString() {
    PropertyValue[] pvs = toArray();
    if (pvs.length > 0) {
      return "PropertyValues: length=" + pvs.length + "; " + StringUtils.arrayToDelimitedString(pvs, "; ");
    }
    return "PropertyValues: length=0";
  }
}

