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
package cn.taketoday.jdbc.support;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

/**
 * @author TODAY <br>
 * 2019-08-18 19:20
 */
public class JdbcExecutor implements BasicOperation, QueryOperation, UpdateOperation, QueryOptionalOperation {

  private final BasicOperation basicOperation;
  private final UpdateOperation updateOperation;
  private final QueryOptionalOperation queryOperation;

  public JdbcExecutor(DataSource dataSource) {
    final QueryExecutor queryExecutor = new QueryExecutor(dataSource);

    this.queryOperation = queryExecutor;
    this.basicOperation = queryExecutor;

    this.updateOperation = new UpdateExecutor(dataSource);
  }

  public JdbcExecutor(BasicOperation basicOperation,
                      UpdateOperation updateOperation,
                      QueryOptionalOperation queryOperation) {

    this.queryOperation = queryOperation;
    this.basicOperation = basicOperation;
    this.updateOperation = updateOperation;
  }

  public BasicOperation getBasicOperation() {
    return basicOperation;
  }

  public UpdateOperation getUpdateOperation() {
    return updateOperation;
  }

  public QueryOptionalOperation getQueryOperation() {
    return queryOperation;
  }

  // Basic
  //-----------------------------

  @Override
  public <T> T execute(ConnectionCallback<T> action) throws SQLException {
    return getBasicOperation().execute(action);
  }

  @Override
  public <T> T execute(StatementCallback<T> action) throws SQLException {
    return getBasicOperation().execute(action);
  }

  @Override
  public void execute(String sql) throws SQLException {
    getBasicOperation().execute(sql);
  }

  @Override
  public <T> T execute(String sql, PreparedStatementCallback<T> action) throws SQLException {
    return getBasicOperation().execute(sql, action);
  }

  @Override
  public <T> T execute(String sql, CallableStatementCallback<T> action) throws SQLException {
    return getBasicOperation().execute(sql, action);
  }

  // update
  // ----------------------------------------------------

  @Override
  public int update(String sql) throws SQLException {
    return getUpdateOperation().update(sql);
  }

  @Override
  public int update(String sql, Object[] args) throws SQLException {
    return getUpdateOperation().update(sql, args);
  }

  @Override
  public int[] batchUpdate(String... sql) throws SQLException {
    return getUpdateOperation().batchUpdate(sql);
  }

  @Override
  public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws SQLException {
    return getUpdateOperation().batchUpdate(sql, batchArgs);
  }

  // query
  // ----------------------------------------

  @Override
  public <T> T query(String sql, Object[] args, ResultSetExtractor<T> rse) throws SQLException {
    return getQueryOperation().query(sql, args, rse);
  }

  @Override
  public void query(String sql, Object[] args, ResultSetHandler rch) throws SQLException {
    getQueryOperation().query(sql, args, rch);
  }

  @Override
  public <T> T query(String sql, Object[] args, Class<T> requiredType) throws SQLException {
    return getQueryOperation().query(sql, args, requiredType);
  }

  @Override
  public <T> List<T> queryList(String sql, Object[] args, RowMapper<T> rowMapper) throws SQLException {
    return getQueryOperation().queryList(sql, args, rowMapper);
  }

  @Override
  public <T> List<T> queryList(String sql, Object[] args, Class<T> elementType) throws SQLException {
    return getQueryOperation().queryList(sql, args, elementType);
  }

  @Override
  public List<Map<String, Object>> queryList(String sql, Object[] args) throws SQLException {
    return getQueryOperation().queryList(sql, args);
  }

  @Override
  public Map<String, Object> queryMap(String sql, Object[] args) throws SQLException {
    return getQueryOperation().queryMap(sql, args);
  }

}
