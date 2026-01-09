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

/**
 * Sub-interface of {@link Connection} to be implemented by
 * Connection proxies. Allows access to the underlying target Connection.
 *
 * <p>This interface can be checked when there is a need to cast to a
 * native JDBC Connection such as Oracle's OracleConnection. Alternatively,
 * all such connections also support JDBC 4.0's {@link Connection#unwrap}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see TransactionAwareDataSourceProxy
 * @see LazyConnectionDataSourceProxy
 * @see DataSourceUtils#getTargetConnection(Connection)
 * @since 4.0
 */
public interface ConnectionProxy extends Connection {

  /**
   * Return the target Connection of this proxy.
   * <p>This will typically be the native driver Connection
   * or a wrapper from a connection pool.
   *
   * @return the underlying Connection (never {@code null})
   */
  Connection getTargetConnection();

}
