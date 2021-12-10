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

package cn.taketoday.transaction.jta;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

/**
 * Default implementation of the {@link TransactionFactory} strategy interface,
 * simply wrapping a standard JTA {@link TransactionManager}.
 *
 * <p>Does not support transaction names; simply ignores any specified name.
 *
 * @author Juergen Hoeller
 * @see TransactionManager#setTransactionTimeout(int)
 * @see TransactionManager#begin()
 * @see TransactionManager#getTransaction()
 * @since 4.0
 */
public class SimpleTransactionFactory implements TransactionFactory {

  private final TransactionManager transactionManager;

  /**
   * Create a new SimpleTransactionFactory for the given TransactionManager.
   *
   * @param transactionManager the JTA TransactionManager to wrap
   */
  public SimpleTransactionFactory(TransactionManager transactionManager) {
    Assert.notNull(transactionManager, "TransactionManager must not be null");
    this.transactionManager = transactionManager;
  }

  @Override
  public Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException {
    if (timeout >= 0) {
      this.transactionManager.setTransactionTimeout(timeout);
    }
    this.transactionManager.begin();
    return new ManagedTransactionAdapter(this.transactionManager);
  }

  @Override
  public boolean supportsResourceAdapterManagedTransactions() {
    return false;
  }

}
