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

package cn.taketoday.transaction.support;

import java.util.ArrayList;
import java.util.Collection;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionExecution;
import cn.taketoday.transaction.TransactionExecutionListener;

/**
 * Composite TransactionExecutionListener
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/23 15:32
 */
class CompositeTransactionExecutionListener implements TransactionExecutionListener {

  public Collection<TransactionExecutionListener> listeners = new ArrayList<>();

  @Override
  public void beforeBegin(TransactionExecution transaction) {
    for (TransactionExecutionListener listener : listeners) {
      listener.beforeBegin(transaction);
    }
  }

  @Override
  public void afterBegin(TransactionExecution transaction, @Nullable Throwable beginFailure) {
    for (TransactionExecutionListener listener : listeners) {
      listener.afterBegin(transaction, beginFailure);
    }
  }

  @Override
  public void beforeCommit(TransactionExecution transaction) {
    for (TransactionExecutionListener listener : listeners) {
      listener.beforeCommit(transaction);
    }
  }

  @Override
  public void afterCommit(TransactionExecution transaction, @Nullable Throwable commitFailure) {
    for (TransactionExecutionListener listener : listeners) {
      listener.afterCommit(transaction, commitFailure);
    }
  }

  @Override
  public void beforeRollback(TransactionExecution transaction) {
    for (TransactionExecutionListener listener : listeners) {
      listener.beforeRollback(transaction);
    }
  }

  @Override
  public void afterRollback(TransactionExecution transaction, @Nullable Throwable rollbackFailure) {
    for (TransactionExecutionListener listener : listeners) {
      listener.afterRollback(transaction, rollbackFailure);
    }
  }

}
