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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;
import infra.transaction.TransactionStatus;
import infra.transaction.TransactionSystemException;
import infra.util.ExceptionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:35
 */
class TransactionTemplateTests {

  @Test
  void shouldConstructTransactionTemplateWithDefaultSettings() {
    TransactionTemplate template = new TransactionTemplate();
    assertThat(template.getTransactionManager()).isNull();
  }

  @Test
  void shouldConstructTransactionTemplateWithTransactionManager() {
    PlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    assertThat(template.getTransactionManager()).isSameAs(transactionManager);
  }

  @Test
  void shouldConstructTransactionTemplateWithTransactionManagerAndDefinition() {
    PlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionDefinition definition = TransactionDefinition.forReadOnly();
    TransactionTemplate template = new TransactionTemplate(transactionManager, definition);

    assertThat(template.getTransactionManager()).isSameAs(transactionManager);
    assertThat(template.isReadOnly()).isTrue();
  }

  @Test
  void shouldSetAndGetTransactionManager() {
    TransactionTemplate template = new TransactionTemplate();
    PlatformTransactionManager transactionManager = new MockPlatformTransactionManager();

    template.setTransactionManager(transactionManager);
    assertThat(template.getTransactionManager()).isSameAs(transactionManager);
  }

  @Test
  void shouldThrowExceptionWhenInitializingWithoutTransactionManager() {
    TransactionTemplate template = new TransactionTemplate();

    assertThatThrownBy(template::afterPropertiesSet)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Property 'transactionManager' is required");
  }

  @Test
  void shouldPassInitializationWhenTransactionManagerIsSet() {
    TransactionTemplate template = new TransactionTemplate();
    template.setTransactionManager(new MockPlatformTransactionManager());

    assertThatNoException().isThrownBy(template::afterPropertiesSet);
  }

  @Test
  void shouldExecuteTransactionCallbackAndReturnResult() {
    PlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    String expectedResult = "result";

    String result = template.execute(status -> expectedResult);

    assertThat(result).isEqualTo(expectedResult);
  }

  @Test
  void shouldExecuteTransactionCallbackWithoutResult() {
    PlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    AtomicBoolean executed = new AtomicBoolean(false);

    template.executeWithoutResult(status -> executed.set(true));

    assertThat(executed.get()).isTrue();
  }

  @Test
  void shouldExecuteTransactionCallbackWithCustomDefinition() {
    PlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    TransactionDefinition customDefinition = TransactionDefinition.forTimeout(30);
    String expectedResult = "result";

    String result = template.execute(action -> expectedResult, customDefinition);

    assertThat(result).isEqualTo(expectedResult);
  }

  @Test
  void shouldExecuteTransactionCallbackWithoutResultWithCustomDefinition() {
    PlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    TransactionDefinition customDefinition = TransactionDefinition.forReadOnly();
    AtomicBoolean executed = new AtomicBoolean(false);

    template.executeWithoutResult(action -> executed.set(true), customDefinition);

    assertThat(executed.get()).isTrue();
  }

  @Test
  void shouldRollbackTransactionOnRuntimeException() {
    MockPlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    RuntimeException expectedException = new RuntimeException("test exception");

    assertThatThrownBy(() -> template.execute(status -> {
      throw expectedException;
    })).isSameAs(expectedException);

    assertThat(transactionManager.rollbackCount).isEqualTo(1);
    assertThat(transactionManager.commitCount).isEqualTo(0);
  }

  @Test
  void shouldRollbackTransactionOnError() {
    MockPlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    Error expectedError = new Error("test error");

    assertThatThrownBy(() -> template.execute(status -> {
      throw expectedError;
    })).isSameAs(expectedError);

    assertThat(transactionManager.rollbackCount).isEqualTo(1);
    assertThat(transactionManager.commitCount).isEqualTo(0);
  }

  @Test
  void shouldCommitTransactionOnSuccess() {
    MockPlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    String expectedResult = "result";

    String result = template.execute(status -> expectedResult);

    assertThat(result).isEqualTo(expectedResult);
    assertThat(transactionManager.commitCount).isEqualTo(1);
    assertThat(transactionManager.rollbackCount).isEqualTo(0);
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenTransactionManagerIsNull() {
    TransactionTemplate template = new TransactionTemplate();

    assertThatThrownBy(() -> template.execute(status -> "result"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No PlatformTransactionManager set");
  }

  @Test
  void shouldDelegateToCallbackPreferringTransactionManager() {
    MockCallbackPreferringPlatformTransactionManager transactionManager =
            new MockCallbackPreferringPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    String expectedResult = "result";

    String result = template.execute(status -> expectedResult);

    assertThat(result).isEqualTo(expectedResult);
    assertThat(transactionManager.executeCalled).isTrue();
  }

  @Test
  void shouldHandleCheckedExceptionInCallback() {
    MockPlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);

    assertThatThrownBy(() -> template.execute(status -> {
      throw ExceptionUtils.sneakyThrow(new Exception("checked exception"));
    })).isInstanceOf(UndeclaredThrowableException.class);

    assertThat(transactionManager.rollbackCount).isEqualTo(1);
  }

  @Test
  void shouldHandleTransactionSystemExceptionDuringRollback() {
    MockPlatformTransactionManager transactionManager = new MockPlatformTransactionManager() {
      @Override
      public void rollback(TransactionStatus status) throws TransactionException {
        rollbackCount++;
        throw new TransactionSystemException("rollback failed");
      }
    };
    TransactionTemplate template = new TransactionTemplate(transactionManager);

    assertThatThrownBy(() -> template.execute(status -> {
      throw new RuntimeException("application exception");
    })).isInstanceOf(TransactionSystemException.class)
            .hasMessage("rollback failed");

    assertThat(transactionManager.rollbackCount).isEqualTo(1);
  }

  @Test
  void shouldHandleRuntimeExceptionDuringRollback() {
    MockPlatformTransactionManager transactionManager = new MockPlatformTransactionManager() {
      @Override
      public void rollback(TransactionStatus status) throws TransactionException {
        rollbackCount++;
        throw new RuntimeException("rollback failed");
      }
    };
    TransactionTemplate template = new TransactionTemplate(transactionManager);

    assertThatThrownBy(() -> template.execute(status -> {
      throw new RuntimeException("application exception");
    })).isInstanceOf(RuntimeException.class)
            .hasMessage("rollback failed");

    assertThat(transactionManager.rollbackCount).isEqualTo(1);
  }

  @Test
  void shouldUseDefaultDefinitionWhenNullDefinitionProvided() {
    MockPlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setReadOnly(true);

    String result = template.execute(action -> "result", null);

    assertThat(result).isEqualTo("result");
    // Execution should succeed with default definition
  }

  @Test
  void shouldExecuteWithTransactionTemplateAsDefinition() {
    MockPlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setReadOnly(true);
    template.setTimeout(30);

    String result = template.execute(action -> "result", template);

    assertThat(result).isEqualTo("result");
  }

  @Test
  void shouldEqualsReturnTrueForSameInstance() {
    TransactionTemplate template = new TransactionTemplate();

    assertThat(template.equals(template)).isTrue();
  }

  @Test
  void shouldEqualsReturnTrueForEqualTemplates() {
    PlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template1 = new TransactionTemplate(transactionManager);
    TransactionTemplate template2 = new TransactionTemplate(transactionManager);

    assertThat(template1.equals(template2)).isTrue();
  }

  @Test
  void shouldEqualsReturnFalseForDifferentTransactionManagers() {
    PlatformTransactionManager transactionManager1 = new MockPlatformTransactionManager();
    PlatformTransactionManager transactionManager2 = new MockPlatformTransactionManager();
    TransactionTemplate template1 = new TransactionTemplate(transactionManager1);
    TransactionTemplate template2 = new TransactionTemplate(transactionManager2);

    assertThat(template1.equals(template2)).isFalse();
  }

  @Test
  void shouldEqualsReturnFalseForDifferentDefinitionProperties() {
    PlatformTransactionManager transactionManager = new MockPlatformTransactionManager();
    TransactionTemplate template1 = new TransactionTemplate(transactionManager);
    TransactionTemplate template2 = new TransactionTemplate(transactionManager);
    template2.setReadOnly(true);

    assertThat(template1.equals(template2)).isFalse();
  }

  @Test
  void shouldHandleNullInEquals() {
    TransactionTemplate template = new TransactionTemplate();

    assertThat(template.equals(null)).isFalse();
  }

  static class MockPlatformTransactionManager implements PlatformTransactionManager {
    int commitCount = 0;
    int rollbackCount = 0;

    @Override
    public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
      return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
      commitCount++;
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
      rollbackCount++;
    }
  }

  static class MockCallbackPreferringPlatformTransactionManager implements CallbackPreferringPlatformTransactionManager {
    boolean executeCalled = false;

    @Override
    public <T> T execute(TransactionDefinition definition, TransactionCallback<T> callback) throws TransactionException {
      executeCalled = true;
      return callback.doInTransaction(new SimpleTransactionStatus());
    }

    @Override
    public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
      return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
    }
  }

}