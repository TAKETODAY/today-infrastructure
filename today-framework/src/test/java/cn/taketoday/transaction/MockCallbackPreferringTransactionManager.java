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

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.support.CallbackPreferringPlatformTransactionManager;
import cn.taketoday.transaction.support.SimpleTransactionStatus;
import cn.taketoday.transaction.support.TransactionCallback;

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
    return callback.doInTransaction(this.status);
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
