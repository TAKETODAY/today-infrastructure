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
  public final void set(final Object obj, final Object value) {
    throw new ReflectionException(
            "Can't set value '%s' to '%s' read only property".formatted(ObjectUtils.toHexString(value), classToString(obj)));
  }

  @VisibleForTesting
  static Class<?> classToString(Object obj) {
    return obj != null ? obj.getClass() : null;
  }

  @Override
  public final Method getWriteMethod() {
    throw new ReflectionException("read only property");
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

}
