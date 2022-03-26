/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.retry;

import java.util.Stack;

import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.support.AbstractPlatformTransactionManager;
import cn.taketoday.transaction.support.DefaultTransactionStatus;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

@SuppressWarnings("serial")
public class ResourcelessTransactionManager extends AbstractPlatformTransactionManager {

  protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
    ((ResourcelessTransaction) transaction).begin();
  }

  protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
    logger.debug("Committing resourceless transaction on [" + status.getTransaction() + "]");
  }

  protected Object doGetTransaction() throws TransactionException {
    Object transaction = new ResourcelessTransaction();
    Stack<Object> resources;
    if (!TransactionSynchronizationManager.hasResource(this)) {
      resources = new Stack<Object>();
      TransactionSynchronizationManager.bindResource(this, resources);
    }
    else {
      @SuppressWarnings("unchecked")
      Stack<Object> stack = (Stack<Object>) TransactionSynchronizationManager.getResource(this);
      resources = stack;
    }
    resources.push(transaction);
    return transaction;
  }

  protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
    logger.debug("Rolling back resourceless transaction on [" + status.getTransaction() + "]");
  }

  protected boolean isExistingTransaction(Object transaction) throws TransactionException {
    if (TransactionSynchronizationManager.hasResource(this)) {
      @SuppressWarnings("unchecked")
      Stack<Object> stack = (Stack<Object>) TransactionSynchronizationManager.getResource(this);
      return stack.size() > 1;
    }
    return ((ResourcelessTransaction) transaction).isActive();
  }

  protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
  }

  protected void doCleanupAfterCompletion(Object transaction) {
    @SuppressWarnings("unchecked")
    Stack<Object> list = (Stack<Object>) TransactionSynchronizationManager.getResource(this);
    Stack<Object> resources = list;
    resources.clear();
    TransactionSynchronizationManager.unbindResource(this);
    ((ResourcelessTransaction) transaction).clear();
  }

  private static class ResourcelessTransaction {

    private boolean active = false;

    public boolean isActive() {
      return active;
    }

    public void begin() {
      active = true;
    }

    public void clear() {
      active = false;
    }

  }

}
