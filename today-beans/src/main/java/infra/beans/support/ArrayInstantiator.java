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

package infra.beans.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;

import infra.lang.Assert;

/**
 * @author TODAY 2021/1/29 15:56
 * @see Array#newInstance(Class, int)
 * @since 3.0
 */
public class ArrayInstantiator extends BeanInstantiator {

  private final Class<?> componentType;

  public ArrayInstantiator(Class<?> componentType) {
    Assert.notNull(componentType, "component type is required");
    this.componentType = componentType;
  }

  @Override
  public Object doInstantiate(final @Nullable Object @Nullable [] args) {
    // TODO - only handles 2-dimensional arrays
    final Class<?> componentType = this.componentType;
    if (componentType.isArray()) {
      Object array = Array.newInstance(componentType, 1);
      Array.set(array, 0, Array.newInstance(componentType.getComponentType(), 0));
      return array;
    }
    else {
      return Array.newInstance(componentType, 0);
    }
  }

}
