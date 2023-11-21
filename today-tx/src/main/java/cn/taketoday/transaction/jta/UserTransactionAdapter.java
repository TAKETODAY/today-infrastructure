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

import cn.taketoday.lang.Assert;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

/**
 * Adapter for a JTA UserTransaction handle, taking a JTA
 * {@link TransactionManager} reference and creating
 * a JTA {@link UserTransaction} handle for it.
 *
 * <p>The JTA UserTransaction interface is an exact subset of the JTA
 * TransactionManager interface. Unfortunately, it does not serve as
 * super-interface of TransactionManager, though, which requires an
 * adapter such as this class to be used when intending to talk to
 * a TransactionManager handle through the UserTransaction interface.
 *
 * <p>Used internally by Framework's {@link JtaTransactionManager} for certain
 * scenarios. Not intended for direct use in application code.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class UserTransactionAdapter implements UserTransaction {

  private final TransactionManager transactionManager;

  /**
   * Create a new UserTransactionAdapter for the given TransactionManager.
   *
   * @param transactionManager the JTA TransactionManager to wrap
   */
  public UserTransactionAdapter(TransactionManager transactionManager) {
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
  public void setTransactionTimeout(int timeout) throws SystemException {
    this.transactionManager.setTransactionTimeout(timeout);
  }

  @Override
  public void begin() throws NotSupportedException, SystemException {
    this.transactionManager.begin();
  }

  @Override
  public void commit()
          throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
          SecurityException, SystemException {
    this.transactionManager.commit();
  }

  @Override
  public void rollback() throws SecurityException, SystemException {
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

}
