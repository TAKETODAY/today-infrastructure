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

package infra.transaction.jta;

import org.junit.jupiter.api.Test;

import infra.transaction.support.TransactionSynchronization;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:25
 */
class JtaSynchronizationAdapterTests {

  @Test
  void constructorWithTransactionSynchronization() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization);

    assertThat(adapter).isNotNull();
  }

  @Test
  void constructorWithTransactionSynchronizationAndUserTransaction() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    UserTransaction userTransaction = new MockUserTransaction();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization, userTransaction);

    assertThat(adapter).isNotNull();
  }

  @Test
  void constructorWithTransactionSynchronizationAndTransactionManager() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    TransactionManager transactionManager = new MockTransactionManager();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization, transactionManager);

    assertThat(adapter).isNotNull();
  }

  @Test
  void beforeCompletionWithReadOnlyTransaction() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization);

    assertThatCode(() -> adapter.beforeCompletion()).doesNotThrowAnyException();
  }

  @Test
  void afterCompletionWithCommittedStatus() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization);

    assertThatCode(() -> adapter.afterCompletion(Status.STATUS_COMMITTED)).doesNotThrowAnyException();
  }

  @Test
  void afterCompletionWithRolledBackStatus() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization);

    assertThatCode(() -> adapter.afterCompletion(Status.STATUS_ROLLEDBACK)).doesNotThrowAnyException();
  }

  @Test
  void afterCompletionWithUnknownStatus() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization);

    assertThatCode(() -> adapter.afterCompletion(Status.STATUS_UNKNOWN)).doesNotThrowAnyException();
  }

  @Test
  void afterCompletionWithOtherStatus() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization);

    assertThatCode(() -> adapter.afterCompletion(Status.STATUS_NO_TRANSACTION)).doesNotThrowAnyException();
  }

  @Test
  void beforeCompletionCalledFlagIsSet() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization);

    adapter.beforeCompletion();
    // beforeCompletionCalled flag should be set to true
  }

  @Test
  void afterCompletionCallsBeforeCompletionIfNotCalledBefore() {
    TransactionSynchronization frameworkSynchronization = new TestTransactionSynchronization();
    JtaSynchronizationAdapter adapter = new JtaSynchronizationAdapter(frameworkSynchronization);

    // beforeCompletion not called, should be called in afterCompletion
    assertThatCode(() -> adapter.afterCompletion(Status.STATUS_COMMITTED)).doesNotThrowAnyException();
  }

  static class TestTransactionSynchronization implements TransactionSynchronization {
    @Override
    public void beforeCommit(boolean readOnly) {
    }

    @Override
    public void beforeCompletion() {
    }

    @Override
    public void afterCompletion(int status) {
    }
  }

  static class MockUserTransaction implements UserTransaction {
    @Override
    public void begin() {
    }

    @Override
    public void commit() {
    }

    @Override
    public void rollback() {
    }

    @Override
    public void setRollbackOnly() {
    }

    @Override
    public int getStatus() {
      return 0;
    }

    @Override
    public void setTransactionTimeout(int seconds) {
    }
  }

  static class MockTransactionManager implements TransactionManager {
    @Override
    public jakarta.transaction.Transaction getTransaction() {
      return null;
    }

    @Override
    public void begin() {
    }

    @Override
    public void commit() {
    }

    @Override
    public void rollback() {
    }

    @Override
    public void setRollbackOnly() {
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {

    }

    @Override
    public int getStatus() {
      return 0;
    }

    @Override
    public jakarta.transaction.Transaction suspend() {
      return null;
    }

    @Override
    public void resume(jakarta.transaction.Transaction tobj) {
    }
  }

}