/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.orm.hibernate5;

import org.hibernate.LockMode;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.PersistenceException;

/**
 * @author TODAY <br>
 * 2019-11-05 21:11
 */
public interface HibernateUpdateOperations<T> {

  Serializable save(Object entity) throws PersistenceException;

  void saveOrUpdate(Object entity) throws PersistenceException;

  void saveAll(List<T> t) throws PersistenceException;

  default Integer insertBySql(String sql) throws PersistenceException {
    return insertBySql(null, sql);
  }

  default Integer insertBySql(String sql, Object... params) throws PersistenceException {
    return insertBySql(params, sql);
  }

  Integer insertBySql(Object[] params, String sql) throws PersistenceException;

  T merge(T entity) throws PersistenceException;

  T merge(String entityName, T entity) throws PersistenceException;

  // update
  // ------------------------------------
  void update(T entity) throws PersistenceException;

  default Integer update(String columnNames, String primaryKey) throws PersistenceException {
    return update(columnNames, null, primaryKey);
  }

  default Integer update(String columnNames, String primaryKey, Object... params) throws PersistenceException {
    return update(columnNames, params, primaryKey);
  }

  Integer update(String columnNames, Object[] params, String primaryKey) throws PersistenceException;

  default Integer updateOne(String sql) throws PersistenceException {
    return updateOne(null, sql);
  }

  default Integer updateOne(String sql, Object... params) throws PersistenceException {
    return updateOne(params, sql);
  }

  Integer updateOne(Object[] params, String sql) throws PersistenceException;

  Integer updateOne(String columnName, String primaryKey, Object columnValue, Object keyValue)
          throws PersistenceException;

  // delete
  // ------------------------------
  Integer deleteById(Serializable id);

  void delete(Object entity) throws PersistenceException;

  void delete(Object entity, LockMode lockMode) throws PersistenceException;

  void delete(String entityName, Object entity) throws PersistenceException;

  void delete(String entityName, Object entity, LockMode lockMode) throws PersistenceException;

  void deleteAll(Collection<?> entities) throws PersistenceException;

}
