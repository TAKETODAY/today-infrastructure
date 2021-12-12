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

package cn.taketoday.transaction.jta;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

/**
 * Adapter that implements the JTA {@link Synchronization}
 * interface delegating to an underlying Spring
 * {@link cn.taketoday.transaction.support.TransactionSynchronization}.
 *
 * <p>Useful for synchronizing Spring resource management code with plain
 * JTA / EJB CMT transactions, despite the original code being built for
 * Spring transaction synchronization.
 *
 * @author Juergen Hoeller
 * @see jakarta.transaction.Transaction#registerSynchronization
 * @see cn.taketoday.transaction.support.TransactionSynchronization
 * @since 4.0
 */
public class JtaSynchronizationAdapter implements Synchronization {

  protected static final Logger logger = LoggerFactory.getLogger(JtaSynchronizationAdapter.class);

  private final TransactionSynchronization springSynchronization;

  @Nullable
  private UserTransaction jtaTransaction;

  private boolean beforeCompletionCalled = false;

  /**
   * Create a new SpringJtaSynchronizationAdapter for the given Spring
   * TransactionSynchronization and JTA TransactionManager.
   *
   * @param springSynchronization the Spring TransactionSynchronization to delegate to
   */
  public JtaSynchronizationAdapter(TransactionSynchronization springSynchronization) {
    Assert.notNull(springSynchronization, "TransactionSynchronization must not be null");
    this.springSynchronization = springSynchronization;
  }

  /**
   * Create a new SpringJtaSynchronizationAdapter for the given Spring
   * TransactionSynchronization and JTA TransactionManager.
   * <p>Note that this adapter will never perform a rollback-only call on WebLogic,
   * since WebLogic Server is known to automatically mark the transaction as
   * rollback-only in case of a {@code beforeCompletion} exception. Hence,
   * on WLS, this constructor is equivalent to the single-arg constructor.
   *
   * @param springSynchronization the Spring TransactionSynchronization to delegate to
   * @param jtaUserTransaction the JTA UserTransaction to use for rollback-only
   * setting in case of an exception thrown in {@code beforeCompletion}
   * (can be omitted if the JTA provider itself marks the transaction rollback-only
   * in such a scenario, which is required by the JTA specification as of JTA 1.1).
   */
  public JtaSynchronizationAdapter(TransactionSynchronization springSynchronization,
                                   @Nullable UserTransaction jtaUserTransaction) {

    this(springSynchronization);
    this.jtaTransaction = jtaUserTransaction;
  }

  /**
   * Create a new SpringJtaSynchronizationAdapter for the given Spring
   * TransactionSynchronization and JTA TransactionManager.
   * <p>Note that this adapter will never perform a rollback-only call on WebLogic,
   * since WebLogic Server is known to automatically mark the transaction as
   * rollback-only in case of a {@code beforeCompletion} exception. Hence,
   * on WLS, this constructor is equivalent to the single-arg constructor.
   *
   * @param springSynchronization the Spring TransactionSynchronization to delegate to
   * @param jtaTransactionManager the JTA TransactionManager to use for rollback-only
   * setting in case of an exception thrown in {@code beforeCompletion}
   * (can be omitted if the JTA provider itself marks the transaction rollback-only
   * in such a scenario, which is required by the JTA specification as of JTA 1.1)
   */
  public JtaSynchronizationAdapter(
          TransactionSynchronization springSynchronization, @Nullable TransactionManager jtaTransactionManager) {

    this(springSynchronization);
    this.jtaTransaction = new UserTransactionAdapter(jtaTransactionManager);
  }

  /**
   * JTA {@code beforeCompletion} callback: just invoked before commit.
   * <p>In case of an exception, the JTA transaction will be marked as rollback-only.
   *
   * @see cn.taketoday.transaction.support.TransactionSynchronization#beforeCommit
   */
  @Override
  public void beforeCompletion() {
    try {
      boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
      this.springSynchronization.beforeCommit(readOnly);
    }
    catch (RuntimeException | Error ex) {
      setRollbackOnlyIfPossible();
      throw ex;
    }
    finally {
      // Process Spring's beforeCompletion early, in order to avoid issues
      // with strict JTA implementations that issue warnings when doing JDBC
      // operations after transaction completion (e.g. Connection.getWarnings).
      this.beforeCompletionCalled = true;
      this.springSynchronization.beforeCompletion();
    }
  }

  /**
   * Set the underlying JTA transaction to rollback-only.
   */
  private void setRollbackOnlyIfPossible() {
    if (this.jtaTransaction != null) {
      try {
        this.jtaTransaction.setRollbackOnly();
      }
      catch (UnsupportedOperationException ex) {
        // Probably Hibernate's WebSphereExtendedJTATransactionLookup pseudo JTA stuff...
        logger.debug("JTA transaction handle does not support setRollbackOnly method - " +
                "relying on JTA provider to mark the transaction as rollback-only based on " +
                "the exception thrown from beforeCompletion", ex);
      }
      catch (Throwable ex) {
        logger.error("Could not set JTA transaction rollback-only", ex);
      }
    }
    else {
      logger.debug("No JTA transaction handle available and/or running on WebLogic - " +
              "relying on JTA provider to mark the transaction as rollback-only based on " +
              "the exception thrown from beforeCompletion");
    }
  }

  /**
   * JTA {@code afterCompletion} callback: invoked after commit/rollback.
   * <p>Needs to invoke the Spring synchronization's {@code beforeCompletion}
   * at this late stage in case of a rollback, since there is no corresponding
   * callback with JTA.
   *
   * @see cn.taketoday.transaction.support.TransactionSynchronization#beforeCompletion
   * @see cn.taketoday.transaction.support.TransactionSynchronization#afterCompletion
   */
  @Override
  public void afterCompletion(int status) {
    if (!this.beforeCompletionCalled) {
      // beforeCompletion not called before (probably because of JTA rollback).
      // Perform the cleanup here.
      this.springSynchronization.beforeCompletion();
    }
    // Call afterCompletion with the appropriate status indication.
    switch (status) {
      case Status.STATUS_COMMITTED -> springSynchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
      case Status.STATUS_ROLLEDBACK -> springSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
      default -> springSynchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    }
  }

}
