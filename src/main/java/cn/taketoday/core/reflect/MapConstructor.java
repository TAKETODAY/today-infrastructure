/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.core.reflect;

import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.utils.CollectionUtils;

/**
 * @author TODAY 2021/1/29 15:56
 * @since 3.0
 */
public class MapConstructor implements ConstructorAccessor {

  private int capacity = Constant.DEFAULT_CAPACITY;
  private Class<?> keyType;
  private final Class<?> mapType;

  public MapConstructor(Class<?> mapType) {
    this(mapType, null);
  }

  public MapConstructor(Class<?> mapType, Class<?> keyType) {
    Assert.notNull(mapType, "map type must not be null");
    this.keyType = keyType;
    this.mapType = mapType;
  }

  @Override
  public Object newInstance(final Object[] args) {
    return CollectionUtils.createMap(mapType, keyType, capacity);
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public void setKeyType(Class<?> keyType) {
    this.keyType = keyType;
  }
}
