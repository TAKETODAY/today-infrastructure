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

package cn.taketoday.transaction.jta;

import javax.transaction.xa.XAResource;

import cn.taketoday.lang.Assert;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

/**
 * Adapter for a managed JTA Transaction handle, taking a JTA
 * {@link TransactionManager} reference and creating
 * a JTA {@link Transaction} handle for it.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ManagedTransactionAdapter implements Transaction {

  private final TransactionManager transactionManager;

  /**
   * Create a new ManagedTransactionAdapter for the given TransactionManager.
   *
   * @param transactionManager the JTA TransactionManager to wrap
   */
  public ManagedTransactionAdapter(TransactionManager transactionManager) throws SystemException {
    Assert.notNull(transactionManager, "TransactionManager is required");
    this.transactionManager = transactionManager;
  }

  /**
   * Return the JTA TransactionManager that this adapter delegates to.
   */
  public final TransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  @Override
  public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
          SecurityException, SystemException {
    this.transactionManager.commit();
  }

  @Override
  public void rollback() throws SystemException {
    this.transactionManager.rollback();
  }

  @Override
  public void setRollbackOnly() throws SystemException {
    this.transactionManager.setRollbackOnly();
  }

  @Override
  public int getStatus() throws SystemException {
    return this.transactionManager.getStatus();
  }

  @Override
  public boolean enlistResource(XAResource xaRes) throws RollbackException, SystemException {
    return this.transactionManager.getTransaction().enlistResource(xaRes);
  }

  @Override
  public boolean delistResource(XAResource xaRes, int flag) throws SystemException {
    return this.transactionManager.getTransaction().delistResource(xaRes, flag);
  }

  @Override
  public void registerSynchronization(Synchronization sync) throws RollbackException, SystemException {
    this.transactionManager.getTransaction().registerSynchronization(sync);
  }

}
