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

/**
 * @author TODAY 2021/1/7 17:43
 * @since 3.0
 */
final class MethodAccessorSetterMethod implements SetterMethod {
  private final MethodInvoker accessor;

  MethodAccessorSetterMethod(MethodInvoker accessor) {
    this.accessor = accessor;
  }

  @Override
  public void set(Object obj, Object value) {
    accessor.invoke(obj, new Object[] { value });
  }

  @Nullable
  @Override
  public Method getWriteMethod() {
    return accessor.getMethod();
  }

}
