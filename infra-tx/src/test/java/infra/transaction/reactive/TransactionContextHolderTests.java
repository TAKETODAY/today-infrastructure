/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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