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

package infra.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.naming.NamingException;

import infra.context.testfixture.jndi.ExpectedLookupTemplate;
import infra.transaction.jta.JtaTransactionManager;
import infra.transaction.jta.UserTransactionAdapter;
import infra.transaction.support.TransactionSynchronizationManager;
import infra.transaction.support.TransactionTemplate;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 05.08.2005
 */
public class JndiJtaTransactionManagerTests {

  @ParameterizedTest
  @ValueSource(strings = { "userTransaction", "transactionManager", "synchronizationRegistry" })
  void reportsMissingExplicitJndiBinding(String binding) {
    var manager = new JtaTransactionManager();
    manager.setJndiTemplate(new ExpectedLookupTemplate());
    manager.setAutodetectUserTransaction(false);
    manager.setAutodetectTransactionManager(false);
    switch (binding) {
      case "userTransaction" -> manager.setUserTransactionName(binding);
      case "transactionManager" -> manager.setTransactionManagerName(binding);
      case "synchronizationRegistry" -> {
        manager.setUserTransaction(mock(UserTransaction.class));
        manager.setTransactionSynchronizationRegistryName(binding);
      }
      default -> throw new AssertionError(binding);
    }

    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(manager::afterPropertiesSet)
            .withMessageContaining("JNDI location [" + binding + "]").withCauseInstanceOf(NamingException.class);
  }

  @Test
  void failsInitializationWhenNoUserTransactionCanBeFound() {
    var manager = new JtaTransactionManager();
    manager.setJndiTemplate(new ExpectedLookupTemplate());

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(manager::afterPropertiesSet)
            .withMessageContaining("No JTA UserTransaction available");
  }

  @Test
  void canDisableUserTransactionAndTransactionManagerAutodetection() {
    var manager = new JtaTransactionManager();
    manager.setJndiTemplate(new ExpectedLookupTemplate());
    manager.setAutodetectUserTransaction(false);
    manager.setAutodetectTransactionManager(false);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(manager::afterPropertiesSet)
            .withMessageContaining("No JTA UserTransaction available");
  }

  @Test
  void canDisableTransactionSynchronizationRegistryAutodetection() {
    var manager = new JtaTransactionManager(mock(UserTransaction.class));
    manager.setAutodetectTransactionSynchronizationRegistry(false);

    manager.afterPropertiesSet();

    assertThat(manager.getTransactionSynchronizationRegistry()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = { "configuredRegistry", "java:comp/TransactionSynchronizationRegistry" })
  void findsSynchronizationRegistryInJndi(String registryName) {
    var registry = mock(TransactionSynchronizationRegistry.class);
    var userTransaction = mock(UserTransaction.class);
    var jndi = new ExpectedLookupTemplate("java:comp/UserTransaction", userTransaction);
    jndi.addObject(registryName, registry);
    var manager = new JtaTransactionManager();
    manager.setJndiTemplate(jndi);
    if (registryName.equals("configuredRegistry")) {
      manager.setTransactionSynchronizationRegistryName(registryName);
    }

    manager.afterPropertiesSet();

    assertThat(manager.getTransactionSynchronizationRegistry()).isSameAs(registry);
  }

  @Test
  void autodetectsTransactionManagerAndRegistryFromUserTransactionHandle() {
    var handle = mock(CombinedUserTransaction.class);
    var manager = new JtaTransactionManager((UserTransaction) handle);
    manager.setJndiTemplate(new ExpectedLookupTemplate());

    manager.afterPropertiesSet();

    assertThat(manager.getTransactionManager()).isSameAs(handle);
    assertThat(manager.getTransactionSynchronizationRegistry()).isSameAs(handle);
    assertThat(new JtaTransactionManager((TransactionManager) handle).getUserTransaction()).isSameAs(handle);
  }

  @Test
  void autodetectsRegistryFromTransactionManagerHandle() {
    var handle = mock(TransactionManagerWithRegistry.class);
    var manager = new JtaTransactionManager(handle);

    manager.afterPropertiesSet();

    assertThat(manager.getTransactionSynchronizationRegistry()).isSameAs(handle);
    assertThat(manager.getUserTransaction()).isInstanceOf(UserTransactionAdapter.class);
  }

  interface TransactionManagerWithRegistry extends TransactionManager, TransactionSynchronizationRegistry { }

  interface CombinedUserTransaction extends UserTransaction, TransactionManagerWithRegistry { }

  @Test
  public void jtaTransactionManagerWithDefaultJndiLookups1() throws Exception {
    doTestJtaTransactionManagerWithDefaultJndiLookups("java:comp/TransactionManager", true, true);
  }

  @Test
  public void jtaTransactionManagerWithDefaultJndiLookups2() throws Exception {
    doTestJtaTransactionManagerWithDefaultJndiLookups("java:/TransactionManager", true, true);
  }

  @Test
  public void jtaTransactionManagerWithDefaultJndiLookupsAndNoTmFound() throws Exception {
    doTestJtaTransactionManagerWithDefaultJndiLookups("java:/tm", false, true);
  }

  @Test
  public void jtaTransactionManagerWithDefaultJndiLookupsAndNoUtFound() throws Exception {
    doTestJtaTransactionManagerWithDefaultJndiLookups("java:/TransactionManager", true, false);
  }

  private void doTestJtaTransactionManagerWithDefaultJndiLookups(String tmName, boolean tmFound, boolean defaultUt)
          throws Exception {

    UserTransaction ut = mock(UserTransaction.class);
    TransactionManager tm = mock(TransactionManager.class);
    if (defaultUt) {
      given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    }
    else {
      given(tm.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    }

    JtaTransactionManager ptm = new JtaTransactionManager();
    ExpectedLookupTemplate jndiTemplate = new ExpectedLookupTemplate();
    if (defaultUt) {
      jndiTemplate.addObject("java:comp/UserTransaction", ut);
    }
    jndiTemplate.addObject(tmName, tm);
    ptm.setJndiTemplate(jndiTemplate);
    ptm.afterPropertiesSet();

    if (tmFound) {
      assertThat(ptm.getTransactionManager()).isEqualTo(tm);
    }
    else {
      assertThat(ptm.getTransactionManager()).isNull();
    }

    if (defaultUt) {
      assertThat(ptm.getUserTransaction()).isEqualTo(ut);
    }
    else {
      boolean condition = ptm.getUserTransaction() instanceof UserTransactionAdapter;
      assertThat(condition).isTrue();
      UserTransactionAdapter uta = (UserTransactionAdapter) ptm.getUserTransaction();
      assertThat(uta.getTransactionManager()).isEqualTo(tm);
    }

    TransactionTemplate tt = new TransactionTemplate(ptm);
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).isTrue();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    });
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

    if (defaultUt) {
      verify(ut).begin();
      verify(ut).commit();
    }
    else {
      verify(tm).begin();
      verify(tm).commit();
    }

  }

  @Test
  public void jtaTransactionManagerWithCustomJndiLookups() throws Exception {
    UserTransaction ut = mock(UserTransaction.class);
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

    TransactionManager tm = mock(TransactionManager.class);

    JtaTransactionManager ptm = new JtaTransactionManager();
    ptm.setUserTransactionName("jndi-ut");
    ptm.setTransactionManagerName("jndi-tm");
    ExpectedLookupTemplate jndiTemplate = new ExpectedLookupTemplate();
    jndiTemplate.addObject("jndi-ut", ut);
    jndiTemplate.addObject("jndi-tm", tm);
    ptm.setJndiTemplate(jndiTemplate);
    ptm.afterPropertiesSet();

    assertThat(ptm.getUserTransaction()).isEqualTo(ut);
    assertThat(ptm.getTransactionManager()).isEqualTo(tm);

    TransactionTemplate tt = new TransactionTemplate(ptm);
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).isTrue();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    });
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

    verify(ut).begin();
    verify(ut).commit();
  }

  @Test
  public void jtaTransactionManagerWithNotCacheUserTransaction() throws Exception {
    UserTransaction ut = mock(UserTransaction.class);
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

    UserTransaction ut2 = mock(UserTransaction.class);
    given(ut2.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

    JtaTransactionManager ptm = new JtaTransactionManager();
    ptm.setJndiTemplate(new ExpectedLookupTemplate("java:comp/UserTransaction", ut));
    ptm.setCacheUserTransaction(false);
    ptm.afterPropertiesSet();

    assertThat(ptm.getUserTransaction()).isEqualTo(ut);

    TransactionTemplate tt = new TransactionTemplate(ptm);
    assertThat(ptm.getTransactionSynchronization()).isEqualTo(JtaTransactionManager.SYNCHRONIZATION_ALWAYS);
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).isTrue();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    });

    ptm.setJndiTemplate(new ExpectedLookupTemplate("java:comp/UserTransaction", ut2));
    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    });
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).isTrue();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

    verify(ut).begin();
    verify(ut).commit();
    verify(ut2).begin();
    verify(ut2).commit();
  }

  /**
   * Prevent any side-effects due to this test modifying ThreadLocals that might
   * affect subsequent tests when all tests are run in the same JVM, as with Eclipse.
   */
  @AfterEach
  public void tearDown() {
    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

}
