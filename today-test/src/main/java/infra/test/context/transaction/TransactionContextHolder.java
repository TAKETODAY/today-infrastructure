/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.context.transaction;

import org.jspecify.annotations.Nullable;

import infra.core.NamedInheritableThreadLocal;

/**
 * {@link InheritableThreadLocal}-based holder for the current {@link TransactionContext}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
final class TransactionContextHolder {

  private static final ThreadLocal<TransactionContext> currentTransactionContext =
          new NamedInheritableThreadLocal<>("Test Transaction Context");

  private TransactionContextHolder() {
  }

  static void setCurrentTransactionContext(TransactionContext transactionContext) {
    currentTransactionContext.set(transactionContext);
  }

  @Nullable
  static TransactionContext getCurrentTransactionContext() {
    return currentTransactionContext.get();
  }

  @Nullable
  static TransactionContext removeCurrentTransactionContext() {
    TransactionContext transactionContext = currentTransactionContext.get();
    currentTransactionContext.remove();
    return transactionContext;
  }

}
