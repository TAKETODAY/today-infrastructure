/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.transaction.support;

import java.io.Flushable;

import infra.core.Ordered;

/**
 * Interface for transaction synchronization callbacks.
 * Supported by AbstractPlatformTransactionManager.
 *
 * <p>TransactionSynchronization implementations can implement the Ordered interface
 * to influence their execution order. A synchronization that does not implement the
 * Ordered interface is appended to the end of the synchronization chain.
 *
 * <p>System synchronizations performed by Framework itself use specific order values,
 * allowing for fine-grained interaction with their execution order (if necessary).
 *
 * <p>Implements the {@link Ordered} interface to enable the execution order of
 * synchronizations to be controlled declaratively. The default
 * {@link #getOrder() order} is {@link Ordered#LOWEST_PRECEDENCE}, indicating
 * late execution; return a lower value for earlier execution.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionSynchronizationManager
 * @see AbstractPlatformTransactionManager
 * @see infra.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
 * @since 4.0
 */
public interface TransactionSynchronization extends Ordered, Flushable {

  /** Completion status in case of proper commit. */
  int STATUS_COMMITTED = 0;

  /** Completion status in case of proper rollback. */
  int STATUS_ROLLED_BACK = 1;

  /** Completion status in case of heuristic mixed completion or system errors. */
  int STATUS_UNKNOWN = 2;

  /**
   * Return the execution order for this transaction synchronization.
   * <p>Default is {@link Ordered#LOWEST_PRECEDENCE}.
   */
  @Override
  default int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  /**
   * Suspend this synchronization.
   * Supposed to unbind resources from TransactionSynchronizationManager if managing any.
   *
   * @see TransactionSynchronizationManager#unbindResource
   */
  default void suspend() { }

  /**
   * Resume this synchronization.
   * Supposed to rebind resources to TransactionSynchronizationManager if managing any.
   *
   * @see TransactionSynchronizationManager#bindResource
   */
  default void resume() { }

  /**
   * Flush the underlying session to the datastore, if applicable:
   * for example, a Hibernate/JPA session.
   *
   * @see infra.transaction.TransactionStatus#flush()
   */
  @Override
  default void flush() { }

  /**
   * Invoked before transaction commit (before "beforeCompletion").
   * Can e.g. flush transactional O/R Mapping sessions to the database.
   * <p>This callback does <i>not</i> mean that the transaction will actually be committed.
   * A rollback decision can still occur after this method has been called. This callback
   * is rather meant to perform work that's only relevant if a commit still has a chance
   * to happen, such as flushing SQL statements to the database.
   * <p>Note that exceptions will get propagated to the commit caller and cause a
   * rollback of the transaction.
   *
   * @param readOnly whether the transaction is defined as read-only transaction
   * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
   * (note: do not throw TransactionException subclasses here!)
   * @see #beforeCompletion
   */
  default void beforeCommit(boolean readOnly) { }

  /**
   * Invoked before transaction commit/rollback.
   * Can perform resource cleanup <i>before</i> transaction completion.
   * <p>This method will be invoked after {@code beforeCommit}, even when
   * {@code beforeCommit} threw an exception. This callback allows for
   * closing resources before transaction completion, for any outcome.
   *
   * @throws RuntimeException in case of errors; will be <b>logged but not propagated</b>
   * (note: do not throw TransactionException subclasses here!)
   * @see #beforeCommit
   * @see #afterCompletion
   */
  default void beforeCompletion() { }

  /**
   * Invoked after transaction commit. Can perform further operations right
   * <i>after</i> the main transaction has <i>successfully</i> committed.
   * <p>Can e.g. commit further operations that are supposed to follow on a successful
   * commit of the main transaction, like confirmation messages or emails.
   * <p><b>NOTE:</b> The transaction will have been committed already, but the
   * transactional resources might still be active and accessible. As a consequence,
   * any data access code triggered at this point will still "participate" in the
   * original transaction, allowing to perform some cleanup (with no commit following
   * anymore!), unless it explicitly declares that it needs to run in a separate
   * transaction. Hence: <b>Use {@code PROPAGATION_REQUIRES_NEW} for any
   * transactional operation that is called from here.</b>
   *
   * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
   * (note: do not throw TransactionException subclasses here!)
   */
  default void afterCommit() { }

  /**
   * Invoked after transaction commit/rollback.
   * Can perform resource cleanup <i>after</i> transaction completion.
   * <p><b>NOTE:</b> The transaction will have been committed or rolled back already,
   * but the transactional resources might still be active and accessible. As a
   * consequence, any data access code triggered at this point will still "participate"
   * in the original transaction, allowing to perform some cleanup (with no commit
   * following anymore!), unless it explicitly declares that it needs to run in a
   * separate transaction. Hence: <b>Use {@code PROPAGATION_REQUIRES_NEW}
   * for any transactional operation that is called from here.</b>
   *
   * @param status completion status according to the {@code STATUS_*} constants
   * @throws RuntimeException in case of errors; will be <b>logged but not propagated</b>
   * (note: do not throw TransactionException subclasses here!)
   * @see #STATUS_COMMITTED
   * @see #STATUS_ROLLED_BACK
   * @see #STATUS_UNKNOWN
   * @see #beforeCompletion
   */
  default void afterCompletion(int status) { }

  /**
   * Invoked on creation of a new savepoint, either when a nested transaction
   * is started against an existing transaction or on a programmatic savepoint
   * via {@link infra.transaction.TransactionStatus}.
   * <p>This synchronization callback is invoked right <i>after</i> the creation
   * of the resource savepoint, with the given savepoint object already active.
   *
   * @param savepoint the associated savepoint object (primarily as a key for
   * identifying the savepoint but also castable to the resource savepoint type)
   * @see infra.transaction.SavepointManager#createSavepoint
   * @see infra.transaction.TransactionDefinition#PROPAGATION_NESTED
   */
  default void savepoint(Object savepoint) {
  }

  /**
   * Invoked in case of a rollback to the previously created savepoint.
   * <p>This synchronization callback is invoked right <i>before</i> the rollback
   * of the resource savepoint, with the given savepoint object still active.
   *
   * @param savepoint the associated savepoint object (primarily as a key for
   * identifying the savepoint but also castable to the resource savepoint type)
   * @see #savepoint
   * @see infra.transaction.SavepointManager#rollbackToSavepoint
   */
  default void savepointRollback(Object savepoint) {
  }

}
