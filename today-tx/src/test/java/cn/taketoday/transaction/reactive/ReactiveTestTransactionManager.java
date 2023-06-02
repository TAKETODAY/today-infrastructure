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

import cn.taketoday.transaction.CannotCreateTransactionException;
import cn.taketoday.transaction.ReactiveTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
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

  private final boolean forceFailOnCommit;

  protected boolean begin = false;

  protected boolean commit = false;

  protected boolean rollback = false;

  protected boolean rollbackOnly = false;

  protected boolean cleanup = false;

  ReactiveTestTransactionManager(boolean existingTransaction, boolean canCreateTransaction) {
    this(existingTransaction, canCreateTransaction, false);
  }

  ReactiveTestTransactionManager(boolean existingTransaction, boolean canCreateTransaction, boolean forceFailOnCommit) {
    this.existingTransaction = existingTransaction;
    this.canCreateTransaction = canCreateTransaction;
    this.forceFailOnCommit = forceFailOnCommit;
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
      if (this.forceFailOnCommit) {
        throw new IllegalArgumentException("Forced failure on commit");
      }
    });
  }

  @Override
  protected Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager, GenericReactiveTransaction status) {
    if (!TRANSACTION.equals(status.getTransaction())) {
      return Mono.error(new IllegalArgumentException("Not the same transaction object"));
    }
    return Mono.fromRunnable(() -> this.rollback = true);
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
