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

package cn.taketoday.jdbc.datasource;

import java.sql.SQLException;
import java.sql.Savepoint;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.CannotCreateTransactionException;
import cn.taketoday.transaction.NestedTransactionNotSupportedException;
import cn.taketoday.transaction.SavepointManager;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.TransactionUsageException;
import cn.taketoday.transaction.support.SmartTransactionObject;

/**
 * Convenient base class for JDBC-aware transaction objects. Can contain a
 * {@link ConnectionHolder} with a JDBC {@code Connection}, and implements the
 * {@link SavepointManager} interface based on that {@code ConnectionHolder}.
 *
 * <p>Allows for programmatic management of JDBC {@link Savepoint Savepoints}.
 * {@link cn.taketoday.transaction.support.DefaultTransactionStatus}
 * automatically delegates to this, as it autodetects transaction objects which
 * implement the {@link SavepointManager} interface.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DataSourceTransactionManager
 * @since 4.0
 */
public abstract class JdbcTransactionObjectSupport implements SavepointManager, SmartTransactionObject {

  @Nullable
  private ConnectionHolder connectionHolder;

  @Nullable
  private Integer previousIsolationLevel;

  private boolean readOnly = false;

  private boolean savepointAllowed = false;

  /**
   * Set the ConnectionHolder for this transaction object.
   */
  public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder) {
    this.connectionHolder = connectionHolder;
  }

  /**
   * Return the ConnectionHolder for this transaction object.
   */
  public ConnectionHolder getConnectionHolder() {
    Assert.state(this.connectionHolder != null, "No ConnectionHolder available");
    return this.connectionHolder;
  }

  /**
   * Check whether this transaction object has a ConnectionHolder.
   */
  public boolean hasConnectionHolder() {
    return (this.connectionHolder != null);
  }

  /**
   * Set the previous isolation level to retain, if any.
   */
  public void setPreviousIsolationLevel(@Nullable Integer previousIsolationLevel) {
    this.previousIsolationLevel = previousIsolationLevel;
  }

  /**
   * Return the retained previous isolation level, if any.
   */
  @Nullable
  public Integer getPreviousIsolationLevel() {
    return this.previousIsolationLevel;
  }

  /**
   * Set the read-only status of this transaction.
   * The default is {@code false}.
   */
  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  /**
   * Return the read-only status of this transaction.
   */
  public boolean isReadOnly() {
    return this.readOnly;
  }

  /**
   * Set whether savepoints are allowed within this transaction.
   * The default is {@code false}.
   */
  public void setSavepointAllowed(boolean savepointAllowed) {
    this.savepointAllowed = savepointAllowed;
  }

  /**
   * Return whether savepoints are allowed within this transaction.
   */
  public boolean isSavepointAllowed() {
    return this.savepointAllowed;
  }

  //---------------------------------------------------------------------
  // Implementation of SavepointManager
  //---------------------------------------------------------------------

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
        throw new CannotCreateTransactionException(
                "Cannot create savepoint for transaction which is already marked as rollback-only");
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
   * @see java.sql.Connection#rollback(Savepoint)
   */
  @Override
  public void rollbackToSavepoint(Object savepoint) throws TransactionException {
    ConnectionHolder conHolder = getConnectionHolderForSavepoint();
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
    ConnectionHolder conHolder = getConnectionHolderForSavepoint();
    try {
      conHolder.getConnection().releaseSavepoint((Savepoint) savepoint);
    }
    catch (Throwable ex) {
      throw new TransactionSystemException("Could not explicitly release JDBC savepoint", ex);
    }
  }

  protected ConnectionHolder getConnectionHolderForSavepoint() throws TransactionException {
    if (!isSavepointAllowed()) {
      throw new NestedTransactionNotSupportedException(
              "Transaction manager does not allow nested transactions");
    }
    if (!hasConnectionHolder()) {
      throw new TransactionUsageException(
              "Cannot create nested transaction when not exposing a JDBC transaction");
    }
    return getConnectionHolder();
  }

}
