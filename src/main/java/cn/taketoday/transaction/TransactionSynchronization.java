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

import java.io.Flushable;

import cn.taketoday.transaction.SynchronizationManager.SynchronizationMetaData;

/**
 * @author TODAY <br>
 * 2018-10-09 10:27
 */
public interface TransactionSynchronization extends Flushable {

  /** Completion status in case of proper commit */
  int STATUS_COMMITTED = 0;

  /** Completion status in case of proper rollback */
  int STATUS_ROLLED_BACK = 1;

  /** Completion status in case of heuristic mixed completion or system errors */
  int STATUS_UNKNOWN = 2;

  /**
   * Suspend this synchronization. Supposed to unbind resources from
   * SynchronizationManager if managing any.
   *
   * @see SynchronizationManager#unbindResource
   */
  default void suspend(SynchronizationMetaData metaData) { }

  /**
   * Resume this synchronization. Supposed to rebind resources to
   * SynchronizationManager if managing any.
   *
   * @see SynchronizationManager#bindResource
   */
  default void resume(SynchronizationMetaData metaData) { }

  @Override
  default void flush() {
    flush(SynchronizationManager.getMetaData());
  }

  default void flush(SynchronizationMetaData metaData) { }

  /**
   * Invoked before transaction commit (before "beforeCompletion"). Can e.g. flush
   * transactional O/R Mapping sessions to the database.
   * <p>
   * This callback does <i>not</i> mean that the transaction will actually be
   * committed. A rollback decision can still occur after this method has been
   * called. This callback is rather meant to perform work that's only relevant if
   * a commit still has a chance to happen, such as flushing SQL statements to the
   * database.
   * <p>
   * Note that exceptions will get propagated to the commit caller and cause a
   * rollback of the transaction.
   *
   * @param readOnly
   *         whether the transaction is defined as read-only transaction
   *
   * @throws RuntimeException
   *         in case of errors; will be <b>propagated to the caller</b> (note:
   *         do not throw TransactionException subclasses here!)
   * @see #beforeCompletion
   */
  default void beforeCommit(SynchronizationMetaData metaData, boolean readOnly) { }

  /**
   * Invoked before transaction commit/rollback. Can perform resource cleanup
   * <i>before</i> transaction completion.
   * <p>
   * This method will be invoked after {@code beforeCommit}, even when
   * {@code beforeCommit} threw an exception. This callback allows for closing
   * resources before transaction completion, for any outcome.
   *
   * @throws RuntimeException
   *         in case of errors; will be <b>logged but not propagated</b>
   *         (note: do not throw TransactionException subclasses here!)
   * @see #beforeCommit
   * @see #afterCompletion
   */
  default void beforeCompletion(SynchronizationMetaData metaData) { }

  /**
   * Invoked after transaction commit. Can perform further operations right
   * <i>after</i> the main transaction has <i>successfully</i> committed.
   * <p>
   * Can e.g. commit further operations that are supposed to follow on a
   * successful commit of the main transaction, like confirmation messages or
   * emails.
   * <p>
   * <b>NOTE:</b> The transaction will have been committed already, but the
   * transactional resources might still be active and accessible. As a
   * consequence, any data access code triggered at this point will still
   * "participate" in the original transaction, allowing to perform some cleanup
   * (with no commit following anymore!), unless it explicitly declares that it
   * needs to run in a separate transaction. Hence: <b>Use
   * {@code PROPAGATION_REQUIRES_NEW} for any transactional operation that is
   * called from here.</b>
   *
   * @throws RuntimeException
   *         in case of errors; will be <b>propagated to the caller</b> (note:
   *         do not throw TransactionException subclasses here!)
   */
  default void afterCommit(SynchronizationMetaData metaData) { }

  default void afterCompletion(SynchronizationMetaData metaData, int status) { }

}
