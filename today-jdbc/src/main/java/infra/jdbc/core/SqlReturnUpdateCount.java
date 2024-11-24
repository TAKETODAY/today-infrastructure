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

package infra.jdbc.core;

import java.sql.Types;

/**
 * Represents a returned update count from a stored procedure call.
 *
 * <p>Returned update counts - like all stored procedure
 * parameters - <b>must</b> have names.
 *
 * @author Thomas Risberg
 */
public class SqlReturnUpdateCount extends SqlParameter {

  /**
   * Create a new SqlReturnUpdateCount.
   *
   * @param name the name of the parameter, as used in input and output maps
   */
  public SqlReturnUpdateCount(String name) {
    super(name, Types.INTEGER);
  }

  /**
   * This implementation always returns {@code false}.
   */
  @Override
  public boolean isInputValueProvided() {
    return false;
  }

  /**
   * This implementation always returns {@code true}.
   */
  @Override
  public boolean isResultsParameter() {
    return true;
  }

}
