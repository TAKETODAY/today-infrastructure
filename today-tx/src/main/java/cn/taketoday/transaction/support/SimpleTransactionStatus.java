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

package cn.taketoday.transaction.support;

import cn.taketoday.transaction.PlatformTransactionManager;

/**
 * A simple {@link cn.taketoday.transaction.TransactionStatus}
 * implementation. Derives from {@link AbstractTransactionStatus} and
 * adds an explicit {@link #isNewTransaction() "newTransaction"} flag.
 *
 * <p>This class is not used by any of Framework's pre-built
 * {@link PlatformTransactionManager}
 * implementations. It is mainly provided as a start for custom transaction
 * manager implementations and as a static mock for testing transactional
 * code (either as part of a mock {@code PlatformTransactionManager} or
 * as argument passed into a {@link TransactionCallback} to be tested).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionCallback#doInTransaction
 * @since 4.0
 */
public class SimpleTransactionStatus extends AbstractTransactionStatus {

  private final boolean newTransaction;

  /**
   * Create a new {@code SimpleTransactionStatus} instance,
   * indicating a new transaction.
   */
  public SimpleTransactionStatus() {
    this(true);
  }

  /**
   * Create a new {@code SimpleTransactionStatus} instance.
   *
   * @param newTransaction whether to indicate a new transaction
   */
  public SimpleTransactionStatus(boolean newTransaction) {
    this.newTransaction = newTransaction;
  }

  @Override
  public boolean isNewTransaction() {
    return this.newTransaction;
  }

}
