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

import org.junit.jupiter.api.Test;

import infra.aop.scope.ScopedObject;
import infra.core.InfraProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:35
 */
class TransactionSynchronizationUtilsTests {

  @Test
  void shouldUnwrapInfraProxyToWrappedObject() {
    Object targetObject = new Object();
    InfraProxy proxy = new InfraProxy() {
      @Override
      public Object getWrappedObject() {
        return targetObject;
      }
    };

    Object result = TransactionSynchronizationUtils.unwrapResourceIfNecessary(proxy);
    assertThat(result).isSameAs(targetObject);
  }

  @Test
  void shouldReturnResourceAsIsWhenNotInfraProxy() {
    Object resource = new Object();

    Object result = TransactionSynchronizationUtils.unwrapResourceIfNecessary(resource);
    assertThat(result).isSameAs(resource);
  }

  @Test
  void shouldUnwrapScopedObjectWhenAopAvailable() {
    Object targetObject = new Object();
    ScopedObject scopedObject = new ScopedObject() {
      @Override
      public Object getTargetObject() {
        return targetObject;
      }

      @Override
      public void removeFromScope() {

      }
    };

    Object result = TransactionSynchronizationUtils.unwrapResourceIfNecessary(scopedObject);
    assertThat(result).isSameAs(targetObject);
  }

  @Test
  void shouldNotUnwrapNonScopedObjectWhenAopAvailable() {
    Object resource = new Object();

    Object result = TransactionSynchronizationUtils.unwrapResourceIfNecessary(resource);
    assertThat(result).isSameAs(resource);
  }

  @Test
  void shouldThrowExceptionWhenResourceIsNull() {
    assertThatThrownBy(() -> TransactionSynchronizationUtils.unwrapResourceIfNecessary(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Resource is required");
  }

  @Test
  void shouldInvokeAfterCommitOnAllSynchronizations() {
    TestTransactionSynchronization sync1 = new TestTransactionSynchronization();
    TestTransactionSynchronization sync2 = new TestTransactionSynchronization();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      TransactionSynchronizationUtils.invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());

      assertThat(sync1.afterCommitCalled).isTrue();
      assertThat(sync2.afterCommitCalled).isTrue();
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldHandleNullSynchronizationsListInInvokeAfterCommit() {
    assertThatNoException().isThrownBy(() ->
            TransactionSynchronizationUtils.invokeAfterCommit(null));
  }

  @Test
  void shouldInvokeAfterCompletionOnAllSynchronizations() {
    TestTransactionSynchronization sync1 = new TestTransactionSynchronization();
    TestTransactionSynchronization sync2 = new TestTransactionSynchronization();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      TransactionSynchronizationUtils.invokeAfterCompletion(
              TransactionSynchronizationManager.getSynchronizations(),
              TransactionSynchronization.STATUS_COMMITTED);

      assertThat(sync1.afterCompletionCalled).isTrue();
      assertThat(sync1.completionStatus).isEqualTo(TransactionSynchronization.STATUS_COMMITTED);
      assertThat(sync2.afterCompletionCalled).isTrue();
      assertThat(sync2.completionStatus).isEqualTo(TransactionSynchronization.STATUS_COMMITTED);
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldHandleExceptionInAfterCompletionGracefully() {
    TestTransactionSynchronization goodSync = new TestTransactionSynchronization();
    TestTransactionSynchronization badSync = new TestTransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        super.afterCompletion(status);
        throw new RuntimeException("Test exception");
      }
    };

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(badSync);
      TransactionSynchronizationManager.registerSynchronization(goodSync);

      assertThatNoException().isThrownBy(() ->
              TransactionSynchronizationUtils.invokeAfterCompletion(
                      TransactionSynchronizationManager.getSynchronizations(),
                      TransactionSynchronization.STATUS_COMMITTED));

      assertThat(badSync.afterCompletionCalled).isTrue();
      assertThat(goodSync.afterCompletionCalled).isTrue();
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldHandleNullSynchronizationsListInInvokeAfterCompletion() {
    assertThatNoException().isThrownBy(() ->
            TransactionSynchronizationUtils.invokeAfterCompletion(null, TransactionSynchronization.STATUS_COMMITTED));
  }

  @Test
  void shouldTriggerFlushOnAllSynchronizations() {
    TestTransactionSynchronization sync1 = new TestTransactionSynchronization();
    TestTransactionSynchronization sync2 = new TestTransactionSynchronization();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      TransactionSynchronizationUtils.triggerFlush();

      assertThat(sync1.flushCalled).isTrue();
      assertThat(sync2.flushCalled).isTrue();
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldTriggerBeforeCommitOnAllSynchronizations() {
    TestTransactionSynchronization sync1 = new TestTransactionSynchronization();
    TestTransactionSynchronization sync2 = new TestTransactionSynchronization();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      boolean readOnly = true;
      TransactionSynchronizationUtils.triggerBeforeCommit(readOnly);

      assertThat(sync1.beforeCommitCalled).isTrue();
      assertThat(sync1.beforeCommitReadOnly).isEqualTo(readOnly);
      assertThat(sync2.beforeCommitCalled).isTrue();
      assertThat(sync2.beforeCommitReadOnly).isEqualTo(readOnly);
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldTriggerBeforeCompletionOnAllSynchronizations() {
    TestTransactionSynchronization sync1 = new TestTransactionSynchronization();
    TestTransactionSynchronization sync2 = new TestTransactionSynchronization();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      TransactionSynchronizationUtils.triggerBeforeCompletion();

      assertThat(sync1.beforeCompletionCalled).isTrue();
      assertThat(sync2.beforeCompletionCalled).isTrue();
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldTriggerAfterCommitThroughTriggerMethod() {
    TestTransactionSynchronization sync1 = new TestTransactionSynchronization();
    TestTransactionSynchronization sync2 = new TestTransactionSynchronization();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      TransactionSynchronizationUtils.triggerAfterCommit();

      assertThat(sync1.afterCommitCalled).isTrue();
      assertThat(sync2.afterCommitCalled).isTrue();
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldTriggerAfterCompletionThroughTriggerMethod() {
    TestTransactionSynchronization sync1 = new TestTransactionSynchronization();
    TestTransactionSynchronization sync2 = new TestTransactionSynchronization();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

      assertThat(sync1.afterCompletionCalled).isTrue();
      assertThat(sync1.completionStatus).isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
      assertThat(sync2.afterCompletionCalled).isTrue();
      assertThat(sync2.completionStatus).isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldTriggerSavepointOnAllSynchronizations() {
    TestTransactionSynchronization sync1 = new TestTransactionSynchronization();
    TestTransactionSynchronization sync2 = new TestTransactionSynchronization();
    Object savepoint = new Object();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      TransactionSynchronizationUtils.triggerSavepoint(savepoint);

      assertThat(sync1.savepointCalled).isTrue();
      assertThat(sync1.savepointObject).isSameAs(savepoint);
      assertThat(sync2.savepointCalled).isTrue();
      assertThat(sync2.savepointObject).isSameAs(savepoint);
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void shouldTriggerSavepointRollbackOnAllSynchronizations() {
    TestTransactionSynchronization sync1 = new TestTransactionSynchronization();
    TestTransactionSynchronization sync2 = new TestTransactionSynchronization();
    Object savepoint = new Object();

    TransactionSynchronizationManager.initSynchronization();
    try {
      TransactionSynchronizationManager.registerSynchronization(sync1);
      TransactionSynchronizationManager.registerSynchronization(sync2);

      TransactionSynchronizationUtils.triggerSavepointRollback(savepoint);

      assertThat(sync1.savepointRollbackCalled).isTrue();
      assertThat(sync1.savepointRollbackObject).isSameAs(savepoint);
      assertThat(sync2.savepointRollbackCalled).isTrue();
      assertThat(sync2.savepointRollbackObject).isSameAs(savepoint);
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  static class TestTransactionSynchronization implements TransactionSynchronization {
    boolean flushCalled = false;
    boolean beforeCommitCalled = false;
    boolean beforeCommitReadOnly = false;
    boolean beforeCompletionCalled = false;
    boolean afterCommitCalled = false;
    boolean afterCompletionCalled = false;
    boolean savepointCalled = false;
    boolean savepointRollbackCalled = false;
    Object savepointObject = null;
    Object savepointRollbackObject = null;
    int completionStatus = -1;

    @Override
    public void suspend() { }

    @Override
    public void resume() { }

    @Override
    public void flush() {
      flushCalled = true;
    }

    @Override
    public void beforeCommit(boolean readOnly) {
      beforeCommitCalled = true;
      beforeCommitReadOnly = readOnly;
    }

    @Override
    public void beforeCompletion() {
      beforeCompletionCalled = true;
    }

    @Override
    public void afterCommit() {
      afterCommitCalled = true;
    }

    @Override
    public void afterCompletion(int status) {
      afterCompletionCalled = true;
      completionStatus = status;
    }

    @Override
    public void savepoint(Object savepoint) {
      savepointCalled = true;
      savepointObject = savepoint;
    }

    @Override
    public void savepointRollback(Object savepoint) {
      savepointRollbackCalled = true;
      savepointRollbackObject = savepoint;
    }
  }

}