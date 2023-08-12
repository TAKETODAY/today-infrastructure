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

import java.io.Flushable;

/**
 * Representation of an ongoing {@link PlatformTransactionManager} transaction.
 * Extends the common {@link TransactionExecution} interface.
 *
 * <p>Transactional code can use this to retrieve status information,
 * and to programmatically request a rollback (instead of throwing
 * an exception that causes an implicit rollback).
 *
 * <p>Includes the {@link SavepointManager} interface to provide access
 * to savepoint management facilities. Note that savepoint management
 * is only available if supported by the underlying transaction manager.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setRollbackOnly()
 * @see PlatformTransactionManager#getTransaction
 * @see cn.taketoday.transaction.support.TransactionCallback#doInTransaction
 * @see cn.taketoday.transaction.interceptor.TransactionInterceptor#currentTransactionStatus()
 * @since 2018-11-16 21:25
 */
public interface TransactionStatus extends TransactionExecution, SavepointManager, Flushable {

  /**
   * Return whether this transaction internally carries a savepoint,
   * that is, has been created as nested transaction based on a savepoint.
   * <p>This method is mainly here for diagnostic purposes, alongside
   * {@link #isNewTransaction()}. For programmatic handling of custom
   * savepoints, use the operations provided by {@link SavepointManager}.
   * <p>The default implementation returns {@code false}.
   *
   * @see #isNewTransaction()
   * @see #createSavepoint()
   * @see #rollbackToSavepoint(Object)
   * @see #releaseSavepoint(Object)
   */
  default boolean hasSavepoint() {
    return false;
  }

  /**
   * Flush the underlying session to the datastore, if applicable:
   * for example, all affected Hibernate/JPA sessions.
   * <p>This is effectively just a hint and may be a no-op if the underlying
   * transaction manager does not have a flush concept. A flush signal may
   * get applied to the primary resource or to transaction synchronizations,
   * depending on the underlying resource.
   * <p>The default implementation is empty, considering flush as a no-op.
   */
  @Override
  default void flush() {

  }

}
