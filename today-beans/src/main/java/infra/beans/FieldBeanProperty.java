/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans;

import java.io.Serial;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.reflect.PropertyAccessor;

/**
 * Field based BeanProperty
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/24 15:16
 */
public final class FieldBeanProperty extends BeanProperty {

  @Serial
  private static final long serialVersionUID = 1L;

  private final boolean writeable;

  FieldBeanProperty(Field field) {
    super(field, null, null);
    this.writeable = !Modifier.isFinal(field.getModifiers());
  }

  @Override
  protected PropertyAccessor createAccessor() {
    return PropertyAccessor.forField(field);
  }

  protected TypeDescriptor createDescriptor() {
    ResolvableType resolvableType = ResolvableType.forField(field);
    return new TypeDescriptor(resolvableType, resolvableType.resolve(getType()), this);
  }

  @Override
  protected ResolvableType createResolvableType() {
    return ResolvableType.forField(field);
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
    return !writeable;
  }

  @Override
  public boolean isReadable() {
    return true;
  }

  @Override
  public boolean isWriteable() {
    return writeable;
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
