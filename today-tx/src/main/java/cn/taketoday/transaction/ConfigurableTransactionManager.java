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

import java.util.Collection;

/**
 * Common configuration interface for transaction manager implementations.
 * Provides registration facilities for {@link TransactionExecutionListener}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PlatformTransactionManager
 * @see ReactiveTransactionManager
 * @since 4.0
 */
public interface ConfigurableTransactionManager extends TransactionManager {

  /**
   * Set the transaction execution listeners for begin/commit/rollback callbacks
   * from this transaction manager.
   *
   * @see #addListener
   */
  void setTransactionExecutionListeners(Collection<TransactionExecutionListener> listeners);

  /**
   * Return the registered transaction execution listeners for this transaction manager.
   *
   * @see #setTransactionExecutionListeners
   */
  Collection<TransactionExecutionListener> getTransactionExecutionListeners();

  /**
   * Conveniently register the given listener for begin/commit/rollback callbacks
   * from this transaction manager.
   *
   * @see #getTransactionExecutionListeners()
   */
  default void addListener(TransactionExecutionListener listener) {
    getTransactionExecutionListeners().add(listener);
  }

}
