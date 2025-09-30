/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

import infra.dao.InvalidDataAccessApiUsageException;

/**
 * Simple adapter for {@link PreparedStatementSetter} that applies
 * given arrays of arguments and JDBC argument types.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ArgumentTypePreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

  private final Object @Nullable [] args;

  private final int @Nullable [] argTypes;

  /**
   * Create a new ArgTypePreparedStatementSetter for the given arguments.
   *
   * @param args the arguments to set
   * @param argTypes the corresponding SQL types of the arguments
   */
  public ArgumentTypePreparedStatementSetter(Object @Nullable [] args, int @Nullable [] argTypes) {
    if ((args != null && argTypes == null) || (args == null && argTypes != null) ||
            (args != null && args.length != argTypes.length)) {
      throw new InvalidDataAccessApiUsageException("args and argTypes parameters must match");
    }
    this.args = args;
    this.argTypes = argTypes;
  }

  @Override
  public void setValues(PreparedStatement ps) throws SQLException {
    int parameterPosition = 1;
    if (this.args != null && this.argTypes != null) {
      for (int i = 0; i < this.args.length; i++) {
        Object arg = this.args[i];
        if (arg instanceof Collection<?> entries && this.argTypes[i] != Types.ARRAY) {
          for (Object entry : entries) {
            if (entry instanceof Object[] valueArray) {
              for (Object argValue : valueArray) {
                doSetValue(ps, parameterPosition, this.argTypes[i], argValue);
                parameterPosition++;
              }
            }
            else {
              doSetValue(ps, parameterPosition, this.argTypes[i], entry);
              parameterPosition++;
            }
          }
        }
        else {
          doSetValue(ps, parameterPosition, this.argTypes[i], arg);
          parameterPosition++;
        }
      }
    }
  }

  /**
   * Set the value for the prepared statement's specified parameter position using the passed in
   * value and type. This method can be overridden by sub-classes if needed.
   *
   * @param ps the PreparedStatement
   * @param parameterPosition index of the parameter position
   * @param argType the argument type
   * @param argValue the argument value
   * @throws SQLException if thrown by PreparedStatement methods
   */
  protected void doSetValue(PreparedStatement ps, int parameterPosition, int argType, Object argValue)
          throws SQLException {

    StatementCreatorUtils.setParameterValue(ps, parameterPosition, argType, argValue);
  }

  @Override
  public void cleanupParameters() {
    StatementCreatorUtils.cleanupParameters(this.args);
  }

}
