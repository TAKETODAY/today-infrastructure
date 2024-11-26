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

package infra.transaction.reactive;

import java.util.function.Function;

import infra.lang.Nullable;
import infra.transaction.CannotCreateTransactionException;
import infra.transaction.ReactiveTransactionManager;
import infra.transaction.TransactionDefinition;
import reactor.core.publisher.Mono;

/**
 * Test implementation of a {@link ReactiveTransactionManager}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
class ReactiveTestTransactionManager extends AbstractReactiveTransactionManager {

  private static final Object TRANSACTION = "transaction";

  private final boolean existingTransaction;

  private final boolean canCreateTransaction;

  @Nullable
  private Function<String, RuntimeException> forceFailOnCommit;

  @Nullable
  private Function<String, RuntimeException> forceFailOnRollback;

  protected boolean begin = false;

  protected boolean commit = false;

  protected boolean rollback = false;

  protected boolean rollbackOnly = false;

  protected boolean cleanup = false;

  ReactiveTestTransactionManager(boolean existingTransaction, boolean canCreateTransaction) {
    this.existingTransaction = existingTransaction;
    this.canCreateTransaction = canCreateTransaction;
  }

  ReactiveTestTransactionManager(boolean existingTransaction, @Nullable Function<String, RuntimeException> forceFailOnCommit, @Nullable Function<String, RuntimeException> forceFailOnRollback) {
    this.existingTransaction = existingTransaction;
    this.canCreateTransaction = true;
    this.forceFailOnCommit = forceFailOnCommit;
    this.forceFailOnRollback = forceFailOnRollback;
  }

  @Override
  protected Object doGetTransaction(TransactionSynchronizationManager synchronizationManager) {
    return TRANSACTION;
  }

  @Override
  protected boolean isExistingTransaction(Object transaction) {
    return this.existingTransaction;
  }

  @Override
  protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager, Object transaction, TransactionDefinition definition) {
    if (!TRANSACTION.equals(transaction)) {
      return Mono.error(new IllegalArgumentException("Not the same transaction object"));
    }
    if (!this.canCreateTransaction) {
      return Mono.error(new CannotCreateTransactionException("Cannot create transaction"));
    }
    return Mono.fromRunnable(() -> this.begin = true);
  }

  @Override
  protected Mono<Void> doCommit(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
    if (!TRANSACTION.equals(status.getTransaction())) {
      return Mono.error(new IllegalArgumentException("Not the same transaction object"));
    }
    return Mono.fromRunnable(() -> {
      this.commit = true;
      if (this.forceFailOnCommit != null) {
        throw this.forceFailOnCommit.apply("Forced failure on commit");
      }
    });
  }

  @Override
  protected Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
    if (!TRANSACTION.equals(status.getTransaction())) {
      return Mono.error(new IllegalArgumentException("Not the same transaction object"));
    }
    return Mono.fromRunnable(() -> {
      this.rollback = true;
      if (this.forceFailOnRollback != null) {
        throw this.forceFailOnRollback.apply("Forced failure on rollback");
      }
    });
  }

  @Override
  protected Mono<Void> doSetRollbackOnly(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
    if (!TRANSACTION.equals(status.getTransaction())) {
      return Mono.error(new IllegalArgumentException("Not the same transaction object"));
    }
    return Mono.fromRunnable(() -> this.rollbackOnly = true);
  }

  @Override
  protected Mono<Void> doCleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager, Object transaction) {
    return Mono.fromRunnable(() -> this.cleanup = true);
  }
}
