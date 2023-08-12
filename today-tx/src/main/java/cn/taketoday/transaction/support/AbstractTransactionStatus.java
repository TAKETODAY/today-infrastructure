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

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.NestedTransactionNotSupportedException;
import cn.taketoday.transaction.SavepointManager;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.TransactionUsageException;

/**
 * Abstract base implementation of the
 * {@link cn.taketoday.transaction.TransactionStatus} interface.
 *
 * <p>Pre-implements the handling of local rollback-only and completed flags, and
 * delegation to an underlying {@link cn.taketoday.transaction.SavepointManager}.
 * Also offers the option of a holding a savepoint within the transaction.
 *
 * <p>Does not assume any specific internal transaction handling, such as an
 * underlying transaction object, and no transaction synchronization mechanism.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setRollbackOnly()
 * @see #isRollbackOnly()
 * @see #setCompleted()
 * @see #isCompleted()
 * @see #getSavepointManager()
 * @see SimpleTransactionStatus
 * @see DefaultTransactionStatus
 * @since 4.0
 */
public abstract class AbstractTransactionStatus implements TransactionStatus {

  private boolean rollbackOnly = false;

  private boolean completed = false;

  @Nullable
  private Object savepoint;

  //---------------------------------------------------------------------
  // Implementation of TransactionExecution
  //---------------------------------------------------------------------

  @Override
  public void setRollbackOnly() {
    if (this.completed) {
      throw new IllegalStateException("Transaction completed");
    }
    this.rollbackOnly = true;
  }

  /**
   * Determine the rollback-only flag via checking both the local rollback-only flag
   * of this TransactionStatus and the global rollback-only flag of the underlying
   * transaction, if any.
   *
   * @see #isLocalRollbackOnly()
   * @see #isGlobalRollbackOnly()
   */
  @Override
  public boolean isRollbackOnly() {
    return (isLocalRollbackOnly() || isGlobalRollbackOnly());
  }

  /**
   * Determine the rollback-only flag via checking this TransactionStatus.
   * <p>Will only return "true" if the application called {@code setRollbackOnly}
   * on this TransactionStatus object.
   */
  public boolean isLocalRollbackOnly() {
    return this.rollbackOnly;
  }

  /**
   * Template method for determining the global rollback-only flag of the
   * underlying transaction, if any.
   * <p>This implementation always returns {@code false}.
   */
  public boolean isGlobalRollbackOnly() {
    return false;
  }

  /**
   * Mark this transaction as completed, that is, committed or rolled back.
   */
  public void setCompleted() {
    this.completed = true;
  }

  @Override
  public boolean isCompleted() {
    return this.completed;
  }

  //---------------------------------------------------------------------
  // Handling of current savepoint state
  //---------------------------------------------------------------------

  @Override
  public boolean hasSavepoint() {
    return (this.savepoint != null);
  }

  /**
   * Set a savepoint for this transaction. Useful for PROPAGATION_NESTED.
   *
   * @see cn.taketoday.transaction.TransactionDefinition#PROPAGATION_NESTED
   */
  protected void setSavepoint(@Nullable Object savepoint) {
    this.savepoint = savepoint;
  }

  /**
   * Get the savepoint for this transaction, if any.
   */
  @Nullable
  protected Object getSavepoint() {
    return this.savepoint;
  }

  /**
   * Create a savepoint and hold it for the transaction.
   *
   * @throws cn.taketoday.transaction.NestedTransactionNotSupportedException if the underlying transaction does not support savepoints
   */
  public void createAndHoldSavepoint() throws TransactionException {
    setSavepoint(getSavepointManager().createSavepoint());
  }

  /**
   * Roll back to the savepoint that is held for the transaction
   * and release the savepoint right afterwards.
   */
  public void rollbackToHeldSavepoint() throws TransactionException {
    Object savepoint = getSavepoint();
    if (savepoint == null) {
      throw new TransactionUsageException(
              "Cannot roll back to savepoint - no savepoint associated with current transaction");
    }
    SavepointManager savepointManager = getSavepointManager();
    savepointManager.rollbackToSavepoint(savepoint);
    savepointManager.releaseSavepoint(savepoint);
    setSavepoint(null);
  }

  /**
   * Release the savepoint that is held for the transaction.
   */
  public void releaseHeldSavepoint() throws TransactionException {
    Object savepoint = getSavepoint();
    if (savepoint == null) {
      throw new TransactionUsageException(
              "Cannot release savepoint - no savepoint associated with current transaction");
    }
    getSavepointManager().releaseSavepoint(savepoint);
    setSavepoint(null);
  }

  //---------------------------------------------------------------------
  // Implementation of SavepointManager
  //---------------------------------------------------------------------

  /**
   * This implementation delegates to a SavepointManager for the
   * underlying transaction, if possible.
   *
   * @see #getSavepointManager()
   * @see SavepointManager#createSavepoint()
   */
  @Override
  public Object createSavepoint() throws TransactionException {
    return getSavepointManager().createSavepoint();
  }

  /**
   * This implementation delegates to a SavepointManager for the
   * underlying transaction, if possible.
   *
   * @see #getSavepointManager()
   * @see SavepointManager#rollbackToSavepoint(Object)
   */
  @Override
  public void rollbackToSavepoint(Object savepoint) throws TransactionException {
    getSavepointManager().rollbackToSavepoint(savepoint);
  }

  /**
   * This implementation delegates to a SavepointManager for the
   * underlying transaction, if possible.
   *
   * @see #getSavepointManager()
   * @see SavepointManager#releaseSavepoint(Object)
   */
  @Override
  public void releaseSavepoint(Object savepoint) throws TransactionException {
    getSavepointManager().releaseSavepoint(savepoint);
  }

  /**
   * Return a SavepointManager for the underlying transaction, if possible.
   * <p>Default implementation always throws a NestedTransactionNotSupportedException.
   *
   * @throws cn.taketoday.transaction.NestedTransactionNotSupportedException if the underlying transaction does not support savepoints
   */
  protected SavepointManager getSavepointManager() {
    throw new NestedTransactionNotSupportedException("This transaction does not support savepoints");
  }

}
