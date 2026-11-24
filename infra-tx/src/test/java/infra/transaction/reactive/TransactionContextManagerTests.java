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

import infra.transaction.NoTransactionException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionContextManagerTests {

  @Test
  void currentContextFailsWhenNoContextIsRegistered() {
    StepVerifier.create(TransactionContextManager.currentContext())
            .expectErrorSatisfies(error -> {
              assertThat(error).isInstanceOf(NoTransactionException.class);
              assertThat(error).hasMessage("No transaction in context");
              assertThat(error.getStackTrace()).isEmpty();
            })
            .verify();
  }

  @Test
  void currentContextReadsExplicitContext() {
    var expected = new TransactionContext();

    StepVerifier.create(TransactionContextManager.currentContext()
                    .contextWrite(Context.of(TransactionContext.class, expected)))
            .assertNext(actual -> assertThat(actual).isSameAs(expected))
            .verifyComplete();
  }

  @Test
  void currentContextReadsContextFromHolder() {
    var holder = new TransactionContextHolder(new java.util.ArrayDeque<>());
    var expected = holder.createContext();

    StepVerifier.create(TransactionContextManager.currentContext()
                    .contextWrite(Context.of(TransactionContextHolder.class, holder)))
            .assertNext(actual -> assertThat(actual).isSameAs(expected))
            .verifyComplete();
  }

  @Test
  void getOrCreateContextHolderRetainsExistingHolder() {
    var holder = new TransactionContextHolder(new java.util.ArrayDeque<>());
    Context context = Context.of(TransactionContextHolder.class, holder);

    Context result = TransactionContextManager.getOrCreateContextHolder().apply(context);

    assertThat(result.get(TransactionContextHolder.class)).isSameAs(holder);
  }

  @Test
  void getOrCreateContextUsesAndCreatesHolderContext() {
    Context context = TransactionContextManager.getOrCreateContextHolder().apply(Context.empty());
    Context first = TransactionContextManager.getOrCreateContext().apply(context);
    Context second = TransactionContextManager.getOrCreateContext().apply(first);

    assertThat(first.get(TransactionContext.class)).isSameAs(second.get(TransactionContext.class));
    assertThat(first.get(TransactionContextHolder.class).hasContext()).isTrue();
  }

  @Test
  void createTransactionContextRegistersFreshContext() {
    var existing = new TransactionContext();
    Context result = TransactionContextManager.createTransactionContext().apply(
            Context.of(TransactionContext.class, existing));

    assertThat(result.get(TransactionContext.class)).isNotSameAs(existing);
  }

}
