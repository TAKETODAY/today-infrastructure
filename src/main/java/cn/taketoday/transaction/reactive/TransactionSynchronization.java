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

import reactor.core.publisher.Mono;

/**
 * Interface for reactive transaction synchronization callbacks.
 * Supported by {@link AbstractReactiveTransactionManager}.
 *
 * <p>TransactionSynchronization implementations can implement the
 * {@link cn.taketoday.core.Ordered} interface to influence their execution order.
 * A synchronization that does not implement the {@link cn.taketoday.core.Ordered}
 * interface is appended to the end of the synchronization chain.
 *
 * <p>System synchronizations performed by Framework itself use specific order values,
 * allowing for fine-grained interaction with their execution order (if necessary).
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @see TransactionSynchronizationManager
 * @see AbstractReactiveTransactionManager
 * @since 4.0
 */
public interface TransactionSynchronization {

  /** Completion status in case of proper commit. */
  int STATUS_COMMITTED = 0;

  /** Completion status in case of proper rollback. */
  int STATUS_ROLLED_BACK = 1;

  /** Completion status in case of heuristic mixed completion or system errors. */
  int STATUS_UNKNOWN = 2;

  /**
   * Suspend this synchronization.
   * Supposed to unbind resources from TransactionSynchronizationManager if managing any.
   *
   * @see TransactionSynchronizationManager#unbindResource
   */
  default Mono<Void> suspend() {
    return Mono.empty();
  }

  /**
   * Resume this synchronization.
   * Supposed to rebind resources to TransactionSynchronizationManager if managing any.
   *
   * @see TransactionSynchronizationManager#bindResource
   */
  default Mono<Void> resume() {
    return Mono.empty();
  }

  /**
   * Invoked before transaction commit (before "beforeCompletion").
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
  default Mono<Void> beforeCommit(boolean readOnly) {
    return Mono.empty();
  }

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
  default Mono<Void> beforeCompletion() {
    return Mono.empty();
  }

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
  default Mono<Void> afterCommit() {
    return Mono.empty();
  }

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
  default Mono<Void> afterCompletion(int status) {
    return Mono.empty();
  }

}
