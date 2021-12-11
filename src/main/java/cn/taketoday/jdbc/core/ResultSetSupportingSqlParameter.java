/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.core;

import java.sql.ResultSet;

import cn.taketoday.lang.Nullable;

/**
 * Common base class for ResultSet-supporting SqlParameters like
 * {@link SqlOutParameter} and {@link SqlReturnResultSet}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ResultSetSupportingSqlParameter extends SqlParameter {

  @Nullable
  private ResultSetExtractor<?> resultSetExtractor;

  @Nullable
  private RowCallbackHandler rowCallbackHandler;

  @Nullable
  private RowMapper<?> rowMapper;

  /**
   * Create a new ResultSetSupportingSqlParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   */
  public ResultSetSupportingSqlParameter(String name, int sqlType) {
    super(name, sqlType);
  }

  /**
   * Create a new ResultSetSupportingSqlParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param scale the number of digits after the decimal point
   * (for DECIMAL and NUMERIC types)
   */
  public ResultSetSupportingSqlParameter(String name, int sqlType, int scale) {
    super(name, sqlType, scale);
  }

  /**
   * Create a new ResultSetSupportingSqlParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param typeName the type name of the parameter (optional)
   */
  public ResultSetSupportingSqlParameter(String name, int sqlType, @Nullable String typeName) {
    super(name, sqlType, typeName);
  }

  /**
   * Create a new ResultSetSupportingSqlParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param rse the {@link ResultSetExtractor} to use for parsing the {@link ResultSet}
   */
  public ResultSetSupportingSqlParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
    super(name, sqlType);
    this.resultSetExtractor = rse;
  }

  /**
   * Create a new ResultSetSupportingSqlParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param rch the {@link RowCallbackHandler} to use for parsing the {@link ResultSet}
   */
  public ResultSetSupportingSqlParameter(String name, int sqlType, RowCallbackHandler rch) {
    super(name, sqlType);
    this.rowCallbackHandler = rch;
  }

  /**
   * Create a new ResultSetSupportingSqlParameter.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param sqlType the parameter SQL type according to {@code java.sql.Types}
   * @param rm the {@link RowMapper} to use for parsing the {@link ResultSet}
   */
  public ResultSetSupportingSqlParameter(String name, int sqlType, RowMapper<?> rm) {
    super(name, sqlType);
    this.rowMapper = rm;
  }

  /**
   * Does this parameter support a ResultSet, i.e. does it hold a
   * ResultSetExtractor, RowCallbackHandler or RowMapper?
   */
  public boolean isResultSetSupported() {
    return (this.resultSetExtractor != null || this.rowCallbackHandler != null || this.rowMapper != null);
  }

  /**
   * Return the ResultSetExtractor held by this parameter, if any.
   */
  @Nullable
  public ResultSetExtractor<?> getResultSetExtractor() {
    return this.resultSetExtractor;
  }

  /**
   * Return the RowCallbackHandler held by this parameter, if any.
   */
  @Nullable
  public RowCallbackHandler getRowCallbackHandler() {
    return this.rowCallbackHandler;
  }

  /**
   * Return the RowMapper held by this parameter, if any.
   */
  @Nullable
  public RowMapper<?> getRowMapper() {
    return this.rowMapper;
  }

  /**
   * This implementation always returns {@code false}.
   */
  @Override
  public boolean isInputValueProvided() {
    return false;
  }

}
