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

package infra.transaction.reactive;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.transaction.ReactiveTransaction;
import infra.transaction.ReactiveTransactionManager;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;
import infra.transaction.TransactionSystemException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Operator class that simplifies programmatic transaction demarcation and
 * transaction exception handling.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #execute
 * @see ReactiveTransactionManager
 * @since 4.0
 */
final class TransactionalOperatorImpl implements TransactionalOperator {
  private static final Logger log = LoggerFactory.getLogger(TransactionalOperatorImpl.class);

  private final ReactiveTransactionManager transactionManager;

  private final TransactionDefinition transactionDefinition;

  /**
   * Construct a new TransactionTemplate using the given transaction manager,
   * taking its default settings from the given transaction definition.
   *
   * @param transactionManager the transaction management strategy to be used
   * @param transactionDefinition the transaction definition to copy the
   * default settings from. Local properties can still be set to change values.
   */
  TransactionalOperatorImpl(ReactiveTransactionManager transactionManager,
          TransactionDefinition transactionDefinition) {
    Assert.notNull(transactionManager, "ReactiveTransactionManager is required");
    Assert.notNull(transactionDefinition, "TransactionDefinition is required");
    this.transactionManager = transactionManager;
    this.transactionDefinition = transactionDefinition;
  }

  /**
   * Return the transaction management strategy to be used.
   */
  public ReactiveTransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  @Override
  public <T> Flux<T> execute(TransactionCallback<T> action) throws TransactionException {
    return TransactionContextManager.currentContext().flatMapMany(context ->
                    Flux.usingWhen(this.transactionManager.getReactiveTransaction(this.transactionDefinition),
                                    action::doInTransaction,
                                    this.transactionManager::commit,
                                    this::rollbackOnException,
                                    this.transactionManager::rollback)
                            .onErrorMap(this::unwrapIfResourceCleanupFailure))
            .contextWrite(TransactionContextManager.getOrCreateContext())
            .contextWrite(TransactionContextManager.getOrCreateContextHolder());
  }

  /**
   * Perform a rollback, handling rollback exceptions properly.
   *
   * @param status object representing the transaction
   * @param ex the thrown application exception or error
   * @throws TransactionException in case of a rollback error
   */
  private Mono<Void> rollbackOnException(ReactiveTransaction status, Throwable ex) throws TransactionException {
    log.debug("Initiating transaction rollback on application exception", ex);
    return transactionManager.rollback(status).onErrorMap(ex2 -> {
              log.error("Application exception overridden by rollback exception", ex);
              if (ex2 instanceof TransactionSystemException tse) {
                tse.initApplicationException(ex);
              }
              else {
                ex2.addSuppressed(ex);
              }
              return ex2;
            }
    );
  }

  /**
   * Unwrap the cause of a throwable, if produced by a failure
   * during the async resource cleanup in {@link Flux#usingWhen}.
   *
   * @param ex the throwable to try to unwrap
   */
  private Throwable unwrapIfResourceCleanupFailure(Throwable ex) {
    if (ex instanceof RuntimeException && ex.getCause() != null) {
      String msg = ex.getMessage();
      if (msg != null && msg.startsWith("Async resource cleanup failed")) {
        return ex.getCause();
      }
    }
    return ex;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return this == other
            || (super.equals(other) && (!(other instanceof TransactionalOperatorImpl toi)
            || getTransactionManager() == toi.getTransactionManager()));
  }

  @Override
  public int hashCode() {
    return getTransactionManager().hashCode();
  }

}
