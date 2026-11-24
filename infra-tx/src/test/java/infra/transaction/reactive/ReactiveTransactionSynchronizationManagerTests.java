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
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactiveTransactionSynchronizationManagerTests {

  @Test
  void managesResourcesAndTransactionMetadata() {
    var context = new TransactionContext();
    var manager = new TransactionSynchronizationManager(context);
    Object key = new Object();
    Object value = new Object();

    manager.bindResource(key, value);
    assertThat(manager.hasResource(key)).isTrue();
    assertThat(manager.getResource(key)).isSameAs(value);
    assertThat(manager.unbindResource(key)).isSameAs(value);
    assertThat(manager.unbindResourceIfPossible(key)).isNull();

    manager.setCurrentTransactionName("name");
    manager.setCurrentTransactionReadOnly(true);
    manager.setCurrentTransactionIsolationLevel(2);
    manager.setActualTransactionActive(true);
    assertThat(manager.getCurrentTransactionName()).isEqualTo("name");
    assertThat(manager.isCurrentTransactionReadOnly()).isTrue();
    assertThat(manager.getCurrentTransactionIsolationLevel()).isEqualTo(2);
    assertThat(manager.isActualTransactionActive()).isTrue();
  }

  @Test
  void rejectsDuplicateAndMissingResourceOperations() {
    var manager = new TransactionSynchronizationManager(new TransactionContext());
    Object key = new Object();
    manager.bindResource(key, "value");

    assertThatThrownBy(() -> manager.bindResource(key, "other"))
            .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> manager.unbindResource(new Object()))
            .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void managesSynchronizations() {
    var manager = new TransactionSynchronizationManager(new TransactionContext());
    var synchronization = new TransactionSynchronization() { };

    assertThat(manager.isSynchronizationActive()).isFalse();
    manager.initSynchronization();
    manager.registerSynchronization(synchronization);
    assertThat(manager.isSynchronizationActive()).isTrue();
    assertThat(manager.getSynchronizations()).containsExactly(synchronization);
    manager.clearSynchronization();
    assertThat(manager.isSynchronizationActive()).isFalse();
    assertThatThrownBy(manager::getSynchronizations)
            .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void forCurrentTransactionRequiresReactiveContext() {
    StepVerifier.create(TransactionSynchronizationManager.forCurrentTransaction())
            .expectError(NoTransactionException.class)
            .verify();
  }

  @Test
  void synchronizationManagerCanBeCreatedFromReactiveContext() {
    var context = new TransactionContext();
    StepVerifier.create(TransactionSynchronizationManager.forCurrentTransaction()
                    .contextWrite(reactor.util.context.Context.of(TransactionContext.class, context)))
            .assertNext(manager -> assertThat(manager.getCurrentTransactionName()).isNull())
            .verifyComplete();
  }

}
