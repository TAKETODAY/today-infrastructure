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

package cn.taketoday.transaction.reactive;

import java.util.Deque;

import cn.taketoday.transaction.NoTransactionException;

/**
 * Mutable holder for reactive transaction {@link TransactionContext contexts}.
 * This holder keeps references to individual {@link TransactionContext}s.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @see TransactionContext
 * @since 4.0
 */
final class TransactionContextHolder {

  private final Deque<TransactionContext> transactionStack;

  TransactionContextHolder(Deque<TransactionContext> transactionStack) {
    this.transactionStack = transactionStack;
  }

  /**
   * Return the current {@link TransactionContext}.
   *
   * @throws NoTransactionException if no transaction is ongoing
   */
  TransactionContext currentContext() {
    TransactionContext context = this.transactionStack.peek();
    if (context == null) {
      throw new NoTransactionException("No transaction in context");
    }
    return context;
  }

  /**
   * Create a new {@link TransactionContext}.
   */
  TransactionContext createContext() {
    TransactionContext context = this.transactionStack.peek();
    if (context != null) {
      context = new TransactionContext(context);
    }
    else {
      context = new TransactionContext();
    }
    this.transactionStack.push(context);
    return context;
  }

  /**
   * Check whether the holder has a {@link TransactionContext}.
   *
   * @return {@literal true} if a {@link TransactionContext} is associated
   */
  boolean hasContext() {
    return !this.transactionStack.isEmpty();
  }

}
