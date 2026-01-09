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

package infra.transaction;

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
 * @see infra.transaction.support.TransactionCallback#doInTransaction
 * @see infra.transaction.interceptor.TransactionInterceptor#currentTransactionStatus()
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
