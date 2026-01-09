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

import org.jspecify.annotations.Nullable;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;

import infra.lang.Assert;
import infra.transaction.CannotCreateTransactionException;
import infra.transaction.NestedTransactionNotSupportedException;
import infra.transaction.SavepointManager;
import infra.transaction.TransactionException;
import infra.transaction.TransactionSystemException;
import infra.transaction.TransactionUsageException;
import infra.transaction.support.SmartTransactionObject;

/**
 * Convenient base class for JDBC-aware transaction objects. Can contain a
 * {@link ConnectionHolder} with a JDBC {@code Connection}, and implements the
 * {@link SavepointManager} interface based on that {@code ConnectionHolder}.
 *
 * <p>Allows for programmatic management of JDBC {@link Savepoint Savepoints}.
 * {@link infra.transaction.support.DefaultTransactionStatus}
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
    catch (SQLFeatureNotSupportedException ex) {
      // typically on Oracle - ignore
    }
    catch (SQLException ex) {
      if ("3B001".equals(ex.getSQLState())) {
        // Savepoint already released (HSQLDB, PostgreSQL, DB2) - ignore
        return;
      }
      // ignore Microsoft SQLServerException: This operation is not supported.
      String msg = ex.getMessage();
      if (msg == null || (!msg.contains("not supported") && !msg.contains("3B001"))) {
        throw new TransactionSystemException("Could not explicitly release JDBC savepoint", ex);
      }
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
