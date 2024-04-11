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

package cn.taketoday.persistence;

import cn.taketoday.util.MapCache;

/**
 * {@link EntityMetadata} Factory
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 23:28
 */
public abstract class EntityMetadataFactory {

  final MapCache<Class<?>, EntityMetadata, EntityMetadataFactory> entityCache = new MapCache<>() {

    @Override
    protected EntityMetadata createValue(Class<?> entityClass, EntityMetadataFactory entityMetadataFactory) {
      return entityMetadataFactory.createEntityMetadata(entityClass);
    }
  };

  /**
   * Get a EntityMetadata instance
   *
   * @param entityClass entity Class
   * @return EntityMetadata may be a cached instance
   * @throws IllegalEntityException entity definition is not legal
   */
  public EntityMetadata getEntityMetadata(Class<?> entityClass) throws IllegalEntityException {
    return entityCache.get(entityClass, this);
  }

  /**
   * create a new EntityMetadata instance
   *
   * @param entityClass entity class
   * @return a new EntityMetadata
   */
  public abstract EntityMetadata createEntityMetadata(Class<?> entityClass) throws IllegalEntityException;

}
