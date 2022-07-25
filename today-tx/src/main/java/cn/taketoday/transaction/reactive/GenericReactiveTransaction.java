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

package cn.taketoday.transaction.reactive;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.ReactiveTransaction;

/**
 * Default implementation of the {@link ReactiveTransaction} interface,
 * used by {@link AbstractReactiveTransactionManager}. Based on the concept
 * of an underlying "transaction object".
 *
 * <p>Holds all status information that {@link AbstractReactiveTransactionManager}
 * needs internally, including a generic transaction object determined by the
 * concrete transaction manager implementation.
 *
 * <p><b>NOTE:</b> This is <i>not</i> intended for use with other ReactiveTransactionManager
 * implementations, in particular not for mock transaction managers in testing environments.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @see AbstractReactiveTransactionManager
 * @see #getTransaction
 * @since 4.0
 */
public class GenericReactiveTransaction implements ReactiveTransaction {

  @Nullable
  private final Object transaction;

  private final boolean newTransaction;

  private final boolean newSynchronization;

  private final boolean readOnly;

  private final boolean debug;

  @Nullable
  private final Object suspendedResources;

  private boolean rollbackOnly = false;

  private boolean completed = false;

  /**
   * Create a new {@code DefaultReactiveTransactionStatus} instance.
   *
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
  public GenericReactiveTransaction(
          @Nullable Object transaction, boolean newTransaction, boolean newSynchronization,
          boolean readOnly, boolean debug, @Nullable Object suspendedResources) {

    this.transaction = transaction;
    this.newTransaction = newTransaction;
    this.newSynchronization = newSynchronization;
    this.readOnly = readOnly;
    this.debug = debug;
    this.suspendedResources = suspendedResources;
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

  /**
   * Return whether there is an actual transaction active.
   */
  public boolean hasTransaction() {
    return (this.transaction != null);
  }

  @Override
  public boolean isNewTransaction() {
    return (hasTransaction() && this.newTransaction);
  }

  /**
   * Return if a new transaction synchronization has been opened
   * for this transaction.
   */
  public boolean isNewSynchronization() {
    return this.newSynchronization;
  }

  /**
   * Return if this transaction is defined as read-only transaction.
   */
  public boolean isReadOnly() {
    return this.readOnly;
  }

  /**
   * Return whether the progress of this transaction is debugged. This is used by
   * {@link AbstractReactiveTransactionManager} as an optimization, to prevent repeated
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

  @Override
  public void setRollbackOnly() {
    this.rollbackOnly = true;
  }

  /**
   * Determine the rollback-only flag via checking this ReactiveTransactionStatus.
   * <p>Will only return "true" if the application called {@code setRollbackOnly}
   * on this TransactionStatus object.
   */
  @Override
  public boolean isRollbackOnly() {
    return this.rollbackOnly;
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

}
