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

package infra.reflect;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import infra.lang.Assert;
import infra.util.ReflectionUtils;

import static infra.reflect.ReadOnlyPropertyAccessor.throwReadonly;

/**
 * java reflect {@link Field} implementation
 *
 * @author TODAY 2020/9/11 17:56
 */
final class ReflectivePropertyAccessor extends PropertyAccessor {

  @Nullable
  private final Field field;

  @Nullable
  private final Method readMethod;

  @Nullable
  private final Method writeMethod;

  ReflectivePropertyAccessor(@Nullable Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    Assert.isTrue(field != null || readMethod != null, "field or read method must be specified one");
    this.field = ReflectionUtils.makeAccessible(field);
    this.readMethod = ReflectionUtils.makeAccessible(readMethod);
    this.writeMethod = ReflectionUtils.makeAccessible(writeMethod);
  }

  @Override
  public Object get(final Object obj) {
    if (readMethod != null) {
      return ReflectionUtils.invokeMethod(readMethod, obj);
    }
    return ReflectionUtils.getField(field, obj);
  }

  @Override
  public void set(Object obj, @Nullable Object value) {
    if (writeMethod != null) {
      ReflectionUtils.invokeMethod(writeMethod, obj, value);
    }
    else if (field != null) {
      ReflectionUtils.setField(field, obj, value);
    }
    else {
      throwReadonly(obj, value);
    }
  }

  @Override
  public boolean isWriteable() {
    return field != null || writeMethod != null;
  }

  @Nullable
  @Override
  public Method getReadMethod() {
    return readMethod;
  }

  @Nullable
  @Override
  public Method getWriteMethod() {
    return writeMethod;
  }
}
