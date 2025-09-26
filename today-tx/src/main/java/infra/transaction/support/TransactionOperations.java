/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;

/**
 * Interface specifying basic transaction execution operations.
 * Implemented by {@link TransactionTemplate}. Not often used directly,
 * but a useful option to enhance testability, as it can easily be
 * mocked or stubbed.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface TransactionOperations {

  /**
   * Execute the action specified by the given callback object within a transaction.
   * <p>Allows for returning a result object created within the transaction, that is,
   * a domain object or a collection of domain objects. A RuntimeException thrown
   * by the callback is treated as a fatal exception that enforces a rollback.
   * Such an exception gets propagated to the caller of the template.
   *
   * @param action the callback object that specifies the transactional action
   * @return a result object returned by the callback, or {@code null} if none
   * @throws TransactionException in case of initialization, rollback, or system errors
   * @throws RuntimeException if thrown by the TransactionCallback
   * @see #executeWithoutResult(TransactionCallbackWithoutResult)
   */
  @Nullable
  <T> T execute(TransactionCallback<T> action) throws TransactionException;

  /**
   * Execute the action specified by the given callback object within a transaction.
   * <p>Allows for returning a result object created within the transaction, that is,
   * a domain object or a collection of domain objects. A RuntimeException thrown
   * by the callback is treated as a fatal exception that enforces a rollback.
   * Such an exception gets propagated to the caller of the template.
   *
   * @param action the callback object that specifies the transactional action
   * @return a result object returned by the callback, or {@code null} if none
   * @throws TransactionException in case of initialization, rollback, or system errors
   * @throws RuntimeException if thrown by the TransactionCallback
   * @see #executeWithoutResult(TransactionCallbackWithoutResult)
   * @since 4.0
   */
  @Nullable
  <T> T execute(TransactionCallback<T> action, @Nullable TransactionDefinition config)
          throws TransactionException;

  /**
   * Execute the action specified by the given {@link Runnable} within a transaction.
   * <p>If you need to return an object from the callback or access the
   * {@link infra.transaction.TransactionStatus} from within the callback,
   * use {@link #execute(TransactionCallback)} instead.
   * <p>This variant is analogous to using a {@link TransactionCallbackWithoutResult}
   * but with a simplified signature for common cases - and conveniently usable with
   * Java 8 lambda expressions.
   *
   * @param action the Runnable that specifies the transactional action
   * @throws TransactionException in case of initialization, rollback, or system errors
   * @throws RuntimeException if thrown by the Runnable
   * @see #execute(TransactionCallback)
   * @see TransactionCallbackWithoutResult
   * @since 4.0
   */
  default void executeWithoutResult(TransactionCallbackWithoutResult action) throws TransactionException {
    execute(action);
  }

  /**
   * Execute the action specified by the given {@link Runnable} within a transaction.
   * <p>If you need to return an object from the callback or access the
   * {@link infra.transaction.TransactionStatus} from within the callback,
   * use {@link #execute(TransactionCallback)} instead.
   * <p>This variant is analogous to using a {@link TransactionCallbackWithoutResult}
   * but with a simplified signature for common cases - and conveniently usable with
   * Java 8 lambda expressions.
   *
   * @param action the Runnable that specifies the transactional action
   * @throws TransactionException in case of initialization, rollback, or system errors
   * @throws RuntimeException if thrown by the Runnable
   * @see #execute(TransactionCallback)
   * @see TransactionCallbackWithoutResult
   * @since 4.0
   */
  default void executeWithoutResult(TransactionCallbackWithoutResult action, @Nullable TransactionDefinition config)
          throws TransactionException {
    execute(action, config);
  }

  /**
   * Return an implementation of the {@code TransactionOperations} interface which
   * executes a given {@link TransactionCallback} without an actual transaction.
   * <p>Useful for testing: The behavior is equivalent to running with a
   * transaction manager with no actual transaction (PROPAGATION_SUPPORTS)
   * and no synchronization (SYNCHRONIZATION_NEVER).
   * <p>For a {@link TransactionOperations} implementation with actual
   * transaction processing, use {@link TransactionTemplate} with an appropriate
   * {@link PlatformTransactionManager}.
   *
   * @see infra.transaction.TransactionDefinition#PROPAGATION_SUPPORTS
   * @see AbstractPlatformTransactionManager#SYNCHRONIZATION_NEVER
   * @see TransactionTemplate
   * @since 4.0
   */
  static TransactionOperations withoutTransaction() {
    return WithoutTransactionOperations.INSTANCE;
  }

}
