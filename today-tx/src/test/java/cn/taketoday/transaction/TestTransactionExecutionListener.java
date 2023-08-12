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

package cn.taketoday.transaction;

import cn.taketoday.lang.Nullable;

/**
 * @author Juergen Hoeller
 */
public class TestTransactionExecutionListener implements TransactionExecutionListener {

  public boolean beforeBeginCalled;

  public boolean afterBeginCalled;

  @Nullable
  public Throwable beginFailure;

  public boolean beforeCommitCalled;

  public boolean afterCommitCalled;

  @Nullable
  public Throwable commitFailure;

  public boolean beforeRollbackCalled;

  public boolean afterRollbackCalled;

  @Nullable
  public Throwable rollbackFailure;

  @Override
  public void beforeBegin(TransactionExecution transactionState) {
    this.beforeBeginCalled = true;
  }

  @Override
  public void afterBegin(TransactionExecution transactionState, @Nullable Throwable beginFailure) {
    this.afterBeginCalled = true;
    this.beginFailure = beginFailure;
  }

  @Override
  public void beforeCommit(TransactionExecution transactionState) {
    this.beforeCommitCalled = true;
  }

  @Override
  public void afterCommit(TransactionExecution transactionState, @Nullable Throwable commitFailure) {
    this.afterCommitCalled = true;
    this.commitFailure = commitFailure;
  }

  @Override
  public void beforeRollback(TransactionExecution transactionState) {
    this.beforeRollbackCalled = true;
  }

  @Override
  public void afterRollback(TransactionExecution transactionState, @Nullable Throwable rollbackFailure) {
    this.afterRollbackCalled = true;
    this.rollbackFailure = rollbackFailure;
  }

}
