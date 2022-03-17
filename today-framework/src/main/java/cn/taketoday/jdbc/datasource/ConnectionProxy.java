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

/**
 * Subinterface of {@link Connection} to be implemented by
 * Connection proxies. Allows access to the underlying target Connection.
 *
 * <p>This interface can be checked when there is a need to cast to a
 * native JDBC Connection such as Oracle's OracleConnection. Alternatively,
 * all such connections also support JDBC 4.0's {@link Connection#unwrap}.
 *
 * @author Juergen Hoeller
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
