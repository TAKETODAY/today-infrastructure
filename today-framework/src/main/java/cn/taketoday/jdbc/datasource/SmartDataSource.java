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

package cn.taketoday.jdbc.datasource;

import java.sql.Connection;

import javax.sql.DataSource;

/**
 * Extension of the {@code javax.sql.DataSource} interface, to be
 * implemented by special DataSources that return JDBC Connections
 * in an unwrapped fashion.
 *
 * <p>Classes using this interface can query whether or not the Connection
 * should be closed after an operation.  DataSourceUtils and
 * JdbcTemplate classes automatically perform such a check.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SingleConnectionDataSource#shouldClose
 * @see DataSourceUtils#releaseConnection
 * @see cn.taketoday.jdbc.core.JdbcTemplate
 * @since 4.0
 */
public interface SmartDataSource extends DataSource {

  /**
   * Should we close this Connection, obtained from this DataSource?
   * <p>Code that uses Connections from a SmartDataSource should always
   * perform a check via this method before invoking {@code close()}.
   * <p>Note that the JdbcTemplate class in the 'jdbc.core' package takes care of
   * releasing JDBC Connections, freeing application code of this responsibility.
   *
   * @param con the Connection to check
   * @return whether the given Connection should be closed
   * @see Connection#close()
   */
  boolean shouldClose(Connection con);

}
