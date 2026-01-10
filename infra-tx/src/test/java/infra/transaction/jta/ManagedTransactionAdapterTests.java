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

package infra.transaction.jta;

import org.junit.jupiter.api.Test;

import javax.transaction.xa.XAResource;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:33
 */
class ManagedTransactionAdapterTests {

  @Test
  void constructorWithTransactionManager() throws SystemException {
    TransactionManager transactionManager = new MockTransactionManager();
    ManagedTransactionAdapter adapter = new ManagedTransactionAdapter(transactionManager);

    assertThat(adapter).isNotNull();
    assertThat(adapter.getTransactionManager()).isEqualTo(transactionManager);
  }

  @Test
  void constructorWithNullTransactionManagerThrowsException() {
    assertThatThrownBy(() -> new ManagedTransactionAdapter(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TransactionManager is required");
  }

  @Test
  void commitDelegatesToTransactionManager() throws SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    ManagedTransactionAdapter adapter = new ManagedTransactionAdapter(transactionManager);

    adapter.commit();

    assertThat(transactionManager.commitCalled).isTrue();
  }

  @Test
  void rollbackDelegatesToTransactionManager() throws SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    ManagedTransactionAdapter adapter = new ManagedTransactionAdapter(transactionManager);

    adapter.rollback();

    assertThat(transactionManager.rollbackCalled).isTrue();
  }

  @Test
  void setRollbackOnlyDelegatesToTransactionManager() throws SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    ManagedTransactionAdapter adapter = new ManagedTransactionAdapter(transactionManager);

    adapter.setRollbackOnly();

    assertThat(transactionManager.setRollbackOnlyCalled).isTrue();
  }

  @Test
  void getStatusDelegatesToTransactionManager() throws SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    ManagedTransactionAdapter adapter = new ManagedTransactionAdapter(transactionManager);

    int status = adapter.getStatus();

    assertThat(status).isEqualTo(TransactionManagerStatusMock.STATUS);
    assertThat(transactionManager.getStatusCalled).isTrue();
  }

  @Test
  void enlistResourceDelegatesToTransaction() throws SystemException, RollbackException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    MockTransaction transaction = new MockTransaction();
    transactionManager.setTransaction(transaction);
    ManagedTransactionAdapter adapter = new ManagedTransactionAdapter(transactionManager);

    XAResource xaResource = new MockXAResource();
    boolean result = adapter.enlistResource(xaResource);

    assertThat(result).isTrue();
    assertThat(transaction.enlistResourceCalled).isTrue();
  }

  @Test
  void delistResourceDelegatesToTransaction() throws SystemException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    MockTransaction transaction = new MockTransaction();
    transactionManager.setTransaction(transaction);
    ManagedTransactionAdapter adapter = new ManagedTransactionAdapter(transactionManager);

    XAResource xaResource = new MockXAResource();
    boolean result = adapter.delistResource(xaResource, 0);

    assertThat(result).isTrue();
    assertThat(transaction.delistResourceCalled).isTrue();
  }

  @Test
  void registerSynchronizationDelegatesToTransaction() throws SystemException, RollbackException {
    MockTransactionManager transactionManager = new MockTransactionManager();
    MockTransaction transaction = new MockTransaction();
    transactionManager.setTransaction(transaction);
    ManagedTransactionAdapter adapter = new ManagedTransactionAdapter(transactionManager);

    Synchronization synchronization = new MockSynchronization();
    adapter.registerSynchronization(synchronization);

    assertThat(transaction.registerSynchronizationCalled).isTrue();
  }

  static class MockTransactionManager implements TransactionManager {
    boolean commitCalled = false;
    boolean rollbackCalled = false;
    boolean setRollbackOnlyCalled = false;
    boolean getStatusCalled = false;
    Transaction transaction = null;

    public void setTransaction(Transaction transaction) {
      this.transaction = transaction;
    }

    @Override
    public void begin() throws jakarta.transaction.NotSupportedException, SystemException {
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
      return TransactionManagerStatusMock.STATUS;
    }

    @Override
    public Transaction getTransaction() throws SystemException {
      return transaction;
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
    }
  }

  static class TransactionManagerStatusMock {
    public static final int STATUS = 1;
  }

  static class MockTransaction implements Transaction {
    boolean enlistResourceCalled = false;
    boolean delistResourceCalled = false;
    boolean registerSynchronizationCalled = false;

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
    public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
      enlistResourceCalled = true;
      return true;
    }

    @Override
    public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
      delistResourceCalled = true;
      return true;
    }

    @Override
    public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException, SystemException {
      registerSynchronizationCalled = true;
    }
  }

  static class MockXAResource implements XAResource {
    @Override
    public void commit(javax.transaction.xa.Xid xid, boolean onePhase) throws javax.transaction.xa.XAException {
    }

    @Override
    public void end(javax.transaction.xa.Xid xid, int flags) throws javax.transaction.xa.XAException {
    }

    @Override
    public void forget(javax.transaction.xa.Xid xid) throws javax.transaction.xa.XAException {
    }

    @Override
    public int getTransactionTimeout() throws javax.transaction.xa.XAException {
      return 0;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws javax.transaction.xa.XAException {
      return false;
    }

    @Override
    public int prepare(javax.transaction.xa.Xid xid) throws javax.transaction.xa.XAException {
      return 0;
    }

    @Override
    public javax.transaction.xa.Xid[] recover(int flag) throws javax.transaction.xa.XAException {
      return new javax.transaction.xa.Xid[0];
    }

    @Override
    public void rollback(javax.transaction.xa.Xid xid) throws javax.transaction.xa.XAException {
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws javax.transaction.xa.XAException {
      return false;
    }

    @Override
    public void start(javax.transaction.xa.Xid xid, int flags) throws javax.transaction.xa.XAException {
    }
  }

  static class MockSynchronization implements Synchronization {
    @Override
    public void beforeCompletion() {
    }

    @Override
    public void afterCompletion(int status) {
    }
  }

}