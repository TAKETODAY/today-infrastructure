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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;

/**
 * Simple adapter for {@link PreparedStatementSetter} that applies a given array of arguments.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ArgumentPreparedStatementSetter implements PreparedStatementSetter, ParameterDisposer {

  @Nullable
  private final Object[] args;

  /**
   * Create a new ArgPreparedStatementSetter for the given arguments.
   *
   * @param args the arguments to set
   */
  public ArgumentPreparedStatementSetter(@Nullable Object[] args) {
    this.args = args;
  }

  @Override
  public void setValues(PreparedStatement ps) throws SQLException {
    if (this.args != null) {
      for (int i = 0; i < this.args.length; i++) {
        Object arg = this.args[i];
        doSetValue(ps, i + 1, arg);
      }
    }
  }

  /**
   * Set the value for prepared statements specified parameter index using the passed in value.
   * This method can be overridden by sub-classes if needed.
   *
   * @param ps the PreparedStatement
   * @param parameterPosition index of the parameter position
   * @param argValue the value to set
   * @throws SQLException if thrown by PreparedStatement methods
   */
  protected void doSetValue(PreparedStatement ps, int parameterPosition, Object argValue) throws SQLException {
    if (argValue instanceof SqlParameterValue paramValue) {
      StatementCreatorUtils.setParameterValue(ps, parameterPosition, paramValue, paramValue.getValue());
    }
    else {
      StatementCreatorUtils.setParameterValue(ps, parameterPosition, SqlTypeValue.TYPE_UNKNOWN, argValue);
    }
  }

  @Override
  public void cleanupParameters() {
    StatementCreatorUtils.cleanupParameters(this.args);
  }

}
