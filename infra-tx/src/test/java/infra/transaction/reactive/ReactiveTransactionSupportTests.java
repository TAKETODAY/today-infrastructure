/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.transaction.reactive;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import infra.dao.ConcurrencyFailureException;
import infra.transaction.CannotCreateTransactionException;
import infra.transaction.IllegalTransactionStateException;
import infra.transaction.ReactiveTransaction;
import infra.transaction.ReactiveTransactionManager;
import infra.transaction.TestTransactionExecutionListener;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionSuspensionNotSupportedException;
import infra.transaction.TransactionSystemException;
import infra.transaction.UnexpectedRollbackException;
import infra.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for transactional support through {@link ReactiveTestTransactionManager}.
 *
 * @author Mark Paluch
 */
public class ReactiveTransactionSupportTests {

  @ParameterizedTest
  @ValueSource(ints = { TransactionDefinition.PROPAGATION_REQUIRES_NEW, TransactionDefinition.PROPAGATION_NOT_SUPPORTED })
  void suspendingPropagationRestoresOuterResourcesAndSynchronization(int propagation) {
    var events = new ArrayList<String>();
    var tm = new SuspendingTransactionManager(true, events);
    var definition = new DefaultTransactionDefinition(propagation);
    definition.setName("inner");
    boolean requiresNew = propagation == TransactionDefinition.PROPAGATION_REQUIRES_NEW;

    Mono<Void> execution = TransactionSynchronizationManager.forCurrentTransaction().flatMap(manager -> {
      registerOuterTransaction(manager, events);
      return tm.getReactiveTransaction(definition).flatMap(status -> {
        assertThat(status.isNewTransaction()).isEqualTo(requiresNew);
        assertThat(status.hasTransaction()).isEqualTo(requiresNew);
        assertThat(manager.getCurrentTransactionName()).isEqualTo("inner");
        assertThat(manager.isActualTransactionActive()).isEqualTo(requiresNew);
        assertThat(manager.isCurrentTransactionReadOnly()).isFalse();
        assertThat(manager.getResource("resource")).isNull();
        assertThat(manager.getSynchronizations()).isEmpty();
        return tm.commit(status).then(Mono.fromRunnable(() -> {
          assertThat(status.isCompleted()).isTrue();
          assertOuterTransactionRestored(manager);
        }));
      });
    });

    StepVerifier.create(execution.contextWrite(TransactionContextManager.createTransactionContext())).verifyComplete();
    assertThat(events).containsExactly("suspend synchronization", "suspend resource", "resume resource", "resume synchronization");
    assertThat(tm.begin).isEqualTo(requiresNew);
    assertThat(tm.commit).isEqualTo(requiresNew);
    assertThat(tm.cleanup).isEqualTo(requiresNew);
  }

  @Test
  void failedRequiresNewBeginRestoresOuterTransaction() {
    var events = new ArrayList<String>();
    var tm = new SuspendingTransactionManager(false, events);
    var listener = new TestTransactionExecutionListener();
    tm.addListener(listener);
    var definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    var execution = TransactionSynchronizationManager.forCurrentTransaction().flatMap(manager -> {
      registerOuterTransaction(manager, events);
      return tm.getReactiveTransaction(definition).doOnError(error -> assertOuterTransactionRestored(manager));
    });

    StepVerifier.create(execution.contextWrite(TransactionContextManager.createTransactionContext()))
            .verifyError(CannotCreateTransactionException.class);
    assertThat(events).containsExactly("suspend synchronization", "suspend resource", "resume resource", "resume synchronization");
    assertThat(listener.beginFailure).isInstanceOf(CannotCreateTransactionException.class);
    assertHasNotCommitted(tm);
  }

  @Test
  void requiresNewIsRejectedWhenSuspensionIsNotSupported() {
    var tm = new ReactiveTestTransactionManager(true, true);
    var definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    StepVerifier.create(tm.getReactiveTransaction(definition)
                    .contextWrite(TransactionContextManager.createTransactionContext()))
            .verifyError(TransactionSuspensionNotSupportedException.class);
    assertHasNotBegan(tm);
  }

  @Test
  void neverRejectsAnExistingTransaction() {
    var tm = new ReactiveTestTransactionManager(true, true);
    var definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NEVER);

    StepVerifier.create(tm.getReactiveTransaction(definition)
                    .contextWrite(TransactionContextManager.createTransactionContext()))
            .verifyError(IllegalTransactionStateException.class);
    assertHasNotBegan(tm);
  }

  @Test
  void nestedPropagationStartsAndCompletesANestedTransaction() {
    var tm = new ReactiveTestTransactionManager(true, true);
    var definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NESTED);

    var execution = tm.getReactiveTransaction(definition).flatMap(status -> {
      assertThat(status.isNested()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      return tm.commit(status).thenReturn(status);
    });

    StepVerifier.create(execution.contextWrite(TransactionContextManager.createTransactionContext()))
            .assertNext(status -> assertThat(status.isCompleted()).isTrue())
            .verifyComplete();
    assertHasBegan(tm);
    assertHasCommitted(tm);
    assertHasCleanedUp(tm);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void completedTransactionRejectsSecondCompletion(boolean commit) {
    var tm = new ReactiveTestTransactionManager(false, true);

    var execution = tm.getReactiveTransaction(null).flatMap(status ->
            tm.commit(status).then(Mono.defer(() -> commit ? tm.commit(status) : tm.rollback(status))));

    StepVerifier.create(execution.contextWrite(TransactionContextManager.createTransactionContext()))
            .verifyError(IllegalTransactionStateException.class);
    assertHasCommitted(tm);
    assertHasNoRollback(tm);
  }

  @Test
  void participatingSynchronizationReceivesUnknownCompletionStatus() {
    var tm = new ReactiveTestTransactionManager(true, true);
    var completion = new ArrayList<Integer>();

    var execution = tm.getReactiveTransaction(null).flatMap(status ->
            TransactionSynchronizationManager.forCurrentTransaction().flatMap(manager -> {
              registerCompletion(manager, completion);
              return tm.commit(status);
            }));

    StepVerifier.create(execution.contextWrite(TransactionContextManager.createTransactionContext())).verifyComplete();
    assertThat(completion).containsExactly(TransactionSynchronization.STATUS_UNKNOWN);
    assertHasNotCommitted(tm);
  }

  @Test
  void unexpectedRollbackReportsRolledBackCompletionWithoutRetryingRollback() {
    var failure = new UnexpectedRollbackException("Already rolled back by coordinator");
    var tm = new ReactiveTestTransactionManager(false, message -> failure, null);
    var completion = new ArrayList<Integer>();
    var listener = new TestTransactionExecutionListener();
    tm.addListener(listener);

    var execution = tm.getReactiveTransaction(null).flatMap(status ->
            TransactionSynchronizationManager.forCurrentTransaction().flatMap(manager -> {
              registerCompletion(manager, completion);
              return tm.commit(status);
            }));

    StepVerifier.create(execution.contextWrite(TransactionContextManager.createTransactionContext()))
            .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
            .verify();
    assertThat(completion).containsExactly(TransactionSynchronization.STATUS_ROLLED_BACK);
    assertThat(listener.afterRollbackCalled).isTrue();
    assertHasNoRollback(tm);
    assertHasCleanedUp(tm);
  }

  private void registerCompletion(TransactionSynchronizationManager manager, List<Integer> completion) {
    manager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public Mono<Void> afterCompletion(int status) {
        return Mono.fromRunnable(() -> completion.add(status));
      }
    });
  }

  private void registerOuterTransaction(TransactionSynchronizationManager manager, List<String> events) {
    manager.setCurrentTransactionName("outer");
    manager.setCurrentTransactionReadOnly(true);
    manager.setCurrentTransactionIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    manager.setActualTransactionActive(true);
    manager.bindResource("resource", "outer value");
    manager.initSynchronization();
    manager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public Mono<Void> suspend() {
        return Mono.fromRunnable(() -> events.add("suspend synchronization"));
      }

      @Override
      public Mono<Void> resume() {
        return Mono.fromRunnable(() -> events.add("resume synchronization"));
      }
    });
  }

  private void assertOuterTransactionRestored(TransactionSynchronizationManager manager) {
    assertThat(manager.getCurrentTransactionName()).isEqualTo("outer");
    assertThat(manager.isCurrentTransactionReadOnly()).isTrue();
    assertThat(manager.getCurrentTransactionIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_SERIALIZABLE);
    assertThat(manager.isActualTransactionActive()).isTrue();
    assertThat(manager.getResource("resource")).isEqualTo("outer value");
    assertThat(manager.getSynchronizations()).hasSize(1);
  }

  @SuppressWarnings("serial")
  private static class SuspendingTransactionManager extends ReactiveTestTransactionManager {

    private final List<String> events;

    SuspendingTransactionManager(boolean canCreateTransaction, List<String> events) {
      super(true, canCreateTransaction);
      this.events = events;
    }

    @Override
    protected Mono<Object> doSuspend(TransactionSynchronizationManager manager, Object transaction) {
      return Mono.fromSupplier(() -> {
        events.add("suspend resource");
        return manager.unbindResource("resource");
      });
    }

    @Override
    protected Mono<Void> doResume(TransactionSynchronizationManager manager, @Nullable Object transaction, Object resource) {
      return Mono.fromRunnable(() -> {
        events.add("resume resource");
        manager.bindResource("resource", resource);
      });
    }

  }

  @Test
  public void noExistingTransaction() {
    ReactiveTransactionManager tm = new ReactiveTestTransactionManager(false, true);

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).consumeNextWith(actual -> assertThat(actual.hasTransaction()).isFalse()
            ).verifyComplete();

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED))
            .cast(GenericReactiveTransaction.class).contextWrite(TransactionContextManager.createTransactionContext())
            .as(StepVerifier::create).consumeNextWith(actual -> {
              assertThat(actual.hasTransaction()).isTrue();
              assertThat(actual.isNewTransaction()).isTrue();
            }).verifyComplete();

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).expectError(IllegalTransactionStateException.class).verify();
  }

  @Test
  public void existingTransaction() {
    ReactiveTransactionManager tm = new ReactiveTestTransactionManager(true, true);

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).consumeNextWith(actual -> {
              assertThat(actual.getTransaction()).isNotNull();
              assertThat(actual.isNewTransaction()).isFalse();
            }).verifyComplete();

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).consumeNextWith(actual -> {
              assertThat(actual.getTransaction()).isNotNull();
              assertThat(actual.isNewTransaction()).isFalse();
            }).verifyComplete();

    tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY))
            .contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
            .as(StepVerifier::create).consumeNextWith(actual -> {
              assertThat(actual.getTransaction()).isNotNull();
              assertThat(actual.isNewTransaction()).isFalse();
            }).verifyComplete();
  }

  @Test
  public void commitWithoutExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext())
            .as(StepVerifier::create).verifyComplete();

    assertHasBegan(tm);
    assertHasCommitted(tm);
    assertHasNoRollback(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beforeCommitCalled).isTrue();
    assertThat(tl.afterCommitCalled).isTrue();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
  }

  @Test
  public void rollbackWithoutExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
            .contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
            .verifyComplete();

    assertHasBegan(tm);
    assertHasNotCommitted(tm);
    assertHasRolledBack(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);

    assertThat(tm.getTransactionExecutionListeners()).contains(tl);
    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isTrue();
    assertThat(tl.afterRollbackCalled).isTrue();
  }

  @Test
  public void rollbackOnlyWithoutExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    tm.getReactiveTransaction(new DefaultTransactionDefinition()).doOnNext(ReactiveTransaction::setRollbackOnly)
            .flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
            .verifyComplete();

    assertHasBegan(tm);
    assertHasNotCommitted(tm);
    assertHasRolledBack(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isTrue();
    assertThat(tl.afterRollbackCalled).isTrue();
  }

  @Test
  public void commitWithExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext())
            .as(StepVerifier::create).verifyComplete();

    assertHasNotBegan(tm);
    assertHasNotCommitted(tm);
    assertHasNoRollback(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasNotCleanedUp(tm);

    assertThat(tl.beforeBeginCalled).isFalse();
    assertThat(tl.afterBeginCalled).isFalse();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
  }

  @Test
  public void rollbackWithExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
            .contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
            .verifyComplete();

    assertHasNotBegan(tm);
    assertHasNotCommitted(tm);
    assertHasNoRollback(tm);
    assertHasSetRollbackOnly(tm);
    assertHasNotCleanedUp(tm);

    assertThat(tl.beforeBeginCalled).isFalse();
    assertThat(tl.afterBeginCalled).isFalse();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
  }

  @Test
  public void rollbackOnlyWithExistingTransaction() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    tm.getReactiveTransaction(new DefaultTransactionDefinition()).doOnNext(ReactiveTransaction::setRollbackOnly).flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
            .verifyComplete();

    assertHasNotBegan(tm);
    assertHasNotCommitted(tm);
    assertHasNoRollback(tm);
    assertHasSetRollbackOnly(tm);
    assertHasNotCleanedUp(tm);

    assertThat(tl.beforeBeginCalled).isFalse();
    assertThat(tl.afterBeginCalled).isFalse();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
  }

  @Test
  public void transactionTemplate() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());

    Flux.just("Walter").as(operator::transactional)
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

    assertHasBegan(tm);
    assertHasCommitted(tm);
    assertHasNoRollback(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);
  }

  @Test
  public void transactionTemplateWithException() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
    TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
    RuntimeException ex = new RuntimeException("Some application exception");

    Mono.error(ex).as(operator::transactional)
            .as(StepVerifier::create)
            .expectError(RuntimeException.class)
            .verify();

    assertHasBegan(tm);
    assertHasNotCommitted(tm);
    assertHasRolledBack(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);
  }

  @Test
    // gh-28968
  void errorInCommitDoesInitiateRollbackAfterCommit() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, ConcurrencyFailureException::new, null);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);
    TransactionalOperator rxtx = TransactionalOperator.create(tm);

    StepVerifier.create(rxtx.transactional(Mono.just("bar")))
            .verifyErrorMessage("Forced failure on commit");

    assertHasBegan(tm);
    assertHasCommitted(tm);
    assertHasRolledBack(tm);
    assertHasNotSetRollbackOnly(tm);
    assertHasCleanedUp(tm);

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beforeCommitCalled).isTrue();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isTrue();
  }

  @Test
  public void beginFailure() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, false);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext())
            .as(StepVerifier::create).verifyError();

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beginFailure).isInstanceOf(CannotCreateTransactionException.class);
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.commitFailure).isNull();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
    assertThat(tl.rollbackFailure).isNull();
  }

  @Test
  public void commitFailure() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, TransactionSystemException::new, null);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
            .contextWrite(TransactionContextManager.createTransactionContext())
            .as(StepVerifier::create).verifyError();

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beginFailure).isNull();
    assertThat(tl.beforeCommitCalled).isTrue();
    assertThat(tl.afterCommitCalled).isTrue();
    assertThat(tl.commitFailure).isInstanceOf(TransactionSystemException.class);
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
    assertThat(tl.rollbackFailure).isNull();
  }

  @Test
  public void rollbackFailure() {
    ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, null, TransactionSystemException::new);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
            .contextWrite(TransactionContextManager.createTransactionContext())
            .as(StepVerifier::create).verifyError();

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beginFailure).isNull();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.commitFailure).isNull();
    assertThat(tl.beforeRollbackCalled).isTrue();
    assertThat(tl.afterRollbackCalled).isTrue();
    assertThat(tl.rollbackFailure).isInstanceOf(TransactionSystemException.class);
  }

  private void assertHasBegan(ReactiveTestTransactionManager actual) {
    assertThat(actual.begin).as("Expected <ReactiveTransactionManager.begin()> but was <begin()> was not invoked").isTrue();
  }

  private void assertHasNotBegan(ReactiveTestTransactionManager actual) {
    assertThat(actual.begin).as("Expected to not call <ReactiveTransactionManager.begin()> but was <begin()> was called").isFalse();
  }

  private void assertHasCommitted(ReactiveTestTransactionManager actual) {
    assertThat(actual.commit).as("Expected <ReactiveTransactionManager.commit()> but was <commit()> was not invoked").isTrue();
  }

  private void assertHasNotCommitted(ReactiveTestTransactionManager actual) {
    assertThat(actual.commit).as("Expected to not call <ReactiveTransactionManager.commit()> but was <commit()> was called").isFalse();
  }

  private void assertHasRolledBack(ReactiveTestTransactionManager actual) {
    assertThat(actual.rollback).as("Expected <ReactiveTransactionManager.rollback()> but was <rollback()> was not invoked").isTrue();
  }

  private void assertHasNoRollback(ReactiveTestTransactionManager actual) {
    assertThat(actual.rollback).as("Expected to not call <ReactiveTransactionManager.rollback()> but was <rollback()> was called").isFalse();
  }

  private void assertHasSetRollbackOnly(ReactiveTestTransactionManager actual) {
    assertThat(actual.rollbackOnly).as("Expected <ReactiveTransactionManager.setRollbackOnly()> but was <setRollbackOnly()> was not invoked").isTrue();
  }

  private void assertHasNotSetRollbackOnly(ReactiveTestTransactionManager actual) {
    assertThat(actual.rollbackOnly).as("Expected to not call <ReactiveTransactionManager.setRollbackOnly()> but was <setRollbackOnly()> was called").isFalse();
  }

  private void assertHasCleanedUp(ReactiveTestTransactionManager actual) {
    assertThat(actual.cleanup).as("Expected <ReactiveTransactionManager.doCleanupAfterCompletion()> but was <doCleanupAfterCompletion()> was not invoked").isTrue();
  }

  private void assertHasNotCleanedUp(ReactiveTestTransactionManager actual) {
    assertThat(actual.cleanup).as("Expected to not call <ReactiveTransactionManager.doCleanupAfterCompletion()> but was <doCleanupAfterCompletion()> was called").isFalse();
  }

}
