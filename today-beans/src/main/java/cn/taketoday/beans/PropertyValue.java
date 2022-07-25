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

package cn.taketoday.beans;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Object to hold information and value for an individual bean property.
 * Using an object here, rather than just storing all properties in
 * a map keyed by property name, allows for more flexibility, and the
 * ability to handle indexed properties etc in an optimized way.
 *
 * <p>Note that the value doesn't need to be the final required type:
 * A {@link BeanWrapper} implementation should handle any necessary conversion,
 * as this object doesn't know anything about the objects it will be applied to.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author TODAY 2021/3/21 15:49
 */
public class PropertyValue extends BeanMetadataAttributeAccessor implements Serializable, BeanMetadataElement {

  private String name;
  private Object value;

  /** Package-visible field for caching the resolved property path tokens. */
  @Nullable
  transient volatile Object resolvedTokens;

  private boolean optional = false;

  private boolean converted = false;

  @Nullable
  private Object convertedValue;

  /** Package-visible field that indicates whether conversion is necessary. */
  @Nullable
  volatile Boolean conversionNecessary;

  public PropertyValue() { }

  public PropertyValue(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  PropertyValue(Map.Entry<String, ?> entry) {
    this(entry.getKey(), entry.getValue());
  }

  /**
   * Copy constructor.
   *
   * @param original the PropertyValue to copy (never {@code null})
   * @since 4.0
   */
  public PropertyValue(PropertyValue original) {
    Assert.notNull(original, "Original must not be null");
    this.name = original.getName();
    this.value = original.getValue();
    this.optional = original.isOptional();
    this.converted = original.converted;
    this.convertedValue = original.convertedValue;
    this.conversionNecessary = original.conversionNecessary;
    this.resolvedTokens = original.resolvedTokens;
    setSource(original.getSource());
    copyAttributesFrom(original);
  }

  /**
   * Constructor that exposes a new value for an original value holder.
   * The original holder will be exposed as source of the new holder.
   *
   * @param original the PropertyValue to link to (never {@code null})
   * @param newValue the new value to apply
   * @since 4.0
   */
  public PropertyValue(PropertyValue original, @Nullable Object newValue) {
    Assert.notNull(original, "Original must not be null");
    this.name = original.getName();
    this.value = newValue;
    this.optional = original.isOptional();
    this.conversionNecessary = original.conversionNecessary;
    this.resolvedTokens = original.resolvedTokens;
    setSource(original);
    copyAttributesFrom(original);
  }

  /**
   * Set whether this is an optional value, that is, to be ignored
   * when no corresponding property exists on the target class.
   *
   * @since 4.0
   */
  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  /**
   * Return whether this is an optional value, that is, to be ignored
   * when no corresponding property exists on the target class.
   *
   * @since 4.0
   */
  public boolean isOptional() {
    return this.optional;
  }

  /**
   * Return whether this holder contains a converted value already ({@code true}),
   * or whether the value still needs to be converted ({@code false}).
   *
   * @since 4.0
   */
  public synchronized boolean isConverted() {
    return this.converted;
  }

  /**
   * Set the converted value of this property value,
   * after processed type conversion.
   *
   * @since 4.0
   */
  public synchronized void setConvertedValue(@Nullable Object value) {
    this.converted = true;
    this.convertedValue = value;
  }

  /**
   * Return the converted value of this property value,
   * after processed type conversion.
   *
   * @since 4.0
   */
  @Nullable
  public synchronized Object getConvertedValue() {
    return this.convertedValue;
  }

  /**
   * Return the name of the property.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the value of the property.
   * <p>Note that type conversion will <i>not</i> have occurred here.
   * It is the responsibility of the BeanWrapper implementation to
   * perform type conversion.
   */
  @Nullable
  public Object getValue() {
    return this.value;
  }

  /**
   * Return the original PropertyValue instance for this value holder.
   *
   * @return the original PropertyValue (either a source of this
   * value holder or this value holder itself).
   */
  public PropertyValue getOriginalPropertyValue() {
    PropertyValue original = this;
    Object source = getSource();
    while (source instanceof PropertyValue && source != original) {
      original = (PropertyValue) source;
      source = original.getSource();
    }
    return original;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PropertyValue otherPv)) {
      return false;
    }
    return name.equals(otherPv.name)
            && Objects.equals(this.value, otherPv.value)
            && Objects.equals(getSource(), otherPv.getSource());
  }

  @Override
  public int hashCode() {
    return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
  }

  @Override
  public String toString() {
    return "bean property '" + this.name + "'";
  }

}
