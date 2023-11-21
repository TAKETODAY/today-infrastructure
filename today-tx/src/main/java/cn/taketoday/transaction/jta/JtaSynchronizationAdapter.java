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
 * interface delegating to an underlying Framework
 * {@link cn.taketoday.transaction.support.TransactionSynchronization}.
 *
 * <p>Useful for synchronizing Framework resource management code with plain
 * JTA / EJB CMT transactions, despite the original code being built for
 * Framework transaction synchronization.
 *
 * @author Juergen Hoeller
 * @see jakarta.transaction.Transaction#registerSynchronization
 * @see cn.taketoday.transaction.support.TransactionSynchronization
 * @since 4.0
 */
public class JtaSynchronizationAdapter implements Synchronization {

  protected static final Logger logger = LoggerFactory.getLogger(JtaSynchronizationAdapter.class);

  private final TransactionSynchronization frameworkSynchronization;

  @Nullable
  private UserTransaction jtaTransaction;

  private boolean beforeCompletionCalled = false;

  /**
   * Create a new FrameworkJtaSynchronizationAdapter for the given Framework
   * TransactionSynchronization and JTA TransactionManager.
   *
   * @param frameworkSynchronization the Framework TransactionSynchronization to delegate to
   */
  public JtaSynchronizationAdapter(TransactionSynchronization frameworkSynchronization) {
    Assert.notNull(frameworkSynchronization, "TransactionSynchronization is required");
    this.frameworkSynchronization = frameworkSynchronization;
  }

  /**
   * Create a new FrameworkJtaSynchronizationAdapter for the given Framework
   * TransactionSynchronization and JTA TransactionManager.
   * <p>Note that this adapter will never perform a rollback-only call on WebLogic,
   * since WebLogic Server is known to automatically mark the transaction as
   * rollback-only in case of a {@code beforeCompletion} exception. Hence,
   * on WLS, this constructor is equivalent to the single-arg constructor.
   *
   * @param frameworkSynchronization the Framework TransactionSynchronization to delegate to
   * @param jtaUserTransaction the JTA UserTransaction to use for rollback-only
   * setting in case of an exception thrown in {@code beforeCompletion}
   * (can be omitted if the JTA provider itself marks the transaction rollback-only
   * in such a scenario, which is required by the JTA specification as of JTA 1.1).
   */
  public JtaSynchronizationAdapter(TransactionSynchronization frameworkSynchronization,
                                   @Nullable UserTransaction jtaUserTransaction) {

    this(frameworkSynchronization);
    this.jtaTransaction = jtaUserTransaction;
  }

  /**
   * Create a new FrameworkJtaSynchronizationAdapter for the given Framework
   * TransactionSynchronization and JTA TransactionManager.
   * <p>Note that this adapter will never perform a rollback-only call on WebLogic,
   * since WebLogic Server is known to automatically mark the transaction as
   * rollback-only in case of a {@code beforeCompletion} exception. Hence,
   * on WLS, this constructor is equivalent to the single-arg constructor.
   *
   * @param frameworkSynchronization the Framework TransactionSynchronization to delegate to
   * @param jtaTransactionManager the JTA TransactionManager to use for rollback-only
   * setting in case of an exception thrown in {@code beforeCompletion}
   * (can be omitted if the JTA provider itself marks the transaction rollback-only
   * in such a scenario, which is required by the JTA specification as of JTA 1.1)
   */
  public JtaSynchronizationAdapter(
          TransactionSynchronization frameworkSynchronization, @Nullable TransactionManager jtaTransactionManager) {

    this(frameworkSynchronization);
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
      this.frameworkSynchronization.beforeCommit(readOnly);
    }
    catch (RuntimeException | Error ex) {
      setRollbackOnlyIfPossible();
      throw ex;
    }
    finally {
      // Process Framework's beforeCompletion early, in order to avoid issues
      // with strict JTA implementations that issue warnings when doing JDBC
      // operations after transaction completion (e.g. Connection.getWarnings).
      this.beforeCompletionCalled = true;
      this.frameworkSynchronization.beforeCompletion();
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
   * <p>Needs to invoke the Framework synchronization's {@code beforeCompletion}
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
      this.frameworkSynchronization.beforeCompletion();
    }
    // Call afterCompletion with the appropriate status indication.
    switch (status) {
      case Status.STATUS_COMMITTED -> frameworkSynchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
      case Status.STATUS_ROLLEDBACK -> frameworkSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
      default -> frameworkSynchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    }
  }

}
