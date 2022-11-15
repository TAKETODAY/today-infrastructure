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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.result.ResultSetIterator;
import cn.taketoday.lang.Nullable;

/**
 * Entity manager
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:47
 */
public interface EntityManager {

  /**
   * persist an entity to underlying repository
   *
   * @param entity entity instance
   * @throws IllegalEntityException entityClass is legal entity
   */
  void persist(Object entity) throws DataAccessException;

  /**
   * persist an entity to underlying repository
   *
   * @param entity entity instance
   * @param returnGeneratedKeys a flag indicating whether auto-generated keys should be returned;
   * @throws IllegalEntityException entityClass is legal entity
   * @see PreparedStatement
   * @see Connection#prepareStatement(String, int)
   */
  void persist(Object entity, boolean returnGeneratedKeys) throws DataAccessException;

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
  void persist(Iterable<?> entities, boolean returnGeneratedKeys) throws DataAccessException;

  /**
   * Merge the state of the given entity into underlying repository
   *
   * @param entity entity instance
   * @throws IllegalEntityException entityClass is legal entity
   */
  void updateById(Object entity);

  /**
   * Delete an entity.
   * <p>
   * No transaction
   *
   * @param entityClass entity descriptor
   * @param id id
   * @throws IllegalEntityException entityClass is legal entity
   */
  void delete(Class<?> entityClass, Object id);

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
  int delete(Object entity);

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
  <T> T findFirst(Class<T> entityClass, Object query) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> T findFirst(Class<T> entityClass, @Nullable QueryCondition conditions)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  @Nullable
  <T> T findUnique(T entity) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  @Nullable
  <T> T findUnique(Class<T> entityClass, Object query) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> T findUnique(Class<T> entityClass, @Nullable QueryCondition conditions)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> List<T> find(T entity) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> List<T> find(Class<T> entityClass, Object params) throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> List<T> find(Class<T> entityClass, @Nullable QueryCondition conditions)
          throws DataAccessException;

  /**
   * The find Map is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * Eg. Return a of Map[Integer,Author] for find(Author.class, params, "id")
   *
   * @param <K> the returned Map keys type
   * @param <T> the returned Map values type
   * @param mapKey The property to use as key for each value in the list.
   * @return Map containing key pair data.
   * @throws IllegalEntityException entityClass is legal entity
   */
  <K, T> Map<K, T> find(Class<T> entityClass, Object params, String mapKey)
          throws DataAccessException;

  /**
   * The find Map is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * Eg. Return a of Map[Integer,Author] for find(Author.class, params, "id")
   *
   * @param <K> the returned Map keys type
   * @param <T> the returned Map values type
   * @param mapKey The property to use as key for each value in the list.
   * @return Map containing key pair data.
   * @throws IllegalEntityException entityClass is legal entity
   */
  <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryCondition conditions, String mapKey)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> void iterate(Class<T> entityClass, Object params, Consumer<T> entityConsumer)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> ResultSetIterator<T> iterate(Class<T> entityClass, Object params)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> void iterate(Class<T> entityClass, @Nullable QueryCondition conditions, Consumer<T> entityConsumer)
          throws DataAccessException;

  /**
   * @throws IllegalEntityException entityClass is legal entity
   */
  <T> ResultSetIterator<T> iterate(Class<T> entityClass, @Nullable QueryCondition conditions)
          throws DataAccessException;
}
