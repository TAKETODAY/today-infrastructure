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
 * @since 5.0 2025/10/12 21:36
 */
class SimpleTransactionFactoryTests {

  @Test
  void constructorWithTransactionManager() {
    TransactionManager transactionManager = new MockTransactionManager();
    SimpleTransactionFactory factory = new SimpleTransactionFactory(transactionManager);

    assertThat(factory).isNotNull();
  }

  @Test
  void constructorWithNullTransactionManagerThrowsException() {
    assertThatThrownBy(() -> new SimpleTransactionFactory(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TransactionManager is required");
  }

  @Test
  void createTransactionWithValidParameters() throws NotSupportedException, SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    SimpleTransactionFactory factory = new SimpleTransactionFactory(transactionManager);

    Transaction transaction = factory.createTransaction("testTransaction", 30);

    assertThat(transaction).isNotNull();
    assertThat(transaction).isInstanceOf(ManagedTransactionAdapter.class);
    assertThat(transactionManager.setTransactionTimeoutCalled).isTrue();
    assertThat(transactionManager.beginCalled).isTrue();
  }

  @Test
  void createTransactionWithNegativeTimeout() throws NotSupportedException, SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    SimpleTransactionFactory factory = new SimpleTransactionFactory(transactionManager);

    Transaction transaction = factory.createTransaction("testTransaction", -1);

    assertThat(transaction).isNotNull();
    assertThat(transactionManager.setTransactionTimeoutCalled).isFalse();
    assertThat(transactionManager.beginCalled).isTrue();
  }

  @Test
  void createTransactionWithZeroTimeout() throws NotSupportedException, SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    SimpleTransactionFactory factory = new SimpleTransactionFactory(transactionManager);

    Transaction transaction = factory.createTransaction("testTransaction", 0);

    assertThat(transaction).isNotNull();
    assertThat(transactionManager.setTransactionTimeoutCalled).isTrue();
    assertThat(transactionManager.beginCalled).isTrue();
  }

  @Test
  void supportsResourceAdapterManagedTransactionsReturnsFalse() {
    TransactionManager transactionManager = new MockTransactionManager();
    SimpleTransactionFactory factory = new SimpleTransactionFactory(transactionManager);

    boolean supports = factory.supportsResourceAdapterManagedTransactions();

    assertThat(supports).isFalse();
  }

  static class MockTransactionManager implements TransactionManager {
    boolean setTransactionTimeoutCalled = false;
    boolean beginCalled = false;

    @Override
    public void begin() throws NotSupportedException, SystemException {
      beginCalled = true;
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
    }

    @Override
    public int getStatus() throws SystemException {
      return 0;
    }

    @Override
    public Transaction getTransaction() throws SystemException {
      return null;
    }

    @Override
    public Transaction suspend() throws SystemException {
      return null;
    }

    @Override
    public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
      setTransactionTimeoutCalled = true;
    }
  }

}