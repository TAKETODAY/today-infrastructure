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

import java.lang.reflect.Method;

import infra.lang.VisibleForTesting;
import infra.util.ObjectUtils;

/**
 * read-only PropertyAccessor
 *
 * @author TODAY 2020/9/12 15:22
 * @see ReflectionException
 */
public abstract class ReadOnlyPropertyAccessor extends PropertyAccessor {

  @Override
  public final void set(Object obj, @Nullable Object value) {
    throwReadonly(obj, value);
  }

  @Override
  public final Method getWriteMethod() {
    throw new ReflectionException("Readonly property");
  }

  @Override
  public final boolean isWriteable() {
    return false;
  }

  @Nullable
  @VisibleForTesting
  static Class<?> classToString(@Nullable Object obj) {
    return obj != null ? obj.getClass() : null;
  }

  static void throwReadonly(Object obj, @Nullable Object value) {
    throw new ReflectionException("Can't set value '%s' to '%s' read only property"
            .formatted(ObjectUtils.toHexString(value), classToString(obj)));
  }

}
