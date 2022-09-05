/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.sql;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MapCache;

/**
 * EntityHolder Factory
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 23:28
 */
public abstract class EntityHolderFactory {

  final MapCache<Class<?>, EntityHolder, EntityHolderFactory> entityCache = new MapCache<>() {

    @Override
    protected EntityHolder createValue(Class<?> entityClass, @Nullable EntityHolderFactory entityHolderFactory) {
      Assert.notNull(entityHolderFactory, "No EntityHolderFactory");
      return entityHolderFactory.createEntityHolder(entityClass);
    }
  };

  public EntityHolder getEntityHolder(Class<?> entityClass) {
    return entityCache.get(entityClass, this);
  }

  /**
   * create a new EntityHolder instance
   *
   * @param entityClass entity class
   * @return a new EntityHolder
   */
  public abstract EntityHolder createEntityHolder(Class<?> entityClass);

}
