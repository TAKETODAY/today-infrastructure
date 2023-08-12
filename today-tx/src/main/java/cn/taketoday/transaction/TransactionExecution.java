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

/**
 * Common representation of the current state of a transaction.
 * Serves as base interface for {@link TransactionStatus} as well as
 * {@link ReactiveTransaction}. and also as transaction
 * representation for {@link TransactionExecutionListener}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/10 21:36
 */
public interface TransactionExecution {

  /**
   * Return the defined name of the transaction (possibly an empty String).
   * <p>In case of Infra declarative transactions, the exposed name will be
   * the {@code fully-qualified class name + "." + method name} (by default).
   * <p>The default implementation returns an empty String.
   *
   * @see TransactionDefinition#getName()
   */
  default String getTransactionName() {
    return "";
  }

  /**
   * Return whether there is an actual transaction active: this is meant to cover
   * a new transaction as well as participation in an existing transaction, only
   * returning {@code false} when not running in an actual transaction at all.
   * <p>The default implementation returns {@code true}.
   *
   * @see #isNewTransaction()
   * @see #isNested()
   * @see #isReadOnly()
   */
  default boolean hasTransaction() {
    return true;
  }

  /**
   * Return whether the transaction manager considers the present transaction
   * as new; otherwise participating in an existing transaction, or potentially
   * not running in an actual transaction in the first place.
   * <p>This is primarily here for transaction manager state handling.
   * Prefer the use of {@link #hasTransaction()} for application purposes
   * since this is usually semantically appropriate.
   * <p>The "new" status can be transaction manager specific, e.g. returning
   * {@code true} for an actual nested transaction but potentially {@code false}
   * for a savepoint-based nested transaction scope if the savepoint management
   * is explicitly exposed (such as on {@link TransactionStatus}). A combined
   * check for any kind of nested execution is provided by {@link #isNested()}.
   * <p>The default implementation returns {@code true}.
   *
   * @see #hasTransaction()
   * @see #isNested()
   * @see TransactionStatus#hasSavepoint()
   */
  default boolean isNewTransaction() {
    return true;
  }

  /**
   * Return if this transaction executes in a nested fashion within another.
   * <p>The default implementation returns {@code false}.
   *
   * @see #hasTransaction()
   * @see #isNewTransaction()
   * @see TransactionDefinition#PROPAGATION_NESTED
   */
  default boolean isNested() {
    return false;
  }

  /**
   * Return if this transaction is defined as read-only transaction.
   * <p>The default implementation returns {@code false}.
   *
   * @see TransactionDefinition#isReadOnly()
   */
  default boolean isReadOnly() {
    return false;
  }

  /**
   * Set the transaction rollback-only. This instructs the transaction manager
   * that the only possible outcome of the transaction may be a rollback, as
   * alternative to throwing an exception which would in turn trigger a rollback.
   * <p>The default implementation throws an UnsupportedOperationException.
   *
   * @see #isRollbackOnly()
   */
  default void setRollbackOnly() {
    throw new UnsupportedOperationException("setRollbackOnly not supported");
  }

  /**
   * Return whether the transaction has been marked as rollback-only
   * (either by the application or by the transaction infrastructure).
   * <p>The default implementation returns {@code false}.
   *
   * @see #setRollbackOnly()
   */
  default boolean isRollbackOnly() {
    return false;
  }

  /**
   * Return whether this transaction is completed, that is,
   * whether it has already been committed or rolled back.
   * <p>The default implementation returns {@code false}.
   */
  default boolean isCompleted() {
    return false;
  }

}
