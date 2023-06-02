/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.transaction.reactive;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.ReactiveTransaction;
import cn.taketoday.transaction.ReactiveTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionSystemException;
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
    Assert.notNull(transactionManager, "ReactiveTransactionManager must not be null");
    Assert.notNull(transactionDefinition, "TransactionDefinition must not be null");
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
    if (ex instanceof RuntimeException &&
            ex.getCause() != null &&
            ex.getMessage().startsWith("Async resource cleanup failed")) {
      return ex.getCause();
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
