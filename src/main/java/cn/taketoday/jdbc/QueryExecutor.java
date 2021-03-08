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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.jdbc.reflection.JdbcBeanMetadata;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.jdbc.utils.JdbcUtils;

/**
 * @author TODAY <br>
 * 2019-08-18 20:12
 */
public class QueryExecutor extends Executor implements QueryOperation, QueryOptionalOperation {

  private TypeHandlerRegistry registry = TypeHandlerRegistry.getSharedInstance();

  public QueryExecutor() { }

  public QueryExecutor(final DataSource dataSource) {
    setDataSource(dataSource);
  }

  public void setRegistry(TypeHandlerRegistry registry) {
    this.registry = registry;
  }

  @Override
  public <T> List<T> queryList(final String sql, final Object[] args, final RowMapper<T> rowMapper) throws SQLException {

    return query(sql, args, (ResultSet result) -> {

      final ArrayList<T> ret = new ArrayList<>();
      int i = 1;
      while (result.next()) {
        ret.add(rowMapper.mapRow(result, i++));
      }
      return ret;
    });
  }

  @Override
  public <T> T query(final String sql, final Object[] args, final ResultSetExtractor<T> rse) throws SQLException {

    return execute((ConnectionCallback<T>) (con) -> {

      try (final PreparedStatement statement = con.prepareStatement(sql)) {
        applyStatementSettings(statement, args);

        try (final ResultSet result = statement.executeQuery()) {
          return rse.extractData(result);
        }
      }
    });
  }

  @Override
  public void query(final String sql, final Object[] args, final ResultSetHandler rch) throws SQLException {
    execute((ConnectionCallback<Void>) (Connection con) -> {

      try (final PreparedStatement statement = con.prepareStatement(sql)) {
        applyStatementSettings(statement, args);
        try (final ResultSet result = statement.executeQuery()) {
          if (result.next()) {
            rch.handleResult(result);
          }
        }
      }
      return null;
    });
  }

  @Override
  public <T> T query(final String sql, final Object[] args, final Class<T> requiredType) throws SQLException {

    final class ResultSetExtractor0 implements ResultSetExtractor<T> {

      @Override
      public T extractData(ResultSet rs) throws SQLException {

        if (rs.next()) {
          final ResultSetMetaData metaData = rs.getMetaData();
          final ResultSetHandlerFactory<T> factory = newFactory(requiredType);
          final cn.taketoday.jdbc.ResultSetHandler<T> resultSetHandler = factory.newResultSetHandler(metaData);

          return resultSetHandler.handle(rs);
        }
        return null;
      }
    }

    return query(sql, args, new ResultSetExtractor0());
  }

  private boolean caseSensitive;
  private boolean throwOnMappingError;
  private boolean autoDeriveColumnNames;
  private Map<String, String> columnMappings;

  public <T> ResultSetHandlerFactory<T> newFactory(Class<T> clazz) {
    JdbcBeanMetadata pojoMetadata = new JdbcBeanMetadata(clazz, caseSensitive, autoDeriveColumnNames, columnMappings, throwOnMappingError);
    return new DefaultResultSetHandlerFactory<>(pojoMetadata, registry);
  }

  @Override
  public <T> List<T> queryList(final String sql, final Object[] args, final Class<T> elementType) throws SQLException {


    return query(sql, args, (ResultSet result) -> {
      final ArrayList<T> ret = new ArrayList<>();

      final ResultSetMetaData metaData = result.getMetaData();
      final ResultSetHandlerFactory<T> factory = newFactory(elementType);
      final cn.taketoday.jdbc.ResultSetHandler<T> resultSetHandler = factory.newResultSetHandler(metaData);

      while (result.next()) {
        ret.add(resultSetHandler.handle(result));
      }
      return ret;
    });
  }

  @Override
  public List<Map<String, Object>> queryList(final String sql, final Object[] args) throws SQLException {

    return query(sql, args, (ResultSet result) -> {

      final List<Map<String, Object>> ret = new ArrayList<>();
      final ResultSetMetaData metaData = result.getMetaData();
      int columnCount = metaData.getColumnCount();

      while (result.next()) {
        final Map<String, Object> map = new HashMap<>(columnCount);

        for (int i = 1; i <= columnCount; i++) {
          map.put(JdbcUtils.getColumnName(metaData, i), JdbcUtils.getResultSetValue(result, i));
        }

        ret.add(map);
      }
      return ret;
    });

  }

  @Override
  public Map<String, Object> queryMap(final String sql, final Object[] args) throws SQLException {

    return query(sql, args, (ResultSet result) -> {

      final ResultSetMetaData metaData = result.getMetaData();
      final int all = metaData.getColumnCount();

      final Map<String, Object> ret = new HashMap<>(all);

      for (int i = 1; i <= all; i++) {
        ret.put(JdbcUtils.getColumnName(metaData, i), JdbcUtils.getResultSetValue(result, i));
      }
      return ret;

    });
  }

}
