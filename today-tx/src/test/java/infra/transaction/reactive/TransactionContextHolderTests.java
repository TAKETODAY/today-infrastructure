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

package infra.transaction.reactive;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import infra.transaction.NoTransactionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:53
 */
class TransactionContextHolderTests {

  @Test
  void shouldCreateTransactionContextHolderWithEmptyStack() {
    Deque<TransactionContext> stack = new ArrayDeque<>();
    TransactionContextHolder holder = new TransactionContextHolder(stack);

    assertThat(holder.hasContext()).isFalse();
  }

  @Test
  void shouldThrowExceptionWhenGettingCurrentContextFromEmptyHolder() {
    Deque<TransactionContext> stack = new ArrayDeque<>();
    TransactionContextHolder holder = new TransactionContextHolder(stack);

    assertThatThrownBy(holder::currentContext)
            .isInstanceOf(NoTransactionException.class)
            .hasMessage("No transaction in context");
  }

  @Test
  void shouldCreateNewContextWhenStackIsEmpty() {
    Deque<TransactionContext> stack = new ArrayDeque<>();
    TransactionContextHolder holder = new TransactionContextHolder(stack);

    TransactionContext context = holder.createContext();

    assertThat(context).isNotNull();
    assertThat(holder.hasContext()).isTrue();
    assertThat(holder.currentContext()).isSameAs(context);
  }

  @Test
  void shouldCreateNewContextBasedOnExistingContext() {
    Deque<TransactionContext> stack = new ArrayDeque<>();
    TransactionContextHolder holder = new TransactionContextHolder(stack);

    TransactionContext firstContext = holder.createContext();
    TransactionContext secondContext = holder.createContext();

    assertThat(secondContext).isNotNull();
    assertThat(secondContext).isNotSameAs(firstContext);
    assertThat(holder.currentContext()).isSameAs(secondContext);
  }

  @Test
  void shouldReturnCurrentContextWhenAvailable() {
    Deque<TransactionContext> stack = new ArrayDeque<>();
    TransactionContextHolder holder = new TransactionContextHolder(stack);

    TransactionContext context = holder.createContext();

    assertThat(holder.currentContext()).isSameAs(context);
  }

  @Test
  void shouldMaintainContextStackOrder() {
    Deque<TransactionContext> stack = new ArrayDeque<>();
    TransactionContextHolder holder = new TransactionContextHolder(stack);

    TransactionContext context1 = holder.createContext();
    TransactionContext context2 = holder.createContext();

    assertThat(holder.currentContext()).isSameAs(context2);

    stack.pop(); // Remove context2
    assertThat(holder.currentContext()).isSameAs(context1);
  }

  @Test
  void shouldReturnHasContextAsTrueWhenStackIsNotEmpty() {
    Deque<TransactionContext> stack = new ArrayDeque<>();
    TransactionContextHolder holder = new TransactionContextHolder(stack);

    holder.createContext();

    assertThat(holder.hasContext()).isTrue();
  }

}