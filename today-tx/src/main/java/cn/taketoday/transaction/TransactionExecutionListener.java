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

package cn.taketoday.transaction;

import cn.taketoday.lang.Nullable;

/**
 * Callback interface for stateless listening to transaction creation/completion steps
 * in a transaction manager. This is primarily meant for observation and statistics;
 * consider stateful transaction synchronizations for resource management purposes.
 *
 * <p>In contrast to synchronizations, the transaction execution listener contract is
 * commonly supported for thread-bound transactions as well as reactive transactions.
 * The callback-provided {@link TransactionExecution} object will be either a
 * {@link TransactionStatus} (for a {@link PlatformTransactionManager} transaction) or
 * a {@link ReactiveTransaction} (for a {@link ReactiveTransactionManager} transaction).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurableTransactionManager#addListener
 * @see cn.taketoday.transaction.support.TransactionSynchronizationManager#registerSynchronization
 * @see cn.taketoday.transaction.reactive.TransactionSynchronizationManager#registerSynchronization
 * @since 4.0
 */
public interface TransactionExecutionListener {

  /**
   * Callback before the transaction begin step.
   *
   * @param transaction the current transaction
   */
  default void beforeBegin(TransactionExecution transaction) {
  }

  /**
   * Callback after the transaction begin step.
   *
   * @param transaction the current transaction
   * @param beginFailure an exception occurring during begin
   * (or {@code null} after a successful begin step)
   */
  default void afterBegin(TransactionExecution transaction, @Nullable Throwable beginFailure) {
  }

  /**
   * Callback before the transaction commit step.
   *
   * @param transaction the current transaction
   */
  default void beforeCommit(TransactionExecution transaction) {
  }

  /**
   * Callback after the transaction commit step.
   *
   * @param transaction the current transaction
   * @param commitFailure an exception occurring during commit
   * (or {@code null} after a successful commit step)
   */
  default void afterCommit(TransactionExecution transaction, @Nullable Throwable commitFailure) {
  }

  /**
   * Callback before the transaction rollback step.
   *
   * @param transaction the current transaction
   */
  default void beforeRollback(TransactionExecution transaction) {
  }

  /**
   * Callback after the transaction rollback step.
   *
   * @param transaction the current transaction
   * @param rollbackFailure an exception occurring during rollback
   * (or {@code null} after a successful rollback step)
   */
  default void afterRollback(TransactionExecution transaction, @Nullable Throwable rollbackFailure) {
  }

}
