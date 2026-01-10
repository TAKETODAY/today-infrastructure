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

import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;
import infra.util.ExceptionUtils;

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
    try {
      return action.doInTransaction(new SimpleTransactionStatus(false));
    }
    catch (Throwable e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  @Nullable
  @Override
  public <T> T execute(TransactionCallback<T> action, @Nullable TransactionDefinition config) throws TransactionException {
    return execute(action);
  }

}
