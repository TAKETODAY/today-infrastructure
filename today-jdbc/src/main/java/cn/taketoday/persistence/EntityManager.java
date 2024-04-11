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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import cn.taketoday.core.Pair;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.sql.Select;
import cn.taketoday.util.StreamIterable;

/**
 * Entity manager
 * <p>
 * for simple and single table operations
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:47
 */
public interface EntityManager {

  /**
   * persist an entity to underlying repository
   *
   * @param entity entity instance
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   */
  int persist(Object entity) throws DataAccessException;

  /**
   * persist an entity to underlying repository
   *
   * @param entity entity instance
   * @param strategy property persist strategy
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   */
  int persist(Object entity, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * persist an entity to underlying repository
   *
   * @param entity entity instance
   * @param autoGenerateId a flag indicating whether auto-generated keys should be returned;
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   * @see PreparedStatement
   * @see Connection#prepareStatement(String, int)
   */
  int persist(Object entity, boolean autoGenerateId) throws DataAccessException;

  /**
   * persist an entity to underlying repository
   *
   * @param entity entity instance
   * @param strategy property persist strategy
   * @param autoGenerateId a flag indicating whether auto-generated
   * keys should be returned
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   * @see PreparedStatement
   * @see Connection#prepareStatement(String, int)
   */
  int persist(Object entity, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId)
          throws DataAccessException;

  /**
   * persist entities to underlying repository
   *
   * @param entities entities instances
   * @throws IllegalEntityException entityClass is legal entity
   */
  void persist(Iterable<?> entities) throws DataAccessException;

  /**
   * persist entities to underlying repository
   *
   * @param returnGeneratedKeys a flag indicating whether auto-generated keys should be returned;
   * @param entities entities instances
   * @throws IllegalEntityException entityClass is legal entity
   */
  void persist(Iterable<?> entities, boolean returnGeneratedKeys)
          throws DataAccessException;

  /**
   * persist entities to underlying repository
   *
   * @param entities entities instances
   * @param strategy property persist strategy
   * @throws IllegalEntityException entityClass is legal entity
   */
  void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * persist entities to underlying repository
   *
   * @param autoGenerateId a flag indicating whether
   * auto-generated keys should be returned;
   * @param entities entities instances
   * @param strategy property persist strategy
   * @throws IllegalEntityException entityClass is legal entity
   */
  void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId)
          throws DataAccessException;

  /**
   * persist entities to underlying repository
   *
   * @param entities entities instances
   * @throws IllegalEntityException entityClass is legal entity
   */
  default void persist(Stream<?> entities) throws DataAccessException {
    persist(new StreamIterable<>(entities));
  }

  /**
   * persist entities to underlying repository
   *
   * @param autoGenerateId a flag indicating whether auto-generated keys should be returned;
   * @param entities entities instances
   * @throws IllegalEntityException entityClass is legal entity
   */
  default void persist(Stream<?> entities, boolean autoGenerateId) throws DataAccessException {
    persist(new StreamIterable<>(entities), autoGenerateId);
  }

  /**
   * persist entities to underlying repository
   *
   * @param entities entities instances
   * @param strategy property persist strategy
   * @throws IllegalEntityException entityClass is legal entity
   */
  default void persist(Stream<?> entities, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    persist(new StreamIterable<>(entities), strategy);
  }

  /**
   * persist entities to underlying repository
   *
   * @param autoGenerateId a flag indicating whether
   * auto-generated keys should be returned;
   * @param entities entities instances
   * @param strategy property persist strategy
   * @throws IllegalEntityException entityClass is legal entity
   */
  default void persist(Stream<?> entities, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId) throws DataAccessException {
    persist(new StreamIterable<>(entities), strategy, autoGenerateId);
  }

  /**
   * Merge the state of the given entity into underlying repository
   *
   * @param entityOrExample entity instance
   * @return update count
   * @throws IllegalEntityException entity metadata parsing failed
   */
  int update(Object entityOrExample) throws DataAccessException;

  /**
   * Merge the state of the given entity into underlying repository
   *
   * @param entityOrExample entity instance
   * @param strategy determine which property can update
   * @return update count
   * @throws IllegalEntityException entity metadata parsing failed
   * @see UpdateBy
   */
  int update(Object entityOrExample, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Merge the state of the given entity into underlying repository
   *
   * @param entityOrExample entity instance
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   */
  int updateById(Object entityOrExample) throws DataAccessException;

  /**
   * Merge the state of the given entity including ID into underlying repository
   *
   * <p>
   * ID can be updated
   *
   * @param entityOrExample entity instance
   * @param id entity id
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   */
  int updateById(Object entityOrExample, Object id) throws DataAccessException;

  /**
   * Merge the state of the given entity into underlying repository
   *
   * @param entityOrExample entity instance
   * @param strategy determine which property can update
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   */
  int updateById(Object entityOrExample, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Merge the state of the given entity including ID into underlying repository
   * <p>
   * ID can be updated
   *
   * @param entityOrExample entity instance
   * @param id entity id
   * @param strategy determine which property can update
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   */
  int updateById(Object entityOrExample, Object id, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Merge the state of the given entity into underlying repository
   *
   * @param entityOrExample entity instance
   * @param where columnName or property name
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   */
  int updateBy(Object entityOrExample, String where) throws DataAccessException;

  /**
   * Merge the state of the given entity into underlying repository
   *
   * @param entityOrExample entity instance
   * @param where columnName or property name
   * @param strategy determine which property can update
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   */
  int updateBy(Object entityOrExample, String where, @Nullable PropertyUpdateStrategy strategy)
          throws DataAccessException;

  /**
   * Delete an entity.
   * <p>
   * No transaction
   *
   * @param entityClass entity descriptor
   * @param id id
   * @return update count
   * @throws IllegalEntityException entityClass is legal entity
   */
  int delete(Class<?> entityClass, Object id) throws DataAccessException;

  /**
   * delete entity
   * <p>
   * No transaction
   * <p>
   * If entity's id is present, using delete by id
   *
   * @return delete rows
   * @throws IllegalEntityException entityClass is legal entity
   */
  int delete(Object entityOrExample) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  @Nullable
  <T> T findById(Class<T> entityClass, Object id) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  @Nullable
  <T> T findFirst(T entity) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  @Nullable
  <T> T findFirst(Class<T> entityClass, Object example) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  @Nullable
  <T> T findFirst(Class<T> entityClass, @Nullable QueryStatement handler)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  @Nullable
  <T> T findUnique(T example) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  @Nullable
  <T> T findUnique(Class<T> entityClass, Object example) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  @Nullable
  <T> T findUnique(Class<T> entityClass, @Nullable QueryStatement handler)
          throws DataAccessException;

  /**
   * Find all entities
   *
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> List<T> find(Class<T> entityClass) throws DataAccessException;

  <T> List<T> find(Class<T> entityClass, Map<String, Order> sortKeys) throws DataAccessException;

  <T> List<T> find(Class<T> entityClass, Pair<String, Order> sortKey) throws DataAccessException;

  <T> List<T> find(Class<T> entityClass, Pair<String, Order>... sortKeys) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> List<T> find(T example) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> List<T> find(Class<T> entityClass, Object example) throws DataAccessException;

  /**
   * @param handler build {@link Select}
   * @throws IllegalEntityException entityClass is legal entity
   * @see #iterate(Class, QueryStatement)
   */
  <T> List<T> find(Class<T> entityClass, @Nullable QueryStatement handler)
          throws DataAccessException;

  /**
   * The find Map is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * <p>
   * E.g. Return an of Map[Integer,Author] for find(Author.class, example, "id")
   *
   * @param <K> the returned Map keys type
   * @param <T> the returned Map values type
   * @param mapKey The property to use as key for each value in the list.
   * @return Map containing key pair data.
   * @throws IllegalEntityException entityClass is legal entity
   */
  <K, T> Map<K, T> find(T example, String mapKey) throws DataAccessException;

  /**
   * The find Map is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * <p>
   * E.g. Return a Map[Integer,Author] for find(Author.class, example, "id")
   *
   * @param <K> the returned Map keys type
   * @param <T> the returned Map values type
   * @param mapKey The property to use as key for each value in the list.
   * @return Map containing key pair data.
   * @throws IllegalEntityException entityClass is legal entity
   */
  <K, T> Map<K, T> find(Class<T> entityClass, Object example, String mapKey)
          throws DataAccessException;

  /**
   * The find Map is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * <p>
   * E.g. Return a Map[Integer,Author] for find(Author.class, example, "id")
   *
   * @param <K> the returned Map keys type
   * @param <T> the returned Map values type
   * @param mapKey The property to use as key for each value in the list.
   * @return Map containing key pair data.
   * @throws IllegalEntityException entityClass is legal entity
   */
  <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryStatement handler, String mapKey)
          throws DataAccessException;

  /**
   * The find Map is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * E.g. Return an of Map[Integer,Author] for {@code find(Author.class, Author::getId)}
   *
   * @param <K> the returned Map keys type
   * @param <T> the returned Map values type
   * @param keyMapper key mapping function
   * @return Map containing key pair data.
   * @throws IllegalEntityException entityClass is legal entity
   */
  <K, T> Map<K, T> find(T example, Function<T, K> keyMapper) throws DataAccessException;

  /**
   * The find Map is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * <p>
   * E.g. Return a Map[Integer,Author] for {@code find(Author.class, example, Author::getId))}
   *
   * @param <K> the returned Map keys type
   * @param <T> the returned Map values type
   * @param keyMapper key mapping function
   * @return Map containing key pair data.
   * @throws IllegalEntityException entityClass is legal entity
   */
  <K, T> Map<K, T> find(Class<T> entityClass, Object example, Function<T, K> keyMapper)
          throws DataAccessException;

  /**
   * The find Map is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * <p>
   * E.g. Return a Map[Integer,Author] for {@code find(Author.class, handler, Author::getId)}
   *
   * @param <K> the returned Map keys type
   * @param <T> the returned Map values type
   * @param keyMapper key mapping function
   * @return Map containing key pair data.
   * @throws IllegalEntityException entityClass is legal entity
   */
  <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryStatement handler, Function<T, K> keyMapper)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> Number count(T example) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> Number count(Class<T> entityClass) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> Number count(Class<T> entityClass, Object example) throws DataAccessException;

  /**
   * @param handler build {@link Select}
   * @throws IllegalEntityException entityClass is legal entity
   * @see #iterate(Class, QueryStatement)
   */
  <T> Number count(Class<T> entityClass, @Nullable ConditionStatement handler)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   * @see #iterate(Class, QueryStatement)
   */
  <T> Page<T> page(Class<T> entityClass, @Nullable Pageable pageable)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> Page<T> page(T example) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> Page<T> page(T example, @Nullable Pageable pageable) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> Page<T> page(Class<T> entityClass, Object example) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> Page<T> page(Class<T> entityClass, Object example, @Nullable Pageable pageable) throws DataAccessException;

  /**
   * @param handler build {@link Select}
   * @throws IllegalEntityException entityClass is legal entity
   * @see #iterate(Class, QueryStatement)
   */
  <T> Page<T> page(Class<T> entityClass, @Nullable ConditionStatement handler)
          throws DataAccessException;

  /**
   * @param handler build {@link Select}
   * @throws IllegalEntityException entityClass is legal entity
   * @see #iterate(Class, QueryStatement)
   */
  <T> Page<T> page(Class<T> entityClass, @Nullable ConditionStatement handler, @Nullable Pageable pageable)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> void iterate(T example, Consumer<T> entityConsumer) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> void iterate(Class<T> entityClass, Object example, Consumer<T> entityConsumer)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> void iterate(Class<T> entityClass, @Nullable QueryStatement handler, Consumer<T> entityConsumer)
          throws DataAccessException;

  /**
   * Iterate entities
   *
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> EntityIterator<T> iterate(T example) throws DataAccessException;

  /**
   * Iterate entities
   *
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> EntityIterator<T> iterate(Class<T> entityClass, Object example)
          throws DataAccessException;

  /**
   * Iterate entities with given {@link QueryStatement}
   *
   * @param handler build {@link Select}
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> EntityIterator<T> iterate(Class<T> entityClass, @Nullable QueryStatement handler)
          throws DataAccessException;

}
