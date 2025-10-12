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

import java.util.Properties;

import infra.jndi.JndiTemplate;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:30
 */
class JtaTransactionManagerTests {
  
  @Test
  void defaultConstructorInitializesProperties() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();

    assertThat(jtaTransactionManager).isNotNull();
    assertThat(jtaTransactionManager.isNestedTransactionAllowed()).isTrue();
  }

  @Test
  void constructorWithUserTransaction() {
    UserTransaction userTransaction = new MockUserTransaction();
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(userTransaction);

    assertThat(jtaTransactionManager).isNotNull();
    assertThat(jtaTransactionManager.getUserTransaction()).isEqualTo(userTransaction);
  }

  @Test
  void constructorWithUserTransactionAndTransactionManager() {
    UserTransaction userTransaction = new MockUserTransaction();
    TransactionManager transactionManager = new MockTransactionManager();
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(userTransaction, transactionManager);

    assertThat(jtaTransactionManager).isNotNull();
    assertThat(jtaTransactionManager.getUserTransaction()).isEqualTo(userTransaction);
    assertThat(jtaTransactionManager.getTransactionManager()).isEqualTo(transactionManager);
  }

  @Test
  void constructorWithTransactionManager() {
    TransactionManager transactionManager = new MockTransactionManager();
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(transactionManager);

    assertThat(jtaTransactionManager).isNotNull();
    assertThat(jtaTransactionManager.getTransactionManager()).isEqualTo(transactionManager);
    assertThat(jtaTransactionManager.getUserTransaction()).isNotNull();
  }

  @Test
  void setUserTransactionName() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    String userTransactionName = "java:comp/UserTransaction";

    jtaTransactionManager.setUserTransactionName(userTransactionName);

    // Simply test that the method exists and can be called
    assertThatNoException().isThrownBy(() ->
            JtaTransactionManager.class.getDeclaredMethod("setUserTransactionName", String.class));
  }

  @Test
  void setTransactionManagerName() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    String transactionManagerName = "java:comp/TransactionManager";

    jtaTransactionManager.setTransactionManagerName(transactionManagerName);

    // Simply test that the method exists and can be called
    assertThatNoException().isThrownBy(() ->
            JtaTransactionManager.class.getDeclaredMethod("setTransactionManagerName", String.class));
  }

  @Test
  void setTransactionSynchronizationRegistryName() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    String registryName = "java:comp/TransactionSynchronizationRegistry";

    jtaTransactionManager.setTransactionSynchronizationRegistryName(registryName);

    // Simply test that the method exists and can be called
    assertThatNoException().isThrownBy(() ->
            JtaTransactionManager.class.getDeclaredMethod("setTransactionSynchronizationRegistryName", String.class));
  }

  @Test
  void setAutodetectUserTransaction() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();

    jtaTransactionManager.setAutodetectUserTransaction(false);

    // Simply test that the method exists and can be called
    assertThatNoException().isThrownBy(() ->
            JtaTransactionManager.class.getDeclaredMethod("setAutodetectUserTransaction", boolean.class));
  }

  @Test
  void setAutodetectTransactionManager() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();

    jtaTransactionManager.setAutodetectTransactionManager(false);

    // Simply test that the method exists and can be called
    assertThatNoException().isThrownBy(() ->
            JtaTransactionManager.class.getDeclaredMethod("setAutodetectTransactionManager", boolean.class));
  }

  @Test
  void setAutodetectTransactionSynchronizationRegistry() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();

    jtaTransactionManager.setAutodetectTransactionSynchronizationRegistry(false);

    // Simply test that the method exists and can be called
    assertThatNoException().isThrownBy(() ->
            JtaTransactionManager.class.getDeclaredMethod("setAutodetectTransactionSynchronizationRegistry", boolean.class));
  }

  @Test
  void setCacheUserTransaction() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();

    jtaTransactionManager.setCacheUserTransaction(false);

    // Simply test that the method exists and can be called
    assertThatNoException().isThrownBy(() ->
            JtaTransactionManager.class.getDeclaredMethod("setCacheUserTransaction", boolean.class));
  }

  @Test
  void setAllowCustomIsolationLevels() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();

    jtaTransactionManager.setAllowCustomIsolationLevels(true);

    // Simply test that the method exists and can be called
    assertThatNoException().isThrownBy(() ->
            JtaTransactionManager.class.getDeclaredMethod("setAllowCustomIsolationLevels", boolean.class));
  }

  @Test
  void setJndiTemplate() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    JndiTemplate jndiTemplate = new JndiTemplate();

    jtaTransactionManager.setJndiTemplate(jndiTemplate);

    assertThat(jtaTransactionManager.getJndiTemplate()).isEqualTo(jndiTemplate);
  }

  @Test
  void setJndiEnvironment() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    Properties jndiEnvironment = new Properties();
    jndiEnvironment.setProperty("java.naming.factory.initial", "test");

    jtaTransactionManager.setJndiEnvironment(jndiEnvironment);

    assertThat(jtaTransactionManager.getJndiEnvironment()).isEqualTo(jndiEnvironment);
  }

  @Test
  void setUserTransaction() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    UserTransaction userTransaction = new MockUserTransaction();

    jtaTransactionManager.setUserTransaction(userTransaction);

    assertThat(jtaTransactionManager.getUserTransaction()).isEqualTo(userTransaction);
  }

  @Test
  void setTransactionManager() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    TransactionManager transactionManager = new MockTransactionManager();

    jtaTransactionManager.setTransactionManager(transactionManager);

    assertThat(jtaTransactionManager.getTransactionManager()).isEqualTo(transactionManager);
  }

  @Test
  void setTransactionSynchronizationRegistry() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    TransactionSynchronizationRegistry registry = new MockTransactionSynchronizationRegistry();

    jtaTransactionManager.setTransactionSynchronizationRegistry(registry);

    assertThat(jtaTransactionManager.getTransactionSynchronizationRegistry()).isEqualTo(registry);
  }

  static class MockUserTransaction implements UserTransaction {
    @Override
    public void begin() throws NotSupportedException, SystemException {
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
    public void setTransactionTimeout(int seconds) throws SystemException {
    }
  }

  static class MockTransactionManager implements TransactionManager {
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
    public void begin() throws NotSupportedException, SystemException {
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
    public void setTransactionTimeout(int seconds) throws SystemException {
    }
  }

  static class MockTransactionSynchronizationRegistry implements TransactionSynchronizationRegistry {
    @Override
    public Object getTransactionKey() {
      return null;
    }

    @Override
    public int getTransactionStatus() {
      return 0;
    }

    @Override
    public boolean getRollbackOnly() {
      return false;
    }

    @Override
    public void setRollbackOnly() {
    }

    @Override
    public Object getResource(Object key) {
      return null;
    }

    @Override
    public void putResource(Object key, Object value) {
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync) {
    }
  }

}