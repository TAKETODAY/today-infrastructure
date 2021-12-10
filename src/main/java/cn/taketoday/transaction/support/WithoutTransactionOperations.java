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

package cn.taketoday.transaction.support;

import java.util.function.Consumer;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionStatus;

/**
 * A {@link TransactionOperations} implementation which executes a given
 * {@link TransactionCallback} without an actual transaction.
 *
 * @author Juergen Hoeller
 * @see TransactionOperations#withoutTransaction()
 * @since 4.0
 */
final class WithoutTransactionOperations implements TransactionOperations {

  static final WithoutTransactionOperations INSTANCE = new WithoutTransactionOperations();

  private WithoutTransactionOperations() {
  }

  @Override
  @Nullable
  public <T> T execute(TransactionCallback<T> action) throws TransactionException {
    return action.doInTransaction(new SimpleTransactionStatus(false));
  }

  @Override
  public void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
    action.accept(new SimpleTransactionStatus(false));
  }

}
