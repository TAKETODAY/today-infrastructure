/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.datasource;

import java.sql.Connection;

import javax.sql.DataSource;

import infra.jdbc.core.JdbcTemplate;

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
 * @see JdbcTemplate
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
