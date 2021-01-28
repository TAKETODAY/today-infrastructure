/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.factory;

import java.lang.reflect.Field;
import java.util.Objects;

import cn.taketoday.context.reflect.SetterMethod;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * Bean property
 *
 * @author TODAY <br>
 * 2018-06-23 11:28:01
 */
public class PropertyValue {

  /** field info */
  private final Field field;
  /** property value */
  private final Object value;
  /** @since 3.0 */
  private final SetterMethod accessor;

  public PropertyValue(Object value, Field field) {
    Assert.notNull(field, "field must not be null");
    this.value = value;
    this.field = field;
    this.accessor = ReflectionUtils.newSetterMethod(field);
  }

  public Field getField() {
    return field;
  }

  public Object getValue() {
    return value;
  }

  public void set(Object bean) {
    accessor.set(bean, value);
  }

  public void set(Object bean, Object value) {
    accessor.set(bean, value);
  }

  @Override
  public int hashCode() {
    return field.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof PropertyValue) {
      final PropertyValue other = (PropertyValue) obj;
      return Objects.equals(other.value, value) && Objects.equals(other.field, field);
    }
    return false;
  }

  @Override
  public String toString() {
    return new StringBuilder()
            .append("{\"value\":\"").append(value)
            .append("\",\"property\":\"").append(field.getName())
            .append("\",\"propertyClass\":\"").append(field.getType())
            .append("\",\"beanClass:\":\"").append(field.getDeclaringClass())
            .append("\"}")
            .toString();
  }

}
