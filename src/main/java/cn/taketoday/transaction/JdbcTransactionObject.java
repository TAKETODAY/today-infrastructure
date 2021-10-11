/**
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import java.sql.SQLException;
import java.sql.Savepoint;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY <br>
 * 2018-11-13 16:58
 */
public abstract class JdbcTransactionObject implements TransactionObject, SavepointManager {

  private static final Logger log = LoggerFactory.getLogger(JdbcTransactionObject.class);

  private Integer previousIsolationLevel;
  private boolean savepointAllowed = false;
  private ConnectionHolder connectionHolder;

  public boolean hasConnectionHolder() {
    return (this.connectionHolder != null);
  }

  @Override
  public void flush() { }

  // ---------------------------------------------------------------------
  // Implementation of SavepointManager
  // ---------------------------------------------------------------------

  /**
   * This implementation creates a JDBC 3.0 Savepoint and returns it.
   *
   * @see java.sql.Connection#setSavepoint
   */
  @Override
  public Object createSavepoint() throws TransactionException {
    ConnectionHolder conHolder = getConnectionHolderForSavepoint();
    try {
      if (!conHolder.supportsSavepoints()) {
        throw new NestedTransactionNotSupportedException(
                "Cannot create a nested transaction because savepoints are not supported by your JDBC driver");
      }
      if (conHolder.isRollbackOnly()) {
        throw new CannotCreateTransactionException("Cannot create savepoint for transaction which is already marked as rollback-only");
      }
      return conHolder.createSavepoint();
    }
    catch (SQLException ex) {
      throw new CannotCreateTransactionException("Could not create JDBC savepoint", ex);
    }
  }

  /**
   * This implementation rolls back to the given JDBC 3.0 Savepoint.
   *
   * @see java.sql.Connection#rollback(java.sql.Savepoint)
   */
  @Override
  public void rollbackToSavepoint(Object savepoint) throws TransactionException {
    final ConnectionHolder conHolder = getConnectionHolderForSavepoint();
    try {
      conHolder.getConnection().rollback((Savepoint) savepoint);
      conHolder.resetRollbackOnly();
    }
    catch (Throwable ex) {
      throw new TransactionSystemException("Could not roll back to JDBC savepoint", ex);
    }
  }

  /**
   * This implementation releases the given JDBC 3.0 Savepoint.
   *
   * @see java.sql.Connection#releaseSavepoint
   */
  @Override
  public void releaseSavepoint(Object savepoint) throws TransactionException {
    final ConnectionHolder conHolder = getConnectionHolderForSavepoint();
    try {
      conHolder.getConnection().releaseSavepoint((Savepoint) savepoint);
    }
    catch (Throwable ex) {
      log.debug("Could not explicitly release JDBC savepoint", ex);
    }
  }

  protected ConnectionHolder getConnectionHolderForSavepoint() throws TransactionException {
    if (!isSavepointAllowed()) {
      throw new NestedTransactionNotSupportedException("Transaction manager does not allow nested transactions");
    }
    if (!hasConnectionHolder()) {
      throw new CannotCreateTransactionException("Cannot create nested transaction when not exposing a JDBC transaction");
    }
    return getConnectionHolder();
  }

  public Integer getPreviousIsolationLevel() {
    return previousIsolationLevel;
  }

  public void setPreviousIsolationLevel(Integer previousIsolationLevel) {
    this.previousIsolationLevel = previousIsolationLevel;
  }

  public boolean isSavepointAllowed() {
    return savepointAllowed;
  }

  public void setSavepointAllowed(boolean savepointAllowed) {
    this.savepointAllowed = savepointAllowed;
  }

  public ConnectionHolder getConnectionHolder() {
    return connectionHolder;
  }

  public void setConnectionHolder(ConnectionHolder connectionHolder) {
    this.connectionHolder = connectionHolder;
  }

}
