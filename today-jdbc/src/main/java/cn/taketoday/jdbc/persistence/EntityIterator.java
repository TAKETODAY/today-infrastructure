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

package cn.taketoday.jdbc.persistence;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.jdbc.ResultSetIterator;

/**
 * Iterator for a {@link ResultSet}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/22 19:38
 */
public abstract class EntityIterator<T> extends ResultSetIterator<T> {

  private final EntityMetadata entityMetadata;

  protected EntityIterator(ResultSet resultSet, EntityMetadata entityMetadata) {
    super(resultSet);
    this.entityMetadata = entityMetadata;
  }

  /**
   * Convert entities to Map
   *
   * @param mapKey entity property
   */
  @SuppressWarnings("unchecked")
  public <K> Map<K, T> toMap(String mapKey) {
    try {
      LinkedHashMap<K, T> entities = new LinkedHashMap<>();
      BeanProperty beanProperty = entityMetadata.root.obtainBeanProperty(mapKey);
      while (hasNext()) {
        T entity = next();
        Object propertyValue = beanProperty.getValue(entity);
        entities.put((K) propertyValue, entity);
      }
      return entities;
    }
    finally {
      close();
    }
  }

  /**
   * Convert entities to Map
   *
   * @param keyMapper key mapping function
   */
  public <K> Map<K, T> toMap(Function<T, K> keyMapper) {
    try {
      LinkedHashMap<K, T> entities = new LinkedHashMap<>();
      while (hasNext()) {
        T entity = next();
        K key = keyMapper.apply(entity);
        entities.put(key, entity);
      }
      return entities;
    }
    finally {
      close();
    }
  }

}
