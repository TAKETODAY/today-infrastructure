/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import cn.taketoday.jdbc.support.WrappedConnection;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY
 */
final class NestedConnection extends WrappedConnection {
  private final static Logger log = LoggerFactory.getLogger(NestedConnection.class);

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
