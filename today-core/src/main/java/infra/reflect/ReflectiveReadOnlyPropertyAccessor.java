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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ReflectionUtils;

/**
 * @author TODAY 2020/9/18 22:03
 */
final class ReflectiveReadOnlyPropertyAccessor extends ReadOnlyPropertyAccessor {

  @Nullable
  private final Field field;

  @Nullable
  private final Method readMethod;

  ReflectiveReadOnlyPropertyAccessor(@Nullable Field field, @Nullable Method readMethod) {
    Assert.isTrue(field != null || readMethod != null,
            "field or read method must be specified one");
    this.field = ReflectionUtils.makeAccessible(field);
    this.readMethod = ReflectionUtils.makeAccessible(readMethod);
  }

  @Override
  public Object get(final Object obj) {
    if (readMethod != null) {
      return ReflectionUtils.invokeMethod(readMethod, obj);
    }
    return ReflectionUtils.getField(field, obj);
  }

  @Nullable
  @Override
  public Method getReadMethod() {
    return readMethod;
  }
}
