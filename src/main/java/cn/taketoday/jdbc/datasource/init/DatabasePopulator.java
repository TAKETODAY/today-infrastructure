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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Strategy used to populate, initialize, or clean up a database.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @see ResourceDatabasePopulator
 * @see DatabasePopulatorUtils
 * @see DataSourceInitializer
 * @since 3.0
 */
@FunctionalInterface
public interface DatabasePopulator {

  /**
   * Populate, initialize, or clean up the database using the provided JDBC
   * connection.
   * <p>Concrete implementations <em>may</em> throw an {@link SQLException} if
   * an error is encountered but are <em>strongly encouraged</em> to throw a
   * specific {@link ScriptException} instead. For example,
   * {@link ResourceDatabasePopulator} and {@link DatabasePopulatorUtils} wrap
   * all {@code SQLExceptions} in {@code ScriptExceptions}.
   *
   * @param connection the JDBC connection to use to populate the db; already
   * configured and ready to use; never {@code null}
   * @throws SQLException if an unrecoverable data access exception occurs
   * during database population
   * @throws ScriptException in all other error cases
   * @see DatabasePopulatorUtils#execute
   */
  void populate(Connection connection) throws SQLException, ScriptException;

}
