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

package infra.test.context.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import infra.context.support.GenericApplicationContext;
import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.test.annotation.Rollback;
import infra.test.context.TestContext;
import infra.test.context.MethodInvoker;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionDefinition;
import infra.transaction.annotation.Propagation;
import infra.transaction.annotation.Transactional;
import infra.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TransactionalListenerLifecycleTests {

  private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);

  private final TransactionalTestExecutionListener listener = new TransactionalTestExecutionListener() {
    @Override
    protected PlatformTransactionManager getTransactionManager(TestContext context, String qualifier) {
      return manager;
    }
  };

  @AfterEach
  void clearTransaction() {
    TransactionContextHolder.removeCurrentTransactionContext();
  }

  @Test
  void endsActiveTransactionAndRejectsOverlappingTransactions() throws Exception {
    Callbacks callbacks = new Callbacks();
    TestContext context = context(callbacks, "test");
    SimpleTransactionStatus status = new SimpleTransactionStatus();
    given(manager.getTransaction(any(TransactionDefinition.class))).willReturn(status);
    listener.beforeTestMethod(context);
    assertThat(TestTransaction.isActive()).isTrue();
    assertThat(callbacks.before).isEqualTo(1);
    listener.afterTestMethod(context);
    verify(manager).rollback(status);
    assertThat(callbacks.after).isEqualTo(1);
    assertThat(TestTransaction.isActive()).isFalse();
    listener.beforeTestMethod(context);
    assertThatIllegalStateException().isThrownBy(() -> listener.beforeTestMethod(context))
            .withMessageContaining("Cannot start new transaction");
  }

  @Test
  void afterCallbacksRunEvenWhenRollbackFails() throws Exception {
    Callbacks callbacks = new Callbacks();
    TestContext context = context(callbacks, "test");
    SimpleTransactionStatus status = new SimpleTransactionStatus();
    given(manager.getTransaction(any(TransactionDefinition.class))).willReturn(status);
    IllegalStateException failure = new IllegalStateException("rollback failed");
    willThrow(failure).given(manager).rollback(status);
    listener.beforeTestMethod(context);
    assertThatThrownBy(() -> listener.afterTestMethod(context)).isSameAs(failure);
    assertThat(callbacks.after).isEqualTo(1);
    assertThat(TestTransaction.isActive()).isFalse();
  }

  @Test
  void completedTransactionIsNotEndedTwice() throws Exception {
    Callbacks callbacks = new Callbacks();
    TestContext context = context(callbacks, "test");
    SimpleTransactionStatus status = new SimpleTransactionStatus();
    given(manager.getTransaction(any(TransactionDefinition.class))).willReturn(status);
    listener.beforeTestMethod(context);
    status.setCompleted();
    listener.afterTestMethod(context);
    assertThat(callbacks.after).isEqualTo(1);
    org.mockito.Mockito.verify(manager, org.mockito.Mockito.never()).rollback(status);
  }

  @Test
  void beforeFailurePreventsTransactionAndAfterFailuresStillRunAllCallbacks() throws Exception {
    FailingCallbacks callbacks = new FailingCallbacks();
    TestContext context = context(callbacks, "test");
    assertThatThrownBy(() -> listener.runBeforeTransactionMethods(context))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("before failed");
    verifyNoInteractions(manager);
    assertThatThrownBy(() -> listener.runAfterTransactionMethods(context))
            .isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("after failed");
    assertThat(callbacks.after).isEqualTo(2);
  }

  @Test
  void reflectiveInvocationFailuresAreUnwrappedAndAllAfterCallbacksRun() throws Exception {
    FailingCallbacks callbacks = new FailingCallbacks();
    TestContext context = context(callbacks, "test");
    MethodInvoker invoker = (method, target) -> {
      method.setAccessible(true);
      return method.invoke(target);
    };
    given(context.getMethodInvoker()).willReturn(invoker);
    assertThatThrownBy(() -> listener.runBeforeTransactionMethods(context))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("before failed");
    assertThatThrownBy(() -> listener.runAfterTransactionMethods(context))
            .isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("after failed");
    assertThat(callbacks.after).isEqualTo(2);
  }

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "TRACE", "DEBUG", "OFF" })
  void transactionLifecycleAndRollbackOverridesAreIndependentOfLogging(LogLevel level) throws Exception {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = TransactionalTestExecutionListener.class.getName();
    LogLevel previous = logging.getLoggerConfiguration(name).getConfiguredLevel();
    logging.setLogLevel(name, level);
    try {
      Callbacks callbacks = new Callbacks();
      TestContext context = context(callbacks, "test");
      given(manager.getTransaction(any(TransactionDefinition.class))).willReturn(new SimpleTransactionStatus());
      listener.beforeTestMethod(context);
      listener.afterTestMethod(context);
      assertThat(callbacks.before).isEqualTo(1);
      assertThat(callbacks.after).isEqualTo(1);
      assertThat(listener.isRollback(context)).isTrue();
      assertThat(listener.isRollback(context(new RollbackCallbacks(), "test"))).isFalse();
      reflectiveInvocationFailuresAreUnwrappedAndAllAfterCallbacksRun();
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

  @Test
  void neverPropagationSkipsTransactionAndNullMethodsAreRejected() throws Exception {
    TestContext context = context(new Callbacks(), "never");
    listener.beforeTestMethod(context);
    listener.afterTestMethod(context);
    verifyNoInteractions(manager);
    given(context.getTestMethod()).willReturn(null);
    assertThatThrownBy(() -> listener.beforeTestMethod(context)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> listener.afterTestMethod(context)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void qualifiedTransactionManagerIsResolvedAndMissingQualifierFails() {
    TransactionalTestExecutionListener standard = new TransactionalTestExecutionListener();
    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {
      applicationContext.registerBean("qualified", PlatformTransactionManager.class, () -> manager);
      applicationContext.refresh();
      TestContext context = mock(TestContext.class);
      given(context.getApplicationContext()).willReturn(applicationContext);
      assertThat(standard.getTransactionManager(context, "qualified")).isSameAs(manager);
      assertThat(standard.getTransactionManager(context, " ")).isSameAs(manager);
      assertThatThrownBy(() -> standard.getTransactionManager(context, "missing"))
              .isInstanceOf(infra.beans.factory.NoSuchBeanDefinitionException.class);
    }
  }

  private TestContext context(Object instance, String method) throws Exception {
    TestContext context = mock(TestContext.class, CALLS_REAL_METHODS);
    org.mockito.BDDMockito.<Class<?>>given(context.getTestClass()).willReturn(instance.getClass());
    given(context.getTestInstance()).willReturn(instance);
    given(context.getTestMethod()).willReturn(instance.getClass().getDeclaredMethod(method));
    return context;
  }

  static class Callbacks {
    int before;
    int after;

    @BeforeTransaction
    void before() {
      before++;
    }

    @AfterTransaction
    void after() {
      after++;
    }

    @Transactional
    void test() { }

    @Transactional(propagation = Propagation.NEVER)
    void never() { }
  }

  static class FailingCallbacks {
    int after;

    @BeforeTransaction
    void before() {
      throw new IllegalArgumentException("before failed");
    }

    @AfterTransaction
    void afterOne() {
      after++;
      throw new IllegalArgumentException("after failed one");
    }

    @AfterTransaction
    void afterTwo() {
      after++;
      throw new IllegalArgumentException("after failed two");
    }

    void test() { }
  }

  @Rollback
  static class RollbackCallbacks {
    @Rollback(false)
    void test() { }
  }

}
