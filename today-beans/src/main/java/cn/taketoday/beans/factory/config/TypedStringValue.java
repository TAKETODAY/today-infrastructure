/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.config;

import java.util.Comparator;

import cn.taketoday.beans.BeanMetadataElement;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Holder for a typed String value. Can be added to bean definitions
 * in order to explicitly specify a target type for a String value,
 * for example for collection elements.
 *
 * <p>This holder will just store the String value and the target type.
 * The actual conversion will be performed by the bean factory.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanDefinition#getPropertyValues
 * @see PropertyValues#add
 * @since 4.0
 */
public class TypedStringValue implements BeanMetadataElement, Comparable<TypedStringValue> {

  @Nullable
  private String value;

  @Nullable
  private volatile Object targetType;

  @Nullable
  private Object source;

  @Nullable
  private String specifiedTypeName;

  private volatile boolean dynamic;

  /**
   * Create a new {@link TypedStringValue} for the given String value.
   *
   * @param value the String value
   */
  public TypedStringValue(@Nullable String value) {
    setValue(value);
  }

  /**
   * Create a new {@link TypedStringValue} for the given String value
   * and target type.
   *
   * @param value the String value
   * @param targetType the type to convert to
   */
  public TypedStringValue(@Nullable String value, Class<?> targetType) {
    setValue(value);
    setTargetType(targetType);
  }

  /**
   * Create a new {@link TypedStringValue} for the given String value
   * and target type.
   *
   * @param value the String value
   * @param targetTypeName the type to convert to
   */
  public TypedStringValue(@Nullable String value, String targetTypeName) {
    setValue(value);
    setTargetTypeName(targetTypeName);
  }

  /**
   * Set the String value.
   * <p>Only necessary for manipulating a registered value,
   * for example in BeanFactoryPostProcessors.
   */
  public void setValue(@Nullable String value) {
    this.value = value;
  }

  /**
   * Return the String value.
   */
  @Nullable
  public String getValue() {
    return this.value;
  }

  /**
   * Set the type to convert to.
   * <p>Only necessary for manipulating a registered value,
   * for example in BeanFactoryPostProcessors.
   */
  public void setTargetType(Class<?> targetType) {
    Assert.notNull(targetType, "'targetType' is required");
    this.targetType = targetType;
  }

  /**
   * Return the type to convert to.
   */
  public Class<?> getTargetType() {
    Object targetTypeValue = this.targetType;
    if (!(targetTypeValue instanceof Class)) {
      throw new IllegalStateException("Typed String value does not carry a resolved target type");
    }
    return (Class<?>) targetTypeValue;
  }

  /**
   * Specify the type to convert to.
   */
  public void setTargetTypeName(@Nullable String targetTypeName) {
    this.targetType = targetTypeName;
  }

  /**
   * Return the type to convert to.
   */
  @Nullable
  public String getTargetTypeName() {
    Object targetTypeValue = this.targetType;
    if (targetTypeValue instanceof Class<?> clazz) {
      return clazz.getName();
    }
    else {
      return (String) targetTypeValue;
    }
  }

  /**
   * Return whether this typed String value carries a target type.
   */
  public boolean hasTargetType() {
    return this.targetType instanceof Class;
  }

  /**
   * Determine the type to convert to, resolving it from a specified class name
   * if necessary. Will also reload a specified Class from its name when called
   * with the target type already resolved.
   *
   * @param classLoader the ClassLoader to use for resolving a (potential) class name
   * @return the resolved type to convert to
   * @throws ClassNotFoundException if the type cannot be resolved
   */
  @Nullable
  public Class<?> resolveTargetType(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
    String typeName = getTargetTypeName();
    if (typeName == null) {
      return null;
    }
    Class<?> resolvedClass = ClassUtils.forName(typeName, classLoader);
    this.targetType = resolvedClass;
    return resolvedClass;
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
   * Set the type name as actually specified for this particular value, if any.
   */
  public void setSpecifiedTypeName(@Nullable String specifiedTypeName) {
    this.specifiedTypeName = specifiedTypeName;
  }

  /**
   * Return the type name as actually specified for this particular value, if any.
   */
  @Nullable
  public String getSpecifiedTypeName() {
    return this.specifiedTypeName;
  }

  /**
   * Mark this value as dynamic, i.e. as containing an expression
   * and hence not being subject to caching.
   */
  public void setDynamic() {
    this.dynamic = true;
  }

  /**
   * Return whether this value has been marked as dynamic.
   */
  public boolean isDynamic() {
    return this.dynamic;
  }

  @Override
  public int compareTo(@Nullable TypedStringValue o) {
    return Comparator.comparing(TypedStringValue::getValue).compare(this, o);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof TypedStringValue that &&
            ObjectUtils.nullSafeEquals(this.value, that.value) &&
            ObjectUtils.nullSafeEquals(this.targetType, that.targetType)));
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHash(this.value, this.targetType);
  }

  @Override
  public String toString() {
    return "TypedStringValue: value [" + this.value + "], target type [" + this.targetType + "]";
  }

}
