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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Holder for a key-value style attribute that is part of a bean definition.
 * Keeps track of the definition source in addition to the key-value pair.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 * @since 4.0 2022/3/7 12:30
 */
public class BeanMetadataAttribute implements BeanMetadataElement {

  private final String name;

  @Nullable
  private final Object value;

  @Nullable
  private Object source;

  /**
   * Create a new AttributeValue instance.
   *
   * @param name the name of the attribute (never {@code null})
   * @param value the value of the attribute (possibly before type conversion)
   */
  public BeanMetadataAttribute(String name, @Nullable Object value) {
    Assert.notNull(name, "Name is required");
    this.name = name;
    this.value = value;
  }

  /**
   * Return the name of the attribute.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the value of the attribute.
   */
  @Nullable
  public Object getValue() {
    return this.value;
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

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BeanMetadataAttribute otherMa)) {
      return false;
    }
    return (this.name.equals(otherMa.name) &&
            ObjectUtils.nullSafeEquals(this.value, otherMa.value) &&
            ObjectUtils.nullSafeEquals(this.source, otherMa.source));
  }

  @Override
  public int hashCode() {
    return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
  }

  @Override
  public String toString() {
    return "metadata attribute '" + this.name + "'";
  }

}
