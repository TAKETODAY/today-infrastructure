/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.support;

import java.lang.reflect.Array;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;

/**
 * @author TODAY 2021/1/29 15:56
 * @see Array#newInstance(Class, int)
 * @since 3.0
 */
public class ArrayInstantiator extends BeanInstantiator {

  private int capacity = Constant.ZERO;
  private final Class<?> componentType;

  public ArrayInstantiator(Class<?> componentType) {
    Assert.notNull(componentType, "component type is required");
    this.componentType = componentType;
  }

  @Override
  public Object doInstantiate(final Object[] args) {
    // TODO - only handles 2-dimensional arrays
    final Class<?> componentType = this.componentType;
    if (componentType.isArray()) {
      Object array = Array.newInstance(componentType, 1);
      Array.set(array, 0, Array.newInstance(componentType.getComponentType(), capacity));
      return array;
    }
    else {
      return Array.newInstance(componentType, capacity);
    }
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

}
