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

import infra.util.ExceptionUtils;
import infra.util.ReflectionUtils;

/**
 * java reflect {@link Method} implementation
 *
 * @author TODAY  2020/9/20 21:49
 */
final class ReflectiveMethodAccessor extends MethodInvoker implements MethodAccessor {

  // @since 4.0
  private final boolean handleReflectionException;

  ReflectiveMethodAccessor(final Method method, boolean handleReflectionException) {
    super(method);
    this.handleReflectionException = handleReflectionException;
  }

  @Override
  public Object invoke(final Object obj, final Object[] args) {
    try {
      return getMethod().invoke(obj, args);
    }
    catch (Exception e) {
      if (handleReflectionException) {
        ReflectionUtils.handleReflectionException(e);
      }
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

}
