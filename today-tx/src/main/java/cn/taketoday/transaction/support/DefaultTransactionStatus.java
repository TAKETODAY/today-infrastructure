/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.transaction.support;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.NestedTransactionNotSupportedException;
import cn.taketoday.transaction.SavepointManager;

/**
 * Default implementation of the {@link cn.taketoday.transaction.TransactionStatus}
 * interface, used by {@link AbstractPlatformTransactionManager}. Based on the concept
 * of an underlying "transaction object".
 *
 * <p>Holds all status information that {@link AbstractPlatformTransactionManager}
 * needs internally, including a generic transaction object determined by the
 * concrete transaction manager implementation.
 *
 * <p>Supports delegating savepoint-related methods to a transaction object
 * that implements the {@link SavepointManager} interface.
 *
 * <p><b>NOTE:</b> This is <i>not</i> intended for use with other PlatformTransactionManager
 * implementations, in particular not for mock transaction managers in testing environments.
 * Use the alternative {@link SimpleTransactionStatus} class or a mock for the plain
 * {@link cn.taketoday.transaction.TransactionStatus} interface instead.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractPlatformTransactionManager
 * @see cn.taketoday.transaction.SavepointManager
 * @see #getTransaction
 * @see #createSavepoint
 * @see #rollbackToSavepoint
 * @see #releaseSavepoint
 * @see SimpleTransactionStatus
 * @since 4.0
 */
public class DefaultTransactionStatus extends AbstractTransactionStatus {

  @Nullable
  private final String transactionName;

  @Nullable
  private final Object transaction;

  private final boolean newTransaction;

  private final boolean newSynchronization;

  private final boolean nested;

  private final boolean readOnly;

  private final boolean debug;

  @Nullable
  private final Object suspendedResources;

  /**
   * Create a new {@code DefaultTransactionStatus} instance.
   *
   * @param transactionName the defined name of the transaction
   * @param transaction underlying transaction object that can hold state
   * for the internal transaction implementation
   * @param newTransaction if the transaction is new, otherwise participating
   * in an existing transaction
   * @param newSynchronization if a new transaction synchronization has been
   * opened for the given transaction
   * @param readOnly whether the transaction is marked as read-only
   * @param debug should debug logging be enabled for the handling of this transaction?
   * Caching it in here can prevent repeated calls to ask the logging system whether
   * debug logging should be enabled.
   * @param suspendedResources a holder for resources that have been suspended
   * for this transaction, if any
   */
  public DefaultTransactionStatus(@Nullable String transactionName, @Nullable Object transaction,
          boolean newTransaction, boolean newSynchronization, boolean nested, boolean readOnly, boolean debug,
          @Nullable Object suspendedResources) {

    this.transactionName = transactionName;
    this.transaction = transaction;
    this.newTransaction = newTransaction;
    this.newSynchronization = newSynchronization;
    this.nested = nested;
    this.readOnly = readOnly;
    this.debug = debug;
    this.suspendedResources = suspendedResources;
  }

  @Override
  public String getTransactionName() {
    return (this.transactionName != null ? this.transactionName : "");
  }

  /**
   * Return the underlying transaction object.
   *
   * @throws IllegalStateException if no transaction is active
   */
  public Object getTransaction() {
    Assert.state(this.transaction != null, "No transaction active");
    return this.transaction;
  }

  @Override
  public boolean hasTransaction() {
    return (this.transaction != null);
  }

  @Override
  public boolean isNewTransaction() {
    return (hasTransaction() && this.newTransaction);
  }

  /**
   * Return if a new transaction synchronization has been opened for this transaction.
   */
  public boolean isNewSynchronization() {
    return this.newSynchronization;
  }

  @Override
  public boolean isNested() {
    return this.nested;
  }

  @Override
  public boolean isReadOnly() {
    return this.readOnly;
  }

  /**
   * Return whether the progress of this transaction is debugged. This is used by
   * {@link AbstractPlatformTransactionManager} as an optimization, to prevent repeated
   * calls to {@code logger.isDebugEnabled()}. Not really intended for client code.
   */
  public boolean isDebug() {
    return this.debug;
  }

  /**
   * Return the holder for resources that have been suspended for this transaction,
   * if any.
   */
  @Nullable
  public Object getSuspendedResources() {
    return this.suspendedResources;
  }

  //---------------------------------------------------------------------
  // Enable functionality through underlying transaction object
  //---------------------------------------------------------------------

  /**
   * Determine the rollback-only flag via checking the transaction object, provided
   * that the latter implements the {@link SmartTransactionObject} interface.
   * <p>Will return {@code true} if the global transaction itself has been marked
   * rollback-only by the transaction coordinator, for example in case of a timeout.
   *
   * @see SmartTransactionObject#isRollbackOnly()
   */
  @Override
  public boolean isGlobalRollbackOnly() {
    return transaction instanceof SmartTransactionObject smartTransactionObject
            && smartTransactionObject.isRollbackOnly();
  }

  /**
   * This implementation exposes the {@link SavepointManager} interface
   * of the underlying transaction object, if any.
   *
   * @throws NestedTransactionNotSupportedException if savepoints are not supported
   * @see #isTransactionSavepointManager()
   */
  @Override
  protected SavepointManager getSavepointManager() {
    Object transaction = this.transaction;
    if (!(transaction instanceof SavepointManager savepointManager)) {
      throw new NestedTransactionNotSupportedException(
              "Transaction object [" + this.transaction + "] does not support savepoints");
    }
    return savepointManager;
  }

  /**
   * Return whether the underlying transaction implements the {@link SavepointManager}
   * interface and therefore supports savepoints.
   *
   * @see #getTransaction()
   * @see #getSavepointManager()
   */
  public boolean isTransactionSavepointManager() {
    return transaction instanceof SavepointManager;
  }

  /**
   * Delegate the flushing to the transaction object, provided that the latter
   * implements the {@link SmartTransactionObject} interface.
   *
   * @see SmartTransactionObject#flush()
   */
  @Override
  public void flush() {
    if (transaction instanceof SmartTransactionObject sto) {
      sto.flush();
    }
  }

}
