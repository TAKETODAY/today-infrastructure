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
import infra.transaction.MockCallbackPreferringTransactionManager;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TestTransactionExecutionListener;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionStatus;
import infra.transaction.TransactionSystemException;
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

/**
 * @author Juergen Hoeller
 */
class TransactionSupportTests {

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
