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

package cn.taketoday.jdbc.support;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import cn.taketoday.lang.NonNull;

/**
 * A callback interface used by the JdbcUtils class. Implementations of this
 * interface perform the actual work of extracting database meta-data, but
 * don't need to worry about exception handling. SQLExceptions will be caught
 * and handled correctly by the JdbcUtils class.
 *
 * @param <T> the result type
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see JdbcUtils#extractDatabaseMetaData(javax.sql.DataSource, DatabaseMetaDataCallback)
 */
@FunctionalInterface
public interface DatabaseMetaDataCallback<T> {

  /**
   * Implementations must implement this method to process the meta-data
   * passed in. Exactly what the implementation chooses to do is up to it.
   *
   * @param dbmd the DatabaseMetaData to process
   * @return a result object extracted from the meta-data
   * (can be an arbitrary object, as needed by the implementation)
   * @throws SQLException if an SQLException is encountered getting
   * column values (that is, there's no need to catch SQLException)
   * @throws MetaDataAccessException in case of other failures while
   * extracting meta-data (for example, reflection failure)
   */
  T processMetaData(@NonNull DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException;

}
