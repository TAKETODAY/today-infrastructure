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

package infra.beans.factory.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.beans.BeanMetadataElement;
import infra.beans.Mergeable;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;

/**
 * Holder for constructor argument values, typically as part of a bean definition.
 *
 * <p>Supports values for a specific index in the constructor argument list
 * as well as for generic argument matches by type.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanDefinition#getConstructorArgumentValues
 * @since 4.0 2022/1/6 20:38
 */
public class ConstructorArgumentValues {

  private final ArrayList<ValueHolder> genericArgumentValues = new ArrayList<>();
  private final LinkedHashMap<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<>();

  /**
   * Create a new empty ConstructorArgumentValues object.
   */
  public ConstructorArgumentValues() { }

  /**
   * Deep copy constructor.
   *
   * @param original the ConstructorArgumentValues to copy
   */
  public ConstructorArgumentValues(ConstructorArgumentValues original) {
    addArgumentValues(original);
  }

  /**
   * Copy all given argument values into this object, using separate holder
   * instances to keep the values independent from the original object.
   * <p>Note: Identical ValueHolder instances will only be registered once,
   * to allow for merging and re-merging of argument value definitions. Distinct
   * ValueHolder instances carrying the same content are of course allowed.
   */
  public void addArgumentValues(@Nullable ConstructorArgumentValues other) {
    if (other != null) {
      for (Map.Entry<Integer, ValueHolder> entry : other.indexedArgumentValues.entrySet()) {
        int index = entry.getKey();
        ValueHolder argValue = entry.getValue();
        addOrMergeIndexedArgumentValue(index, argValue.copy());
      }

      for (ValueHolder valueHolder : other.genericArgumentValues) {
        if (!this.genericArgumentValues.contains(valueHolder)) {
          addOrMergeGenericArgumentValue(valueHolder.copy());
        }
      }
    }
  }

  /**
   * Add an argument value for the given index in the constructor argument list.
   *
   * @param index the index in the constructor argument list
   * @param value the argument value
   */
  public void addIndexedArgumentValue(int index, @Nullable Object value) {
    addIndexedArgumentValue(index, new ValueHolder(value));
  }

  /**
   * Add an argument value for the given index in the constructor argument list.
   *
   * @param index the index in the constructor argument list
   * @param value the argument value
   * @param type the type of the constructor argument
   */
  public void addIndexedArgumentValue(int index, @Nullable Object value, String type) {
    addIndexedArgumentValue(index, new ValueHolder(value, type));
  }

  /**
   * Add an argument value for the given index in the constructor argument list.
   *
   * @param index the index in the constructor argument list
   * @param newValue the argument value in the form of a ValueHolder
   */
  public void addIndexedArgumentValue(int index, ValueHolder newValue) {
    Assert.isTrue(index >= 0, "Index must not be negative");
    Assert.notNull(newValue, "ValueHolder is required");
    addOrMergeIndexedArgumentValue(index, newValue);
  }

  /**
   * Add an argument value for the given index in the constructor argument list,
   * merging the new value (typically a collection) with the current value
   * if demanded: see {@link Mergeable}.
   *
   * @param key the index in the constructor argument list
   * @param newValue the argument value in the form of a ValueHolder
   */
  private void addOrMergeIndexedArgumentValue(Integer key, ValueHolder newValue) {
    ValueHolder currentValue = this.indexedArgumentValues.get(key);
    if (currentValue != null && newValue.getValue() instanceof Mergeable mergeable) {
      if (mergeable.isMergeEnabled()) {
        newValue.setValue(mergeable.merge(currentValue.getValue()));
      }
    }
    this.indexedArgumentValues.put(key, newValue);
  }

  /**
   * Check whether an argument value has been registered for the given index.
   *
   * @param index the index in the constructor argument list
   */
  public boolean hasIndexedArgumentValue(int index) {
    return this.indexedArgumentValues.containsKey(index);
  }

  /**
   * Get argument value for the given index in the constructor argument list.
   *
   * @param index the index in the constructor argument list
   * @param requiredType the type to match (can be {@code null} to match
   * untyped values only)
   * @return the ValueHolder for the argument, or {@code null} if none set
   */
  @Nullable
  public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType) {
    return getIndexedArgumentValue(index, requiredType, null);
  }

  /**
   * Get argument value for the given index in the constructor argument list.
   *
   * @param index the index in the constructor argument list
   * @param requiredType the type to match (can be {@code null} to match
   * untyped values only)
   * @param requiredName the type to match (can be {@code null} to match
   * unnamed values only, or empty String to match any name)
   * @return the ValueHolder for the argument, or {@code null} if none set
   */
  @Nullable
  public ValueHolder getIndexedArgumentValue(
          int index, @Nullable Class<?> requiredType, @Nullable String requiredName) {
    Assert.isTrue(index >= 0, "Index must not be negative");
    ValueHolder valueHolder = this.indexedArgumentValues.get(index);
    if (valueHolder != null
            && (valueHolder.getType() == null || (requiredType != null
            && ClassUtils.matchesTypeName(requiredType, valueHolder.getType())))
            && (valueHolder.getName() == null || (requiredName != null && (requiredName.isEmpty() || requiredName.equals(valueHolder.getName()))))) {
      return valueHolder;
    }
    return null;
  }

  /**
   * Return the map of indexed argument values.
   *
   * @return unmodifiable Map with Integer index as key and ValueHolder as value
   * @see ValueHolder
   */
  public Map<Integer, ValueHolder> getIndexedArgumentValues() {
    return Collections.unmodifiableMap(this.indexedArgumentValues);
  }

  /**
   * Add a generic argument value to be matched by type.
   * <p>Note: A single generic argument value will just be used once,
   * rather than matched multiple times.
   *
   * @param value the argument value
   */
  public void addGenericArgumentValue(Object value) {
    this.genericArgumentValues.add(new ValueHolder(value));
  }

  /**
   * Add a generic argument value to be matched by type.
   * <p>Note: A single generic argument value will just be used once,
   * rather than matched multiple times.
   *
   * @param value the argument value
   * @param type the type of the constructor argument
   */
  public void addGenericArgumentValue(Object value, String type) {
    this.genericArgumentValues.add(new ValueHolder(value, type));
  }

  /**
   * Add a generic argument value to be matched by type or name (if available).
   * <p>Note: A single generic argument value will just be used once,
   * rather than matched multiple times.
   *
   * @param newValue the argument value in the form of a ValueHolder
   * <p>Note: Identical ValueHolder instances will only be registered once,
   * to allow for merging and re-merging of argument value definitions. Distinct
   * ValueHolder instances carrying the same content are of course allowed.
   */
  public void addGenericArgumentValue(ValueHolder newValue) {
    Assert.notNull(newValue, "ValueHolder is required");
    if (!this.genericArgumentValues.contains(newValue)) {
      addOrMergeGenericArgumentValue(newValue);
    }
  }

  /**
   * Add a generic argument value, merging the new value (typically a collection)
   * with the current value if demanded: see {@link Mergeable}.
   *
   * @param newValue the argument value in the form of a ValueHolder
   */
  private void addOrMergeGenericArgumentValue(ValueHolder newValue) {
    if (newValue.getName() != null) {
      for (Iterator<ValueHolder> it = genericArgumentValues.iterator(); it.hasNext(); ) {
        ValueHolder currentValue = it.next();
        if (newValue.getName().equals(currentValue.getName())) {
          if (newValue.getValue() instanceof Mergeable mergeable) {
            if (mergeable.isMergeEnabled()) {
              newValue.setValue(mergeable.merge(currentValue.getValue()));
            }
          }
          it.remove();
        }
      }
    }
    genericArgumentValues.add(newValue);
  }

  /**
   * Look for a generic argument value that matches the given type.
   *
   * @param requiredType the type to match
   * @return the ValueHolder for the argument, or {@code null} if none set
   */
  @Nullable
  public ValueHolder getGenericArgumentValue(Class<?> requiredType) {
    return getGenericArgumentValue(requiredType, null, null);
  }

  /**
   * Look for a generic argument value that matches the given type.
   *
   * @param requiredType the type to match
   * @param requiredName the name to match
   * @return the ValueHolder for the argument, or {@code null} if none set
   */
  @Nullable
  public ValueHolder getGenericArgumentValue(Class<?> requiredType, String requiredName) {
    return getGenericArgumentValue(requiredType, requiredName, null);
  }

  /**
   * Look for the next generic argument value that matches the given type,
   * ignoring argument values that have already been used in the current
   * resolution process.
   *
   * @param requiredType the type to match (can be {@code null} to find
   * an arbitrary next generic argument value)
   * @param requiredName the name to match (can be {@code null} to not
   * match argument values by name, or empty String to match any name)
   * @param usedValueHolders a Set of ValueHolder objects that have already been used
   * in the current resolution process and should therefore not be returned again
   * @return the ValueHolder for the argument, or {@code null} if none found
   */
  @Nullable
  public ValueHolder getGenericArgumentValue(@Nullable Class<?> requiredType,
          @Nullable String requiredName, @Nullable Set<ValueHolder> usedValueHolders) {

    for (ValueHolder valueHolder : genericArgumentValues) {
      if (usedValueHolders != null && usedValueHolders.contains(valueHolder)) {
        continue;
      }
      if (valueHolder.getName() != null &&
              (requiredName == null || (!requiredName.isEmpty() && !requiredName.equals(valueHolder.getName())))) {
        continue;
      }
      if (valueHolder.getType() != null
              && (requiredType == null || !ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) {
        continue;
      }
      if (requiredType != null && valueHolder.getType() == null && valueHolder.getName() == null
              && !ClassUtils.isAssignableValue(requiredType, valueHolder.getValue())) {
        continue;
      }
      return valueHolder;
    }
    return null;
  }

  /**
   * Return the list of generic argument values.
   *
   * @return unmodifiable List of ValueHolders
   * @see ValueHolder
   */
  public List<ValueHolder> getGenericArgumentValues() {
    return Collections.unmodifiableList(this.genericArgumentValues);
  }

  /**
   * Look for an argument value that either corresponds to the given index
   * in the constructor argument list or generically matches by type.
   *
   * @param index the index in the constructor argument list
   * @param requiredType the parameter type to match
   * @return the ValueHolder for the argument, or {@code null} if none set
   */
  @Nullable
  public ValueHolder getArgumentValue(int index, Class<?> requiredType) {
    return getArgumentValue(index, requiredType, null, null);
  }

  /**
   * Look for an argument value that either corresponds to the given index
   * in the constructor argument list or generically matches by type.
   *
   * @param index the index in the constructor argument list
   * @param requiredType the parameter type to match
   * @param requiredName the parameter name to match
   * @return the ValueHolder for the argument, or {@code null} if none set
   */
  @Nullable
  public ValueHolder getArgumentValue(int index, Class<?> requiredType, String requiredName) {
    return getArgumentValue(index, requiredType, requiredName, null);
  }

  /**
   * Look for an argument value that either corresponds to the given index
   * in the constructor argument list or generically matches by type.
   *
   * @param index the index in the constructor argument list
   * @param requiredType the parameter type to match (can be {@code null}
   * to find an untyped argument value)
   * @param requiredName the parameter name to match (can be {@code null}
   * to find an unnamed argument value, or empty String to match any name)
   * @param usedValueHolders a Set of ValueHolder objects that have already
   * been used in the current resolution process and should therefore not
   * be returned again (allowing to return the next generic argument match
   * in case of multiple generic argument values of the same type)
   * @return the ValueHolder for the argument, or {@code null} if none set
   */
  @Nullable
  public ValueHolder getArgumentValue(int index, @Nullable Class<?> requiredType,
          @Nullable String requiredName, @Nullable Set<ValueHolder> usedValueHolders) {
    Assert.isTrue(index >= 0, "Index must not be negative");

    ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);
    if (valueHolder == null) {
      valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
    }
    return valueHolder;
  }

  /**
   * Determine whether at least one argument value refers to a name.
   *
   * @see ValueHolder#getName()
   */
  public boolean containsNamedArgument() {
    for (ValueHolder valueHolder : indexedArgumentValues.values()) {
      if (valueHolder.getName() != null) {
        return true;
      }
    }
    for (ValueHolder valueHolder : genericArgumentValues) {
      if (valueHolder.getName() != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the number of argument values held in this instance,
   * counting both indexed and generic argument values.
   */
  public int getArgumentCount() {
    return (this.indexedArgumentValues.size() + this.genericArgumentValues.size());
  }

  /**
   * Return if this holder does not contain any argument values,
   * neither indexed ones nor generic ones.
   */
  public boolean isEmpty() {
    return (this.indexedArgumentValues.isEmpty() && this.genericArgumentValues.isEmpty());
  }

  /**
   * Clear this holder, removing all argument values.
   */
  public void clear() {
    this.indexedArgumentValues.clear();
    this.genericArgumentValues.clear();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ConstructorArgumentValues that)) {
      return false;
    }
    if (this.genericArgumentValues.size() != that.genericArgumentValues.size()
            || this.indexedArgumentValues.size() != that.indexedArgumentValues.size()) {
      return false;
    }
    Iterator<ValueHolder> it1 = this.genericArgumentValues.iterator();
    Iterator<ValueHolder> it2 = that.genericArgumentValues.iterator();
    while (it1.hasNext() && it2.hasNext()) {
      ValueHolder vh1 = it1.next();
      ValueHolder vh2 = it2.next();
      if (!vh1.contentEquals(vh2)) {
        return false;
      }
    }
    for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
      ValueHolder vh1 = entry.getValue();
      ValueHolder vh2 = that.indexedArgumentValues.get(entry.getKey());
      if (vh2 == null || !vh1.contentEquals(vh2)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 7;
    for (ValueHolder valueHolder : this.genericArgumentValues) {
      hashCode = 31 * hashCode + valueHolder.contentHashCode();
    }
    hashCode = 29 * hashCode;
    for (Map.Entry<Integer, ValueHolder> entry : this.indexedArgumentValues.entrySet()) {
      hashCode = 31 * hashCode + (entry.getValue().contentHashCode() ^ entry.getKey().hashCode());
    }
    return hashCode;
  }

  /**
   * Holder for a constructor argument value, with an optional type
   * attribute indicating the target type of the actual constructor argument.
   */
  public static class ValueHolder implements BeanMetadataElement {

    @Nullable
    private Object value;

    @Nullable
    private String type;

    @Nullable
    private String name;

    @Nullable
    private Object source;

    private boolean converted = false;

    @Nullable
    private Object convertedValue;

    /**
     * Create a new ValueHolder for the given value.
     *
     * @param value the argument value
     */
    public ValueHolder(@Nullable Object value) {
      this.value = value;
    }

    /**
     * Create a new ValueHolder for the given value and type.
     *
     * @param value the argument value
     * @param type the type of the constructor argument
     */
    public ValueHolder(@Nullable Object value, @Nullable String type) {
      this.value = value;
      this.type = type;
    }

    /**
     * Create a new ValueHolder for the given value, type and name.
     *
     * @param value the argument value
     * @param type the type of the constructor argument
     * @param name the name of the constructor argument
     */
    public ValueHolder(@Nullable Object value, @Nullable String type, @Nullable String name) {
      this.value = value;
      this.type = type;
      this.name = name;
    }

    /**
     * Set the value for the constructor argument.
     */
    public void setValue(@Nullable Object value) {
      this.value = value;
    }

    /**
     * Return the value for the constructor argument.
     */
    @Nullable
    public Object getValue() {
      return this.value;
    }

    /**
     * Set the type of the constructor argument.
     */
    public void setType(@Nullable String type) {
      this.type = type;
    }

    /**
     * Return the type of the constructor argument.
     */
    @Nullable
    public String getType() {
      return this.type;
    }

    /**
     * Set the name of the constructor argument.
     */
    public void setName(@Nullable String name) {
      this.name = name;
    }

    /**
     * Return the name of the constructor argument.
     */
    @Nullable
    public String getName() {
      return this.name;
    }

    /**
     * Set the configuration source {@code Object} for this metadata element.
     * <p>The exact type of the object will depend on the configuration mechanism used.
     */
    public void setSource(@Nullable Object source) {
      this.source = source;
    }

    @Override
    @Nullable
    public Object getSource() {
      return this.source;
    }

    /**
     * Return whether this holder contains a converted value already ({@code true}),
     * or whether the value still needs to be converted ({@code false}).
     */
    public synchronized boolean isConverted() {
      return this.converted;
    }

    /**
     * Set the converted value of the constructor argument,
     * after processed type conversion.
     */
    public synchronized void setConvertedValue(@Nullable Object value) {
      this.converted = (value != null);
      this.convertedValue = value;
    }

    /**
     * Return the converted value of the constructor argument,
     * after processed type conversion.
     */
    @Nullable
    public synchronized Object getConvertedValue() {
      return this.convertedValue;
    }

    /**
     * Determine whether the content of this ValueHolder is equal
     * to the content of the given other ValueHolder.
     * <p>Note that ValueHolder does not implement {@code equals}
     * directly, to allow for multiple ValueHolder instances with the
     * same content to reside in the same Set.
     */
    private boolean contentEquals(ValueHolder other) {
      return (this == other ||
              (ObjectUtils.nullSafeEquals(this.value, other.value) && ObjectUtils.nullSafeEquals(this.type, other.type)));
    }

    /**
     * Determine whether the hash code of the content of this ValueHolder.
     * <p>Note that ValueHolder does not implement {@code hashCode}
     * directly, to allow for multiple ValueHolder instances with the
     * same content to reside in the same Set.
     */
    private int contentHashCode() {
      return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.type);
    }

    /**
     * Create a copy of this ValueHolder: that is, an independent
     * ValueHolder instance with the same contents.
     */
    public ValueHolder copy() {
      ValueHolder copy = new ValueHolder(this.value, this.type, this.name);
      copy.setSource(this.source);
      return copy;
    }
  }

}
