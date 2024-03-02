/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc;

import java.io.Closeable;

/**
 * Iterable {@link java.sql.ResultSet}. Needs to be closeable, because allowing
 * manual iteration means it's impossible to know when to close the ResultSet
 * and Connection.
 *
 * @author aldenquimby@gmail.com
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public abstract class ResultSetIterable<T> implements Iterable<T>, Closeable, AutoCloseable {
  private boolean autoCloseConnection = false;

  public boolean isAutoCloseConnection() {
    return this.autoCloseConnection;
  }

  public void setAutoCloseConnection(boolean autoCloseConnection) {
    this.autoCloseConnection = autoCloseConnection;
  }

  // override close to not throw
  @Override
  public abstract void close();

}
