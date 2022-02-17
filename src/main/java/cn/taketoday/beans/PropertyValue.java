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
import java.util.Objects;

import cn.taketoday.lang.Nullable;

/**
 * Object to hold information and value for an individual bean property.
 *
 * @author TODAY 2021/3/21 15:49
 */
public class PropertyValue implements Serializable, BeanMetadataElement {

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

  @Nullable
  private Object source;

  public PropertyValue() { }

  public PropertyValue(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  public PropertyValue(PropertyValue pv) {
    this.name = pv.name;
    this.value = pv.value;
  }

  /**
   * Set the configuration source {@code Object} for this metadata element.
   * <p>The exact type of the object will depend on the configuration mechanism used.
   *
   * @since 4.0
   */
  public void setSource(@Nullable Object source) {
    this.source = source;
  }

  // @since 4.0
  @Override
  @Nullable
  public Object getSource() {
    return this.source;
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
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof PropertyValue that))
      return false;
    return Objects.equals(name, that.name) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public String toString() {
    return "bean property '" + this.name + "'";
  }

}
