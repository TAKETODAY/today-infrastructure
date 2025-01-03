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

package infra.transaction;

import infra.lang.Nullable;
import infra.transaction.support.CallbackPreferringPlatformTransactionManager;
import infra.transaction.support.SimpleTransactionStatus;
import infra.transaction.support.TransactionCallback;
import infra.util.ExceptionUtils;

/**
 * @author Juergen Hoeller
 */
public class MockCallbackPreferringTransactionManager implements CallbackPreferringPlatformTransactionManager {

  private TransactionDefinition definition;

  private TransactionStatus status;

  @Override
  public <T> T execute(TransactionDefinition definition, TransactionCallback<T> callback) throws TransactionException {
    this.definition = definition;
    this.status = new SimpleTransactionStatus();
    try {
      return callback.doInTransaction(this.status);
    }
    catch (Throwable e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  public TransactionDefinition getDefinition() {
    return this.definition;
  }

  public TransactionStatus getStatus() {
    return this.status;
  }

  @Override
  public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void commit(TransactionStatus status) throws TransactionException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void rollback(TransactionStatus status) throws TransactionException {
    throw new UnsupportedOperationException();
  }

}
