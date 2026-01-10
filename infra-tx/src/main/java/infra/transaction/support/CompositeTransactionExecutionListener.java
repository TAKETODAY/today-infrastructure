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

package infra.transaction.support;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

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
