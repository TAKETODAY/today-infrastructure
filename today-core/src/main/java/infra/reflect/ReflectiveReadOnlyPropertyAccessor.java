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
package infra.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import infra.lang.Nullable;
import infra.util.ReflectionUtils;

/**
 * @author TODAY 2020/9/18 22:03
 */
final class ReflectiveReadOnlyPropertyAccessor extends ReadOnlyPropertyAccessor {
  @Nullable
  private final Field field;
  private final Method readMethod;

  ReflectiveReadOnlyPropertyAccessor(@Nullable Field field, Method readMethod) {
    this.field = field;
    this.readMethod = readMethod;
  }

  @Override
  public Object get(final Object obj) {
    if (field != null) {
      return ReflectionUtils.getField(field, obj);
    }
    return ReflectionUtils.invokeMethod(readMethod, obj);
  }

  @Override
  public Method getReadMethod() {
    return readMethod;
  }
}
