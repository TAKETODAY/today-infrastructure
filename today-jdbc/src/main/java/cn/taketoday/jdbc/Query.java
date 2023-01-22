/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.io.InputStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * <p>
 * better to use :
 * create queries with {@link JdbcConnection} class instead,
 * using try-with-resource blocks
 * <pre>
 * try (Connection con = repositoryManager.open()) {
 *    return repositoryManager.createQuery(query, name, returnGeneratedKeys)
 *                .fetch(Pojo.class);
 * }
 * </pre>
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/18 23:17
 */
public final class Query extends AbstractQuery {

  private final ArrayList<ParameterBinder> queryParameters = new ArrayList<>();

  public Query(JdbcConnection connection, String querySQL, boolean generatedKeys) {
    super(connection, querySQL, generatedKeys);
  }

  public Query(JdbcConnection connection, String querySQL, String[] columnNames) {
    super(connection, querySQL, columnNames);
  }

  protected Query(JdbcConnection connection, String querySQL, boolean generatedKeys, String[] columnNames) {
    super(connection, querySQL, generatedKeys, columnNames);
  }

  public Query addParameter(int value) {
    addParameter(ParameterBinder.forInt(value));
    return this;
  }

  public Query addParameter(long value) {
    addParameter(ParameterBinder.forLong(value));
    return this;
  }

  public Query addParameter(String value) {
    addParameter(ParameterBinder.forString(value));
    return this;
  }

  public Query addParameter(boolean value) {
    addParameter(ParameterBinder.forBoolean(value));
    return this;
  }

  public Query addParameter(InputStream value) {
    addParameter(ParameterBinder.forBinaryStream(value));
    return this;
  }

  public Query addParameter(LocalDate value) {
    addParameter(ParameterBinder.forDate(Date.valueOf(value)));
    return this;
  }

  public Query addParameter(LocalTime value) {
    addParameter(ParameterBinder.forTime(Time.valueOf(value)));
    return this;
  }

  public Query addParameter(LocalDateTime value) {
    addParameter(ParameterBinder.forTimestamp(Timestamp.valueOf(value)));
    return this;
  }

  public Query addParameter(ParameterBinder binder) {
    queryParameters.add(binder);
    return this;
  }

  //

  public Query setParameter(int pos, String value) {
    setParameter(pos, ParameterBinder.forString(value));
    return this;
  }

  public Query setParameter(int pos, int value) {
    setParameter(pos, ParameterBinder.forInt(value));
    return this;
  }

  public Query setParameter(int pos, long value) {
    setParameter(pos, ParameterBinder.forLong(value));
    return this;
  }

  public Query setParameter(int pos, boolean value) {
    setParameter(pos, ParameterBinder.forBoolean(value));
    return this;
  }

  public Query setParameter(int pos, InputStream value) {
    setParameter(pos, ParameterBinder.forBinaryStream(value));
    return this;
  }

  public Query setParameter(int pos, LocalDate value) {
    setParameter(pos, ParameterBinder.forDate(Date.valueOf(value)));
    return this;
  }

  public Query setParameter(int pos, LocalTime value) {
    setParameter(pos, ParameterBinder.forTime(Time.valueOf(value)));
    return this;
  }

  public Query setParameter(int pos, LocalDateTime value) {
    setParameter(pos, ParameterBinder.forTimestamp(Timestamp.valueOf(value)));
    return this;
  }

  public Query setParameter(int pos, ParameterBinder binder) {
    queryParameters.set(pos, binder);
    return this;
  }

  public ArrayList<ParameterBinder> getQueryParameters() {
    return queryParameters;
  }

  //

  @Override
  protected void postProcessStatement(PreparedStatement statement) {
    // parameters assignation to query
    int paramIdx = 1;
    for (ParameterBinder binder : queryParameters) {
      try {
        binder.bind(statement, paramIdx++);
      }
      catch (SQLException e) {
        throw new ParameterBindFailedException(
                "Error binding parameter index: '" + (paramIdx - 1) + "' - " + e.getMessage(), e);
      }
    }
  }

  //
  @Override
  public Query setCaseSensitive(boolean caseSensitive) {
    super.setCaseSensitive(caseSensitive);
    return this;
  }

  @Override
  public Query setAutoDerivingColumns(boolean autoDerivingColumns) {
    super.setAutoDerivingColumns(autoDerivingColumns);
    return this;
  }

  @Override
  public Query throwOnMappingFailure(boolean throwOnMappingFailure) {
    super.throwOnMappingFailure(throwOnMappingFailure);
    return this;
  }

  @Override
  public Query addColumnMapping(String columnName, String propertyName) {
    super.addColumnMapping(columnName, propertyName);
    return this;
  }

  /**
   * add a Statement processor when {@link  #buildStatement() build a PreparedStatement}
   */
  @Override
  public Query processStatement(StatementCallback callback) {
    super.processStatement(callback);
    return this;
  }

  @Override
  public Query addToBatch() {
    super.addToBatch();
    return this;
  }

}
