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

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;

/**
 * A {@link TransactionOperations} implementation which executes a given
 * {@link TransactionCallback} without an actual transaction.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionOperations#withoutTransaction()
 * @since 4.0
 */
final class WithoutTransactionOperations implements TransactionOperations {

  static final WithoutTransactionOperations INSTANCE = new WithoutTransactionOperations();

  @Override
  @Nullable
  public <T> T execute(TransactionCallback<T> action) throws TransactionException {
    return action.doInTransaction(new SimpleTransactionStatus(false));
  }

  @Nullable
  @Override
  public <T> T execute(TransactionCallback<T> action, @Nullable TransactionDefinition config) throws TransactionException {
    return execute(action);
  }

}
