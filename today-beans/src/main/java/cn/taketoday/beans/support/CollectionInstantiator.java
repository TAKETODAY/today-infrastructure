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

package cn.taketoday.beans.support;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CollectionUtils#createCollection(Class, Class, int)
 * @since 3.0 2021/1/29 15:56
 */
public class CollectionInstantiator extends BeanInstantiator {

  private final Class<?> elementType;

  private final Class<?> collectionType;

  public CollectionInstantiator(Class<?> collectionType) {
    this(collectionType, null);
  }

  public CollectionInstantiator(Class<?> collectionType, Class<?> elementType) {
    Assert.notNull(collectionType, "collection type is required");
    this.elementType = elementType;
    this.collectionType = collectionType;
  }

  @Override
  public Object doInstantiate(final Object[] args) {
    return CollectionUtils.createCollection(collectionType, elementType, 0);
  }

}
