/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.transaction;

import cn.taketoday.lang.NonNull;
import cn.taketoday.transaction.support.AbstractPlatformTransactionManager;
import cn.taketoday.transaction.support.DefaultTransactionStatus;

/**
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
class TestTransactionManager extends AbstractPlatformTransactionManager {

  private static final Object TRANSACTION = "transaction";

  private final boolean existingTransaction;

  private final boolean canCreateTransaction;

  protected boolean begin = false;

  protected boolean commit = false;

  protected boolean rollback = false;

  protected boolean rollbackOnly = false;

  protected TestTransactionManager(boolean existingTransaction, boolean canCreateTransaction) {
    this.existingTransaction = existingTransaction;
    this.canCreateTransaction = canCreateTransaction;
    setTransactionSynchronization(SYNCHRONIZATION_NEVER);
  }

  @Override
  protected Object doGetTransaction() {
    return TRANSACTION;
  }

  @Override
  protected boolean isExistingTransaction(Object transaction) {
    return existingTransaction;
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) {
    if (!TRANSACTION.equals(transaction)) {
      throw new IllegalArgumentException("Not the same transaction object");
    }
    if (!this.canCreateTransaction) {
      throw new CannotCreateTransactionException("Cannot create transaction");
    }
    this.begin = true;
  }

  @Override
  protected void doCommit(@NonNull DefaultTransactionStatus status) {
    if (!TRANSACTION.equals(status.getTransaction())) {
      throw new IllegalArgumentException("Not the same transaction object");
    }
    this.commit = true;
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) {
    if (!TRANSACTION.equals(status.getTransaction())) {
      throw new IllegalArgumentException("Not the same transaction object");
    }
    this.rollback = true;
  }

  @Override
  protected void doSetRollbackOnly(@NonNull DefaultTransactionStatus status) {
    if (!TRANSACTION.equals(status.getTransaction())) {
      throw new IllegalArgumentException("Not the same transaction object");
    }
    this.rollbackOnly = true;
  }

}
