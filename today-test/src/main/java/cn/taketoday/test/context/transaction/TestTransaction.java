/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.context.transaction;

import cn.taketoday.lang.Assert;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.transaction.TransactionStatus;

/**
 * {@code TestTransaction} provides a collection of static utility methods for
 * programmatic interaction with <em>test-managed transactions</em> within
 * <em>test</em> methods, <em>before</em> methods, and <em>after</em> methods.
 *
 * <p>Consult the javadocs for {@link TransactionalTestExecutionListener}
 * for a detailed explanation of <em>test-managed transactions</em>.
 *
 * <p>Support for {@code TestTransaction} is automatically available whenever
 * the {@code TransactionalTestExecutionListener} is enabled. Note that the
 * {@code TransactionalTestExecutionListener} is typically enabled by default,
 * but it can also be manually enabled via the
 * {@link TestExecutionListeners @TestExecutionListeners} annotation.
 *
 * @author Sam Brannen
 * @see TransactionalTestExecutionListener
 * @since 4.0
 */
public final class TestTransaction {

  private TestTransaction() {
  }

  /**
   * Determine whether a test-managed transaction is currently <em>active</em>.
   *
   * @return {@code true} if a test-managed transaction is currently active
   * @see #start()
   * @see #end()
   */
  public static boolean isActive() {
    TransactionContext transactionContext = TransactionContextHolder.getCurrentTransactionContext();
    if (transactionContext != null) {
      TransactionStatus transactionStatus = transactionContext.getTransactionStatus();
      return (transactionStatus != null && !transactionStatus.isCompleted());
    }
    return false;
  }

  /**
   * Determine whether the current test-managed transaction has been
   * {@linkplain #flagForRollback() flagged for rollback} or
   * {@linkplain #flagForCommit() flagged for commit}.
   *
   * @return {@code true} if the current test-managed transaction is flagged
   * to be rolled back; {@code false} if the current test-managed transaction
   * is flagged to be committed
   * @throws IllegalStateException if a transaction is not active for the
   * current test
   * @see #isActive()
   * @see #flagForRollback()
   * @see #flagForCommit()
   */
  public static boolean isFlaggedForRollback() {
    return requireCurrentTransactionContext().isFlaggedForRollback();
  }

  /**
   * Flag the current test-managed transaction for <em>rollback</em>.
   * <p>Invoking this method will <em>not</em> end the current transaction.
   * Rather, the value of this flag will be used to determine whether or not
   * the current test-managed transaction should be rolled back or committed
   * once it is {@linkplain #end ended}.
   *
   * @throws IllegalStateException if no transaction is active for the current test
   * @see #isActive()
   * @see #isFlaggedForRollback()
   * @see #start()
   * @see #end()
   */
  public static void flagForRollback() {
    setFlaggedForRollback(true);
  }

  /**
   * Flag the current test-managed transaction for <em>commit</em>.
   * <p>Invoking this method will <em>not</em> end the current transaction.
   * Rather, the value of this flag will be used to determine whether or not
   * the current test-managed transaction should be rolled back or committed
   * once it is {@linkplain #end ended}.
   *
   * @throws IllegalStateException if no transaction is active for the current test
   * @see #isActive()
   * @see #isFlaggedForRollback()
   * @see #start()
   * @see #end()
   */
  public static void flagForCommit() {
    setFlaggedForRollback(false);
  }

  /**
   * Start a new test-managed transaction.
   * <p>Only call this method if {@link #end} has been called or if no
   * transaction has been previously started.
   *
   * @throws IllegalStateException if the transaction context could not be
   * retrieved or if a transaction is already active for the current test
   * @see #isActive()
   * @see #end()
   */
  public static void start() {
    requireCurrentTransactionContext().startTransaction();
  }

  /**
   * Immediately force a <em>commit</em> or <em>rollback</em> of the
   * current test-managed transaction, according to the
   * {@linkplain #isFlaggedForRollback rollback flag}.
   *
   * @throws IllegalStateException if the transaction context could not be
   * retrieved or if a transaction is not active for the current test
   * @see #isActive()
   * @see #start()
   */
  public static void end() {
    requireCurrentTransactionContext().endTransaction();
  }

  private static TransactionContext requireCurrentTransactionContext() {
    TransactionContext txContext = TransactionContextHolder.getCurrentTransactionContext();
    Assert.state(txContext != null, "TransactionContext is not active");
    return txContext;
  }

  private static void setFlaggedForRollback(boolean flag) {
    requireCurrentTransactionContext().setFlaggedForRollback(flag);
  }

}
