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

package infra.persistence;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import infra.beans.BeanProperty;
import infra.jdbc.ResultSetIterator;

/**
 * An abstract class that extends {@link ResultSetIterator} to provide iteration
 * over entities retrieved from a database result set. It supports converting
 * the iterated entities into a {@link Map} using either a property name or a
 * custom key mapping function.
 *
 * <p>This class is designed to be extended by concrete implementations that
 * define how entities are read from the result set and how errors are handled.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 *  try (EntityIterator<MyEntity> iterator = ...) {
 *    // Convert entities to a map using a property name as the key
 *    Map<Long, MyEntity> entityMap = iterator.toMap("id");
 *
 *    // Alternatively, use a custom key mapping function
 *    Map<String, MyEntity> customMap = iterator.toMap(entity -> entity.getName());
 *  }
 * }</pre>
 *
 * @param <T> the type of elements returned by this iterator
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/22 19:38
 */
public abstract class EntityIterator<T> extends ResultSetIterator<T> {

  private final EntityMetadata entityMetadata;

  protected EntityIterator(ResultSet resultSet, EntityMetadata entityMetadata) {
    super(resultSet);
    this.entityMetadata = entityMetadata;
  }

  /**
   * Converts the entities returned by this iterator into a {@link Map}, using the value of
   * a specified property as the key. The property is identified by its name, and its value
   * is retrieved using reflection.
   *
   * <p>This method iterates through all entities, retrieves the value of the specified property
   * for each entity, and uses it as the key in the resulting map. The iteration stops when
   * there are no more entities to process.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *  try (EntityIterator<MyEntity> iterator = ...) {
   *    // Convert entities to a map using the "id" property as the key
   *    Map<Long, MyEntity> entityMap = iterator.toMap("id");
   *
   *    // The resulting map will have the following structure:
   *    // {
   *    //   1L: MyEntity{id=1, ...},
   *    //   2L: MyEntity{id=2, ...},
   *    //   ...
   *    // }
   *  }
   * }</pre>
   *
   * @param <K> the type of the keys in the resulting map, inferred from the property value
   * @param mapKey the name of the property to use as the key in the resulting map. This
   * property must exist in the entities being iterated.
   * @return a {@link Map} where the keys are the values of the specified property and the
   * values are the corresponding entities. If the property value is null for any
   * entity, the behavior is undefined.
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
   * Converts the entities returned by this iterator into a {@link Map}, using a provided
   * key mapping function to generate the keys for the map.
   *
   * <p>This method iterates through all entities, applies the given key mapping function
   * to each entity to compute the key, and inserts the key-value pair into the resulting map.
   * The iteration stops when there are no more entities to process. After processing, the
   * iterator is closed automatically.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *  try (EntityIterator<MyEntity> iterator = ...) {
   *    // Convert entities to a map using a lambda to extract the key
   *    Map<Long, MyEntity> entityMap = iterator.toMap(entity -> entity.getId());
   *
   *    // The resulting map will have the following structure:
   *    // {
   *    //   1L: MyEntity{id=1, ...},
   *    //   2L: MyEntity{id=2, ...},
   *    //   ...
   *    // }
   *  }
   * }</pre>
   *
   * @param <K> the type of the keys in the resulting map, inferred from the return type
   * of the key mapping function
   * @param keyMapper a function that computes the key for each entity in the iteration
   * @return a {@link Map} where the keys are computed by the provided key mapping function
   * and the values are the corresponding entities. If the key mapping function
   * returns null for any entity, the behavior is undefined.
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
