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
package cn.taketoday.orm.mybatis;

import com.mockrunner.mock.jdbc.MockDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;

final class PooledMockDataSource extends MockDataSource {

  private int connectionCount = 0;

  private final LinkedList<Connection> connections = new LinkedList<>();

  @Override
  public Connection getConnection() throws SQLException {
    if (connections.isEmpty()) {
      throw new SQLException("Sorry, I ran out of connections");
    }
    ++this.connectionCount;
    return this.connections.removeLast();
  }

  int getConnectionCount() {
    return this.connectionCount;
  }

  void reset() {
    this.connectionCount = 0;
    this.connections.clear();
  }

  @Override
  public void setupConnection(Connection connection) {
    throw new UnsupportedOperationException("used addConnection() instead");
  }

  public void addConnection(Connection c) {
    this.connections.add(c);
  }

}
