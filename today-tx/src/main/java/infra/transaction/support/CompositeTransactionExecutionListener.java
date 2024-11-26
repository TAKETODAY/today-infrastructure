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

package infra.transaction.support;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import infra.lang.Nullable;
import infra.transaction.TransactionExecution;
import infra.transaction.TransactionExecutionListener;

/**
 * Composite TransactionExecutionListener
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/23 15:32
 */
class CompositeTransactionExecutionListener implements TransactionExecutionListener, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public Collection<TransactionExecutionListener> listeners;

  public CompositeTransactionExecutionListener() {
    this(new ArrayList<>());
  }

  public CompositeTransactionExecutionListener(Collection<TransactionExecutionListener> listeners) {
    this.listeners = listeners;
  }

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
