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

import infra.util.ReflectionUtils;

/**
 * @author TODAY 2020/9/19 22:39
 * @see ReflectionUtils#getField(Field, Object)
 */
final class ReflectiveGetterMethod implements GetterMethod {
  private final Field field;

  ReflectiveGetterMethod(final Field field) {
    this.field = field;
  }

  @Override
  public Object get(final Object obj) {
    return ReflectionUtils.getField(field, obj);
  }
}
