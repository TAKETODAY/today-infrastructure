/*
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

/**
 * @author TODAY <br>
 * 2018-11-16 21:29
 */
public class DefaultTransactionStatus extends AbstractTransactionStatus {

  private final boolean readOnly;
  private final Object transaction;
  private final boolean newTransaction;
  private final boolean newSynchronization;

  private final Object suspendedResources;

  /**
   * Create a new DefaultTransactionStatus instance.
   *
   * @param transaction underlying transaction object that can hold state for the internal
   * transaction implementation
   * @param newTransaction if the transaction is new, else participating in an existing
   * transaction
   * @param newSynchronization if a new transaction synchronization has been opened for the given
   * transaction
   * @param readOnly whether the transaction is read-only
   * @param suspendedResources a holder for resources that have been suspended for this
   * transaction, if any
   */
  public DefaultTransactionStatus(Object transaction, boolean newTransaction, //
                                  boolean newSynchronization, boolean readOnly, Object suspendedResources) //
  {
    this.transaction = transaction;
    this.newTransaction = newTransaction;
    this.newSynchronization = newSynchronization;
    this.readOnly = readOnly;
    this.suspendedResources = suspendedResources;
  }

  /**
   * Return the underlying transaction object.
   */
  public Object getTransaction() {
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
   * Return if a new transaction synchronization has been opened for this
   * transaction.
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
   * Return the holder for resources that have been suspended for this
   * transaction, if any.
   */
  public Object getSuspendedResources() {
    return this.suspendedResources;
  }

  /**
   * Return whether the underlying transaction implements the SavepointManager
   * interface.
   *
   * @see #getTransaction
   */
  public boolean isTransactionSavepointManager() {
    return (this.transaction instanceof SavepointManager);
  }

  @Override
  protected SavepointManager getSavepointManager() {
    if (isTransactionSavepointManager()) {
      return (SavepointManager) transaction;
    }
    return super.getSavepointManager();
  }

}
