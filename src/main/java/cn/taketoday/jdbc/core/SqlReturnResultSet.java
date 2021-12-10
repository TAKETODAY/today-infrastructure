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

/**
 * Represents a returned {@link java.sql.ResultSet} from a stored procedure call.
 *
 * <p>A {@link ResultSetExtractor}, {@link RowCallbackHandler} or {@link RowMapper}
 * must be provided to handle any returned rows.
 *
 * <p>Returned {@link java.sql.ResultSet ResultSets} - like all stored procedure
 * parameters - must have names.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class SqlReturnResultSet extends ResultSetSupportingSqlParameter {

  /**
   * Create a new instance of the {@link SqlReturnResultSet} class.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param extractor the {@link ResultSetExtractor} to use for parsing the {@link java.sql.ResultSet}
   */
  public SqlReturnResultSet(String name, ResultSetExtractor<?> extractor) {
    super(name, 0, extractor);
  }

  /**
   * Create a new instance of the {@link SqlReturnResultSet} class.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param handler the {@link RowCallbackHandler} to use for parsing the {@link java.sql.ResultSet}
   */
  public SqlReturnResultSet(String name, RowCallbackHandler handler) {
    super(name, 0, handler);
  }

  /**
   * Create a new instance of the {@link SqlReturnResultSet} class.
   *
   * @param name the name of the parameter, as used in input and output maps
   * @param mapper the {@link RowMapper} to use for parsing the {@link java.sql.ResultSet}
   */
  public SqlReturnResultSet(String name, RowMapper<?> mapper) {
    super(name, 0, mapper);
  }

  /**
   * This implementation always returns {@code true}.
   */
  @Override
  public boolean isResultsParameter() {
    return true;
  }

}
