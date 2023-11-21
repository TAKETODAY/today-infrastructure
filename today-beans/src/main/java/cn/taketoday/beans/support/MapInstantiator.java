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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY 2021/1/29 15:56
 * @see CollectionUtils#createMap(Class, Class, int)
 * @since 3.0
 */
public class MapInstantiator extends BeanInstantiator {

  private int capacity = Constant.ZERO;
  private Class<?> keyType;
  private final Class<?> mapType;

  public MapInstantiator(Class<?> mapType) {
    this(mapType, null);
  }

  public MapInstantiator(Class<?> mapType, Class<?> keyType) {
    Assert.notNull(mapType, "map type is required");
    this.keyType = keyType;
    this.mapType = mapType;
  }

  @Override
  public Object doInstantiate(final Object[] args) {
    return CollectionUtils.createMap(mapType, keyType, capacity);
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public void setKeyType(Class<?> keyType) {
    this.keyType = keyType;
  }
}
