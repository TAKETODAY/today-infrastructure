/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import infra.jdbc.datasource.WrappedConnection;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * @author TODAY
 */
final class NestedConnection extends WrappedConnection {

  private static final Logger log = LoggerFactory.getLogger(NestedConnection.class);

  private boolean autocommit = true;

  NestedConnection(Connection source) {
    super(source);
  }

  private boolean commited = false;

  @Override
  public void commit() throws SQLException {
    commited = true;
    //do nothing, parent connection should be committed
  }

  @Override
  public void rollback() throws SQLException {
    if (!commited) {
      log.warn("rollback of nested transaction leads to rollback of parent transaction. Maybe it is not wat you want.");
      super.rollback(); //probably it's worth to use savepoints
    }
  }

  @Override
  public void close() throws SQLException {
    //do nothing, parent connection should be closed by someone who cares
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    //do nothing, parent connection should be configured
  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    return autocommit;
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    this.autocommit = autoCommit;
  }

}
