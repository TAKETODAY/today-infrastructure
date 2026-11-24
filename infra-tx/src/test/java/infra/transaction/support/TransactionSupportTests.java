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

package infra.transaction.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import infra.transaction.IllegalTransactionStateException;
import infra.transaction.InvalidTimeoutException;
import infra.transaction.MockCallbackPreferringTransactionManager;
import infra.transaction.NestedTransactionNotSupportedException;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.SavepointManager;
import infra.transaction.TestTransactionExecutionListener;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionStatus;
import infra.transaction.TransactionSuspensionNotSupportedException;
import infra.transaction.TransactionSystemException;
import infra.transaction.UnexpectedRollbackException;
import infra.util.ReflectionUtils;

import static infra.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ;
import static infra.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE;
import static infra.transaction.TransactionDefinition.PROPAGATION_MANDATORY;
import static infra.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import static infra.transaction.TransactionDefinition.PROPAGATION_SUPPORTS;
import static infra.transaction.support.AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION;
import static infra.transaction.support.DefaultTransactionDefinition.PREFIX_ISOLATION;
import static infra.transaction.support.DefaultTransactionDefinition.PREFIX_PROPAGATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 */
class TransactionSupportTests {

  @Test
  void rejectsInvalidTimeoutBeforeStartingTransaction() {
    var manager = new TestTransactionManager(false, true);
    assertThatExceptionOfType(InvalidTimeoutException.class).isThrownBy(() -> manager.setDefaultTimeout(-2));
    TransactionDefinition invalid = new TransactionDefinition() {
      @Override
      public int getTimeout() {
        return TIMEOUT_DEFAULT - 1;
      }
    };

    assertThatExceptionOfType(InvalidTimeoutException.class).isThrownBy(() -> manager.getTransaction(invalid));
    assertThat(manager.begin).isFalse();
  }

  @Test
  void rejectsNeverAndNestedPropagationWhenAnExistingTransactionDoesNotAllowThem() {
    var manager = new TestTransactionManager(true, true);
    var never = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NEVER);
    var nested = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NESTED);

    assertThatExceptionOfType(IllegalTransactionStateException.class)
            .isThrownBy(() -> manager.getTransaction(never)).withMessageContaining("propagation 'never'");
    assertThatExceptionOfType(NestedTransactionNotSupportedException.class)
            .isThrownBy(() -> manager.getTransaction(nested));
    assertThat(manager.begin).isFalse();
  }

  @Test
  void restoresSynchronizationWhenSuspendingTheTransactionFails() {
    var manager = new TestTransactionManager(true, true);
    TransactionSynchronization synchronization = mock();
    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(synchronization);
      var definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

      assertThatExceptionOfType(TransactionSuspensionNotSupportedException.class)
              .isThrownBy(() -> manager.getTransaction(definition));

      var order = inOrder(synchronization);
      order.verify(synchronization).suspend();
      order.verify(synchronization).resume();
      assertThat(TransactionSynchronizationManager.getSynchronizations()).containsExactly(synchronization);
    }
    finally {
      TransactionSynchronizationManager.clear();
    }
  }

  @Test
  void reportsResumeFailureWhenRequiresNewCannotBeginOrResume() {
    var manager = new TestTransactionManager(true, false) {
      @Override
      protected Object doSuspend(Object transaction) {
        return transaction;
      }
    };
    var definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    assertThatExceptionOfType(TransactionSuspensionNotSupportedException.class)
            .isThrownBy(() -> manager.getTransaction(definition));
    assertThat(manager.begin).isFalse();
  }

  @Test
  void globalRollbackOnlyPreventsCommit() {
    SmartTransactionObject transaction = mock();
    given(transaction.isRollbackOnly()).willReturn(true);
    var manager = new TestTransactionManager(false, true) {
      @Override
      protected Object doGetTransaction() {
        return transaction;
      }

      @Override
      protected void doBegin(Object resource, TransactionDefinition definition) {
      }

      @Override
      protected void doRollback(DefaultTransactionStatus status) {
        this.rollback = true;
      }
    };

    var status = manager.getTransaction(null);
    assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() -> manager.commit(status));
    assertThat(status.isCompleted()).isTrue();
    assertThat(manager.commit).isFalse();
    assertThat(manager.rollback).isTrue();
  }

  @Test
  void rollbackFailureAfterCommitFailureNotifiesListenersAndClearsSynchronization() {
    var commitFailure = new TransactionSystemException("commit failed");
    var rollbackFailure = new TransactionSystemException("rollback failed");
    var manager = new TestTransactionManager(false, true) {
      @Override
      protected void doCommit(DefaultTransactionStatus status) {
        throw commitFailure;
      }

      @Override
      protected void doRollback(DefaultTransactionStatus status) {
        throw rollbackFailure;
      }
    };
    manager.setRollbackOnCommitFailure(true);
    manager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS);
    var listener = new TestTransactionExecutionListener();
    manager.addListener(listener);
    var status = manager.getTransaction(null);
    TransactionSynchronization synchronization = mock();
    TransactionSynchronizationManager.registerSynchronization(synchronization);

    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> manager.commit(status))
            .isSameAs(rollbackFailure);
    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    assertThat(listener.rollbackFailure).isSameAs(rollbackFailure);
    assertThat(status.isCompleted()).isTrue();
  }

  @Test
  void emptySupportsTransactionCanActivateSynchronization() {
    var manager = new TestTransactionManager(false, true);
    manager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS);
    var definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS);
    definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

    var status = manager.getTransaction(definition);
    assertThat(status.hasTransaction()).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())
            .isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);

    manager.commit(status);
    assertThat(status.isCompleted()).isTrue();
  }

  @Test
  void participatingRollbackCanLeaveGlobalTransactionUnmarked() {
    var manager = new TestTransactionManager(true, true);
    manager.setGlobalRollbackOnParticipationFailure(false);
    var status = manager.getTransaction(null);

    manager.rollback(status);

    assertThat(manager.rollbackOnly).isFalse();
    assertThat(status.isCompleted()).isTrue();
  }

  @Test
  void participatingSynchronizationReportsUnknownWhenCoordinatorCannotTrackCompletion() {
    var manager = new TestTransactionManager(true, true);
    manager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS);
    var status = manager.getTransaction(null);
    TransactionSynchronization synchronization = mock();
    TransactionSynchronizationManager.registerSynchronization(synchronization);

    manager.commit(status);

    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    assertThat(manager.commit).isFalse();
    assertThat(status.isCompleted()).isTrue();
  }

  @Test
  void commitFailureWithoutRollbackOnCommitFailureReportsUnknownCompletion() {
    var failure = new TransactionSystemException("commit failed");
    var manager = new TestTransactionManager(false, true) {
      @Override
      protected void doCommit(DefaultTransactionStatus status) {
        throw failure;
      }
    };
    manager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS);
    var listener = new TestTransactionExecutionListener();
    manager.addListener(listener);
    var status = manager.getTransaction(null);
    TransactionSynchronization synchronization = mock();
    TransactionSynchronizationManager.registerSynchronization(synchronization);

    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> manager.commit(status))
            .isSameAs(failure);
    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    assertThat(listener.afterCommitCalled).isTrue();
    assertThat(manager.rollback).isFalse();
  }

  @Test
  void afterCommitFailureStillRunsCompletionCallbacks() {
    var failure = new IllegalStateException("after commit failed");
    var manager = new TestTransactionManager(false, true);
    manager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS);
    var synchronization = mock(TransactionSynchronization.class);
    org.mockito.BDDMockito.willThrow(failure).given(synchronization).afterCommit();
    var status = manager.getTransaction(null);
    TransactionSynchronizationManager.registerSynchronization(synchronization);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> manager.commit(status)).isSameAs(failure);
    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(status.isCompleted()).isTrue();
  }

  @Test
  void validatesIsolationAndReadOnlySettingsWhenJoiningAnExistingTransaction() {
    var manager = new TestTransactionManager(true, true);
    manager.setValidateExistingTransaction(true);
    var definition = new DefaultTransactionDefinition();
    definition.setIsolationLevel(ISOLATION_SERIALIZABLE);
    definition.setReadOnly(true);

    assertThatExceptionOfType(IllegalTransactionStateException.class)
            .isThrownBy(() -> manager.getTransaction(definition)).withMessageContaining("(unknown)");
    try {
      TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(ISOLATION_REPEATABLE_READ);
      assertThatExceptionOfType(IllegalTransactionStateException.class)
              .isThrownBy(() -> manager.getTransaction(definition)).withMessageContaining("ISOLATION_REPEATABLE_READ");

      TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(ISOLATION_SERIALIZABLE);
      TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
      var participant = manager.getTransaction(definition);
      assertThat(participant.isNewTransaction()).isFalse();
      manager.commit(participant);

      definition.setReadOnly(false);
      assertThatExceptionOfType(IllegalTransactionStateException.class)
              .isThrownBy(() -> manager.getTransaction(definition)).withMessageContaining("not marked as read-only");
    }
    finally {
      TransactionSynchronizationManager.clear();
    }
  }

  @Test
  void nestedTransactionsCommitAndRollBackThroughSavepoints() {
    SavepointManager resource = mock();
    Object first = new Object();
    Object second = new Object();
    given(resource.createSavepoint()).willReturn(first, second);
    var manager = new TestTransactionManager(true, true) {
      @Override
      protected Object doGetTransaction() {
        return resource;
      }
    };
    var listener = new TestTransactionExecutionListener();
    manager.setNestedTransactionAllowed(true);
    manager.addListener(listener);
    var definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NESTED);

    var committed = manager.getTransaction(definition);
    assertThat(committed.hasSavepoint()).isTrue();
    assertThat(listener.afterBeginCalled).isTrue();
    manager.commit(committed);
    var rolledBack = manager.getTransaction(definition);
    manager.rollback(rolledBack);

    var order = inOrder(resource);
    order.verify(resource).createSavepoint();
    order.verify(resource).releaseSavepoint(first);
    order.verify(resource).createSavepoint();
    order.verify(resource).rollbackToSavepoint(second);
    order.verify(resource).releaseSavepoint(second);
    assertThat(committed.isCompleted()).isTrue();
    assertThat(rolledBack.isCompleted()).isTrue();
    assertThat(manager.commit).isFalse();
    assertThat(manager.rollback).isFalse();

    var savepointFailure = new TransactionSystemException("cannot create savepoint");
    given(resource.createSavepoint()).willThrow(savepointFailure);
    assertThatExceptionOfType(TransactionSystemException.class)
            .isThrownBy(() -> manager.getTransaction(definition)).isSameAs(savepointFailure);
    assertThat(listener.beginFailure).isSameAs(savepointFailure);
  }

  @AfterEach
  void postConditions() {
    assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  @Test
  void noExistingTransaction() {
    PlatformTransactionManager tm = new TestTransactionManager(false, true);

    DefaultTransactionStatus status1 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_SUPPORTS));
    assertThat(status1.hasTransaction()).as("Must not have transaction").isFalse();

    DefaultTransactionStatus status2 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRED));
    assertThat(status2.hasTransaction()).as("Must have transaction").isTrue();
    assertThat(status2.isNewTransaction()).as("Must be new transaction").isTrue();

    assertThatExceptionOfType(IllegalTransactionStateException.class)
            .isThrownBy(() -> tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_MANDATORY)));
  }

  @Test
  void existingTransaction() {
    PlatformTransactionManager tm = new TestTransactionManager(true, true);

    DefaultTransactionStatus status1 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_SUPPORTS));
    assertThat(status1.getTransaction()).as("Must have transaction").isNotNull();
    assertThat(status1.isNewTransaction()).as("Must not be new transaction").isFalse();

    DefaultTransactionStatus status2 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRED));
    assertThat(status2.getTransaction()).as("Must have transaction").isNotNull();
    assertThat(status2.isNewTransaction()).as("Must not be new transaction").isFalse();

    DefaultTransactionStatus status3 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_MANDATORY));
    assertThat(status3.getTransaction()).as("Must have transaction").isNotNull();
    assertThat(status3.isNewTransaction()).as("Must not be new transaction").isFalse();
  }

  @Test
  void commitWithoutExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    TransactionStatus status = tm.getTransaction(null);
    tm.commit(status);

    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("triggered commit").isTrue();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beforeCommitCalled).isTrue();
    assertThat(tl.afterCommitCalled).isTrue();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
  }

  @Test
  void rollbackWithoutExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    TransactionStatus status = tm.getTransaction(null);
    tm.rollback(status);

    assertThat(tm.getTransactionExecutionListeners()).contains(tl);
    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isTrue();
    assertThat(tl.afterRollbackCalled).isTrue();
  }

  @Test
  void rollbackOnlyWithoutExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    TransactionStatus status = tm.getTransaction(null);
    status.setRollbackOnly();
    tm.commit(status);

    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isTrue();
    assertThat(tl.afterRollbackCalled).isTrue();
  }

  @Test
  void commitWithExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(true, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    TransactionStatus status = tm.getTransaction(null);
    tm.commit(status);

    assertThat(tm.begin).as("no begin").isFalse();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();

    assertThat(tl.beforeBeginCalled).isFalse();
    assertThat(tl.afterBeginCalled).isFalse();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
  }

  @Test
  void rollbackWithExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(true, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    TransactionStatus status = tm.getTransaction(null);
    tm.rollback(status);

    assertThat(tm.begin).as("no begin").isFalse();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("triggered rollbackOnly").isTrue();

    assertThat(tl.beforeBeginCalled).isFalse();
    assertThat(tl.afterBeginCalled).isFalse();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
  }

  @Test
  void rollbackOnlyWithExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(true, true);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    tm.addListener(tl);

    TransactionStatus status = tm.getTransaction(null);
    status.setRollbackOnly();
    tm.commit(status);

    assertThat(tm.begin).as("no begin").isFalse();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("triggered rollbackOnly").isTrue();

    assertThat(tl.beforeBeginCalled).isFalse();
    assertThat(tl.afterBeginCalled).isFalse();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
  }

  @Test
  void transactionTemplate() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionTemplate template = new TransactionTemplate(tm);
    template.executeWithoutResult(status -> {

    });

    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("triggered commit").isTrue();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  void transactionTemplateWithCallbackPreference() {
    MockCallbackPreferringTransactionManager ptm = new MockCallbackPreferringTransactionManager();
    TransactionTemplate template = new TransactionTemplate(ptm);
    template.executeWithoutResult(status -> {
    });

    assertThat(ptm.getDefinition()).isSameAs(template);
    assertThat(ptm.getStatus().isRollbackOnly()).isFalse();
  }

  @Test
  void transactionTemplateWithException() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionTemplate template = new TransactionTemplate(tm);
    RuntimeException ex = new RuntimeException("Some application exception");
    assertThatRuntimeException()
            .isThrownBy(() -> template.executeWithoutResult(status -> {
              throw ex;
            }))
            .isSameAs(ex);
    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  void transactionTemplateWithRollbackException() {
    final TransactionSystemException tex = new TransactionSystemException("system exception");
    TestTransactionManager tm = new TestTransactionManager(false, true) {
      @Override
      protected void doRollback(DefaultTransactionStatus status) {
        super.doRollback(status);
        throw tex;
      }
    };
    TransactionTemplate template = new TransactionTemplate(tm);
    RuntimeException ex = new RuntimeException("Some application exception");
    assertThatRuntimeException()
            .isThrownBy(() -> template.executeWithoutResult(status -> { throw ex; }))
            .isSameAs(tex);
    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  void transactionTemplateWithError() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionTemplate template = new TransactionTemplate(tm);
    assertThatExceptionOfType(Error.class)
            .isThrownBy(() -> template.executeWithoutResult(status -> { throw new Error("Some application error"); }));
    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  void transactionTemplateEquality() {
    TestTransactionManager tm1 = new TestTransactionManager(false, true);
    TestTransactionManager tm2 = new TestTransactionManager(false, true);
    TransactionTemplate template1 = new TransactionTemplate(tm1);
    TransactionTemplate template2 = new TransactionTemplate(tm2);
    TransactionTemplate template3 = new TransactionTemplate(tm2);

    assertThat(template2).isNotEqualTo(template1);
    assertThat(template3).isNotEqualTo(template1);
    assertThat(template3).isEqualTo(template2);
  }

  @Test
  void setTransactionExecutionListeners() {
    TestTransactionManager tm1 = new TestTransactionManager(false, true);

    tm1.setTransactionExecutionListeners(null);
    assertThat(tm1.getTransactionExecutionListeners()).isEmpty();

    tm1.setTransactionExecutionListeners(List.of());
    assertThat(tm1.getTransactionExecutionListeners()).isEmpty();

    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();

    tm1.setTransactionExecutionListeners(List.of(tl));
    assertThat(tm1.getTransactionExecutionListeners()).contains(tl);

    tm1.setTransactionExecutionListeners(List.of());
    assertThat(tm1.getTransactionExecutionListeners()).isEmpty();
  }

  @Test
  void addListener() {
    TestTransactionManager tm = new TestTransactionManager(false, true);

    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    TestTransactionExecutionListener t2 = new TestTransactionExecutionListener();

    tm.addListener(tl);
    assertThat(tm.getTransactionExecutionListeners()).contains(tl);

    tm.addListener(t2);

    assertThat(tm.getTransactionExecutionListeners()).contains(t2);
    assertThat(tm.getTransactionExecutionListeners()).contains(tl);
  }

  @Nested
  class AbstractPlatformTransactionManagerConfigurationTests {

    private final AbstractPlatformTransactionManager tm = new TestTransactionManager(false, true);

    @Test
    void setTransactionSynchronizationNameToUnsupportedValues() {
      assertThatIllegalArgumentException().isThrownBy(() -> tm.setTransactionSynchronizationName(null));
      assertThatIllegalArgumentException().isThrownBy(() -> tm.setTransactionSynchronizationName("   "));
      assertThatIllegalArgumentException().isThrownBy(() -> tm.setTransactionSynchronizationName("bogus"));
    }

    /**
     * Verify that the internal 'constants' map is properly configured for all
     * SYNCHRONIZATION_ constants defined in {@link AbstractPlatformTransactionManager}.
     */
    @Test
    void setTransactionSynchronizationNameToAllSupportedValues() {
      Set<Integer> uniqueValues = new HashSet<>();
      streamSynchronizationConstants()
              .forEach(name -> {
                tm.setTransactionSynchronizationName(name);
                int transactionSynchronization = tm.getTransactionSynchronization();
                int expected = AbstractPlatformTransactionManager.constants.get(name);
                assertThat(transactionSynchronization).isEqualTo(expected);
                uniqueValues.add(transactionSynchronization);
              });
      assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(AbstractPlatformTransactionManager.constants.values());
    }

    @Test
    void setTransactionSynchronization() {
      tm.setTransactionSynchronization(SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
      assertThat(tm.getTransactionSynchronization()).isEqualTo(SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
    }

    private static Stream<String> streamSynchronizationConstants() {
      return Arrays.stream(AbstractPlatformTransactionManager.class.getFields())
              .filter(ReflectionUtils::isPublicStaticFinal)
              .map(Field::getName)
              .filter(name -> name.startsWith("SYNCHRONIZATION_"));
    }
  }

  @Nested
  class TransactionTemplateConfigurationTests {

    private final TransactionTemplate template = new TransactionTemplate();

    @Test
    void setTransactionManager() {
      TestTransactionManager tm = new TestTransactionManager(false, true);
      template.setTransactionManager(tm);
      assertThat(template.getTransactionManager()).as("correct transaction manager set").isSameAs(tm);
    }

    @Test
    void setPropagationBehaviorNameToUnsupportedValues() {
      assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehaviorName(null));
      assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehaviorName("   "));
      assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehaviorName("bogus"));
      assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehaviorName("ISOLATION_SERIALIZABLE"));
    }

    /**
     * Verify that the internal 'propagationConstants' map is properly configured
     * for all PROPAGATION_ constants defined in {@link TransactionDefinition}.
     */
    @Test
    void setPropagationBehaviorNameToAllSupportedValues() {
      Set<Integer> uniqueValues = new HashSet<>();
      streamPropagationConstants()
              .forEach(name -> {
                template.setPropagationBehaviorName(name);
                int propagationBehavior = template.getPropagationBehavior();
                int expected = DefaultTransactionDefinition.propagationConstants.get(name);
                assertThat(propagationBehavior).isEqualTo(expected);
                uniqueValues.add(propagationBehavior);
              });
      assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(DefaultTransactionDefinition.propagationConstants.values());
    }

    @Test
    void setPropagationBehavior() {
      assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehavior(999));
      assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehavior(ISOLATION_SERIALIZABLE));

      template.setPropagationBehavior(PROPAGATION_MANDATORY);
      assertThat(template.getPropagationBehavior()).isEqualTo(PROPAGATION_MANDATORY);
    }

    @Test
    void setIsolationLevelNameToUnsupportedValues() {
      assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevelName(null));
      assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevelName("   "));
      assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevelName("bogus"));
      assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevelName("PROPAGATION_MANDATORY"));
    }

    /**
     * Verify that the internal 'isolationConstants' map is properly configured
     * for all ISOLATION_ constants defined in {@link TransactionDefinition}.
     */
    @Test
    void setIsolationLevelNameToAllSupportedValues() {
      Set<Integer> uniqueValues = new HashSet<>();
      streamIsolationConstants()
              .forEach(name -> {
                template.setIsolationLevelName(name);
                int isolationLevel = template.getIsolationLevel();
                int expected = DefaultTransactionDefinition.isolationConstants.get(name);
                assertThat(isolationLevel).isEqualTo(expected);
                uniqueValues.add(isolationLevel);
              });
      assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(DefaultTransactionDefinition.isolationConstants.values());
    }

    @Test
    void setIsolationLevel() {
      assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevel(999));

      template.setIsolationLevel(ISOLATION_REPEATABLE_READ);
      assertThat(template.getIsolationLevel()).isEqualTo(ISOLATION_REPEATABLE_READ);
    }

    @Test
    void getDefinitionDescription() {
      assertThat(template.getDefinitionDescription()).asString()
              .isEqualTo("PROPAGATION_REQUIRED,ISOLATION_DEFAULT");

      template.setPropagationBehavior(PROPAGATION_MANDATORY);
      template.setIsolationLevel(ISOLATION_REPEATABLE_READ);
      template.setReadOnly(true);
      template.setTimeout(42);
      assertThat(template.getDefinitionDescription()).asString()
              .isEqualTo("PROPAGATION_MANDATORY,ISOLATION_REPEATABLE_READ,timeout_42,readOnly");
    }

    private static Stream<String> streamPropagationConstants() {
      return streamTransactionDefinitionConstants()
              .filter(name -> name.startsWith(PREFIX_PROPAGATION));
    }

    private static Stream<String> streamIsolationConstants() {
      return streamTransactionDefinitionConstants()
              .filter(name -> name.startsWith(PREFIX_ISOLATION));
    }

    private static Stream<String> streamTransactionDefinitionConstants() {
      return Arrays.stream(TransactionDefinition.class.getFields())
              .filter(ReflectionUtils::isPublicStaticFinal)
              .map(Field::getName);
    }
  }

}
