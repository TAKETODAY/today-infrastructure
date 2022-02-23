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

package cn.taketoday.jdbc.datasource.init;

import cn.taketoday.core.io.EncodedResource;

/**
 * Thrown by {@link ScriptUtils} if a statement in an SQL script failed when
 * executing it against the target database.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ScriptStatementFailedException extends ScriptException {

  /**
   * Construct a new {@code ScriptStatementFailedException}.
   *
   * @param stmt the actual SQL statement that failed
   * @param stmtNumber the statement number in the SQL script (i.e.,
   * the n<sup>th</sup> statement present in the resource)
   * @param encodedResource the resource from which the SQL statement was read
   * @param cause the underlying cause of the failure
   */
  public ScriptStatementFailedException(String stmt, int stmtNumber, EncodedResource encodedResource, Throwable cause) {
    super(buildErrorMessage(stmt, stmtNumber, encodedResource), cause);
  }

  /**
   * Build an error message for an SQL script execution failure,
   * based on the supplied arguments.
   *
   * @param stmt the actual SQL statement that failed
   * @param stmtNumber the statement number in the SQL script (i.e.,
   * the n<sup>th</sup> statement present in the resource)
   * @param encodedResource the resource from which the SQL statement was read
   * @return an error message suitable for an exception's <em>detail message</em>
   * or logging
   */
  public static String buildErrorMessage(String stmt, int stmtNumber, EncodedResource encodedResource) {
    return String.format("Failed to execute SQL script statement #%s of %s: %s", stmtNumber, encodedResource, stmt);
  }

}
