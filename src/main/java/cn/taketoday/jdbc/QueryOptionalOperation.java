/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.jdbc;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author TODAY <br>
 *         2019-08-18 20:08
 */
public interface QueryOptionalOperation extends QueryOperation {

  default <T> Optional<T> queryOptional(String sql, ResultSetExtractor<T> rse) throws SQLException {
    return queryOptional(sql, null, rse);
  }

  default <T> Optional<T> queryOptional(String sql, ResultSetExtractor<T> rse, Object... args) throws SQLException {
    return Optional.ofNullable(query(sql, args, rse));
  }

  default <T> Optional<T> queryOptional(String sql, Object[] args, ResultSetExtractor<T> rse) throws SQLException {
    return Optional.ofNullable(query(sql, args, rse));
  }

  default <T> Optional<T> queryOptional(String sql, Class<T> requiredType) throws SQLException {
    return Optional.ofNullable(query(sql, null, requiredType));
  }

  default <T> Optional<T> queryOptional(String sql, Class<T> requiredType, Object... args) throws SQLException {
    return Optional.ofNullable(query(sql, args, requiredType));
  }

  default <T> Optional<T> queryOptional(String sql, Object[] args, Class<T> requiredType) throws SQLException {
    return Optional.ofNullable(query(sql, args, requiredType));
  }

  // List
  // -----------------------------------------------------------

  default <T> Optional<List<T>> queryListOptional(String sql, RowMapper<T> rowMapper) throws SQLException {
    return Optional.ofNullable(queryList(sql, null, rowMapper));
  }

  default <T> Optional<List<T>> queryListOptional(String sql, RowMapper<T> rowMapper, Object... args) throws SQLException {
    return Optional.ofNullable(queryList(sql, args, rowMapper));
  }

  default <T> Optional<List<T>> queryListOptional(String sql, Object[] args, RowMapper<T> rowMapper) throws SQLException {
    return Optional.ofNullable(queryList(sql, args, rowMapper));
  }

  default <T> Optional<List<T>> queryListOptional(String sql, Class<T> elementType) throws SQLException {
    return Optional.ofNullable(queryList(sql, null, elementType));
  }

  default <T> Optional<List<T>> queryListOptional(String sql, Class<T> elementType, Object... args) throws SQLException {
    return Optional.ofNullable(queryList(sql, args, elementType));
  }

  default <T> Optional<List<T>> queryListOptional(String sql, Object[] args, Class<T> elementType) throws SQLException {
    return Optional.ofNullable(queryList(sql, args, elementType));
  }

  default Optional<List<Map<String, Object>>> queryListOptional(String sql) throws SQLException {
    return queryListOptional(sql, (Object[]) null);
  }

  default Optional<List<Map<String, Object>>> queryListOptional(String sql, Object[] args) throws SQLException {
    return Optional.ofNullable(queryList(sql, args));
  }

  // Map
  // -------------------------------------------------------------

}
