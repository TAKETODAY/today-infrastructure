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

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:40
 */
class UserTransactionAdapterTests {

  @Test
  void constructorWithTransactionManager() {
    TransactionManager transactionManager = new MockTransactionManager();
    UserTransactionAdapter adapter = new UserTransactionAdapter(transactionManager);

    assertThat(adapter).isNotNull();
    assertThat(adapter.getTransactionManager()).isEqualTo(transactionManager);
  }

  @Test
  void constructorWithNullTransactionManagerThrowsException() {
    assertThatThrownBy(() -> new UserTransactionAdapter(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TransactionManager is required");
  }

  @Test
  void setTransactionTimeoutDelegatesToTransactionManager() throws SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    UserTransactionAdapter adapter = new UserTransactionAdapter(transactionManager);

    adapter.setTransactionTimeout(30);

    assertThat(transactionManager.setTransactionTimeoutCalled).isTrue();
    assertThat(transactionManager.timeoutValue).isEqualTo(30);
  }

  @Test
  void beginDelegatesToTransactionManager() throws NotSupportedException, SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    UserTransactionAdapter adapter = new UserTransactionAdapter(transactionManager);

    adapter.begin();

    assertThat(transactionManager.beginCalled).isTrue();
  }

  @Test
  void commitDelegatesToTransactionManager() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    UserTransactionAdapter adapter = new UserTransactionAdapter(transactionManager);

    adapter.commit();

    assertThat(transactionManager.commitCalled).isTrue();
  }

  @Test
  void rollbackDelegatesToTransactionManager() throws SecurityException, SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    UserTransactionAdapter adapter = new UserTransactionAdapter(transactionManager);

    adapter.rollback();

    assertThat(transactionManager.rollbackCalled).isTrue();
  }

  @Test
  void setRollbackOnlyDelegatesToTransactionManager() throws SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    UserTransactionAdapter adapter = new UserTransactionAdapter(transactionManager);

    adapter.setRollbackOnly();

    assertThat(transactionManager.setRollbackOnlyCalled).isTrue();
  }

  @Test
  void getStatusDelegatesToTransactionManager() throws SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    UserTransactionAdapter adapter = new UserTransactionAdapter(transactionManager);

    int status = adapter.getStatus();

    assertThat(status).isEqualTo(MockTransactionManager.MOCK_STATUS);
    assertThat(transactionManager.getStatusCalled).isTrue();
  }

  static class MockTransactionManager implements TransactionManager {
    static final int MOCK_STATUS = 1;

    boolean setTransactionTimeoutCalled = false;
    boolean beginCalled = false;
    boolean commitCalled = false;
    boolean rollbackCalled = false;
    boolean setRollbackOnlyCalled = false;
    boolean getStatusCalled = false;
    int timeoutValue = -1;

    @Override
    public void begin() throws NotSupportedException, SystemException {
      beginCalled = true;
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
      commitCalled = true;
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
      rollbackCalled = true;
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
      setRollbackOnlyCalled = true;
    }

    @Override
    public int getStatus() throws SystemException {
      getStatusCalled = true;
      return MOCK_STATUS;
    }

    @Override
    public Transaction getTransaction() throws SystemException {
      return null;
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
      setTransactionTimeoutCalled = true;
      timeoutValue = seconds;
    }

    @Override
    public Transaction suspend() throws SystemException {
      return null;
    }

    @Override
    public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
    }
  }

}