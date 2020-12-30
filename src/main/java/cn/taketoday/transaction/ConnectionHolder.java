/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Objects;

/**
 * @author TODAY <br>
 *         2018-10-09 11:29
 */
public class ConnectionHolder extends AbstractResourceHolder {

  /**
   * Prefix for savepoint names.
   */
  public static final String SAVEPOINT_NAME_PREFIX = "SAVEPOINT_";

  private Connection connection;

  private boolean transactionActive = false;

  private Boolean savepointsSupported;

  private int savepointCounter = 0;

  public ConnectionHolder(Connection connection) {
    this.connection = connection;
  }

  public ConnectionHolder(Connection connection, boolean transactionActive) {
    this(connection);
    this.transactionActive = transactionActive;
  }

  /**
   * Return whether this holder currently has a Connection.
   */
  public boolean hasConnection() {
    return this.connection != null;
  }

  /**
   * Return whether JDBC 3.0 Savepoints are supported. Caches the flag for the
   * lifetime of this ConnectionHolder.
   *
   * @throws SQLException
   *             if thrown by the JDBC driver
   */
  public boolean supportsSavepoints() throws SQLException {
    if (this.savepointsSupported == null) {
      this.savepointsSupported = connection.getMetaData().supportsSavepoints();
    }
    return this.savepointsSupported;
  }

  /**
   * Create a new JDBC 3.0 Savepoint for the current Connection, using generated
   * savepoint names that are unique for the Connection.
   *
   * @return the new Savepoint
   * @throws SQLException
   *             if thrown by the JDBC driver
   */
  public Savepoint createSavepoint() throws SQLException {
    this.savepointCounter++;
    return connection.setSavepoint(SAVEPOINT_NAME_PREFIX + this.savepointCounter);
  }

  /**
   * Releases the current Connection held by this ConnectionHolder.
   */
  @Override
  public void released() {

    super.released();
    if (!isOpen()) {
      releaseInternal(connection);
    }
  }

  protected void releaseInternal(Connection connection) {}

  @Override
  public void clear() {
    super.clear();
    this.transactionActive = false;
    this.savepointsSupported = null;
    this.savepointCounter = 0;
  }

  /**
   * Return the current Connection held by this ConnectionHolder.
   * <p>
   * This will be the same Connection until {@code released} gets called on the
   * ConnectionHolder, which will reset the held Connection, fetching a new
   * Connection on demand.
   *
   * @see #released()
   */
  public Connection getConnection() {
    return Objects.requireNonNull(connection, "Active Connection is required");
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  /**
   * Set whether this holder represents an active, JDBC-managed transaction.
   *
   * @see DataSourceTransactionManager
   */
  public void setTransactionActive(boolean transactionActive) {
    this.transactionActive = transactionActive;
  }

  /**
   * Return whether this holder represents an active, JDBC-managed transaction.
   */
  public boolean isTransactionActive() {
    return this.transactionActive;
  }

}
