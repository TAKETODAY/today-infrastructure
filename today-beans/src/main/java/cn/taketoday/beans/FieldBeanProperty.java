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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.reflect.PropertyAccessor;

/**
 * Field based BeanProperty
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/24 15:16
 */
public final class FieldBeanProperty extends BeanProperty {

  FieldBeanProperty(Field field) {
    super(field);
  }

  @Override
  protected PropertyAccessor createAccessor() {
    return PropertyAccessor.fromField(field);
  }

  protected TypeDescriptor createDescriptor() {
    ResolvableType resolvableType = ResolvableType.fromField(field);
    return new TypeDescriptor(resolvableType, resolvableType.resolve(getType()), this);
  }

  @Override
  protected ResolvableType createResolvableType() {
    return ResolvableType.fromField(field);
  }

  @Override
  public int getModifiers() {
    return field.getModifiers();
  }

  @Override
  public boolean isSynthetic() {
    return field.isSynthetic();
  }

  @Override
  public boolean isReadOnly() {
    return Modifier.isFinal(field.getModifiers());
  }

  @Override
  public boolean isReadable() {
    return true;
  }

  @Override
  public boolean isWriteable() {
    return !isReadOnly();
  }

  @Override
  public Field getField() {
    return field;
  }

  @Override
  public Class<?> getType() {
    return field.getType();
  }

  @Override
  public Class<?> getDeclaringClass() {
    return field.getDeclaringClass();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o instanceof BeanProperty property) {
      return Objects.equals(field, property.getField());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(field);
  }

  @Override
  public String toString() {
    return getType().getSimpleName() + " " + getName();
  }

}
