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

package infra.transaction.jta;

import javax.transaction.xa.XAResource;

import infra.lang.Assert;
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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
