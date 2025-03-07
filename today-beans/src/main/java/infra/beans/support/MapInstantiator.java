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

package infra.beans.support;

import infra.lang.Assert;
import infra.util.CollectionUtils;

/**
 * @author TODAY 2021/1/29 15:56
 * @see CollectionUtils#createMap(Class, Class, int)
 * @since 3.0
 */
public class MapInstantiator extends BeanInstantiator {

  private final Class<?> keyType;

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
    return CollectionUtils.createMap(mapType, keyType, 0);
  }

}
