/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.testfixture.jndi.ExpectedLookupTemplate;
import cn.taketoday.transaction.jta.JtaTransactionManager;
import cn.taketoday.transaction.jta.UserTransactionAdapter;
import cn.taketoday.transaction.support.TransactionCallbackWithoutResult;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 05.08.2005
 */
public class JndiJtaTransactionManagerTests {

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
    tt.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        // something transactional
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      }
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
    tt.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        // something transactional
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      }
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
    tt.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        // something transactional
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      }
    });

    ptm.setJndiTemplate(new ExpectedLookupTemplate("java:comp/UserTransaction", ut2));
    tt.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        // something transactional
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      }
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
