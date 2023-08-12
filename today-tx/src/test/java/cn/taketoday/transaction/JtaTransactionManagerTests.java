/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.dao.OptimisticLockingFailureException;
import cn.taketoday.transaction.jta.JtaTransactionManager;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 4.0
 */
public class JtaTransactionManagerTests {

  @Test
  public void jtaTransactionManagerWithCommit() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setName("txName");

    assertThat(ptm.getTransactionSynchronization()).isEqualTo(JtaTransactionManager.SYNCHRONIZATION_ALWAYS);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(status.getTransactionName()).isEqualTo("txName");
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.isReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName");
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(status.isRollbackOnly()).isFalse();
      assertThat(status.isCompleted()).isFalse();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

    verify(ut).begin();
    verify(ut).commit();
    verify(synch).beforeCommit(false);
    verify(synch).beforeCompletion();
    verify(synch).afterCommit();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
  }

  @Test
  public void jtaTransactionManagerWithCommitAndSynchronizationOnActual() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(status.getTransactionName()).isEmpty();
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.isReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      assertThat(status.isRollbackOnly()).isFalse();
      assertThat(status.isCompleted()).isFalse();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).begin();
    verify(ut).commit();
    verify(synch).beforeCommit(false);
    verify(synch).beforeCompletion();
    verify(synch).afterCommit();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
  }

  @Test
  public void jtaTransactionManagerWithCommitAndSynchronizationNever() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(
            Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_NEVER);
    ptm.afterPropertiesSet();

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(status.getTransactionName()).isEmpty();
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.isReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
      assertThat(status.isRollbackOnly()).isFalse();
      assertThat(status.isCompleted()).isFalse();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).begin();
    verify(ut).commit();
  }

  @Test
  public void jtaTransactionManagerWithRollback() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setTimeout(10);
    tt.setName("txName");

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(status.getTransactionName()).isEqualTo("txName");
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.isReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName");
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      status.setRollbackOnly();
      assertThat(status.isRollbackOnly()).isTrue();
      assertThat(status.isCompleted()).isFalse();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

    verify(ut).setTransactionTimeout(10);
    verify(ut).begin();
    verify(ut).rollback();
    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
  }

  @Test
  public void jtaTransactionManagerWithRollbackAndSynchronizationOnActual() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
    tt.setTimeout(10);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(status.getTransactionName()).isEmpty();
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.isReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      status.setRollbackOnly();
      assertThat(status.isRollbackOnly()).isTrue();
      assertThat(status.isCompleted()).isFalse();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).setTransactionTimeout(10);
    verify(ut).begin();
    verify(ut).rollback();
    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
  }

  @Test
  public void jtaTransactionManagerWithRollbackAndSynchronizationNever() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    ptm.setTransactionSynchronizationName("SYNCHRONIZATION_NEVER");
    tt.setTimeout(10);
    ptm.afterPropertiesSet();

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(status.getTransactionName()).isEmpty();
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.isReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
      assertThat(status.isRollbackOnly()).isFalse();
      status.setRollbackOnly();
      assertThat(status.isRollbackOnly()).isTrue();
      assertThat(status.isCompleted()).isFalse();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).setTransactionTimeout(10);
    verify(ut).begin();
    verify(ut, atLeastOnce()).getStatus();
    verify(ut).rollback();
  }

  @Test
  public void jtaTransactionManagerWithExistingTransactionAndRollbackOnly() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).setRollbackOnly();
    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
  }

  @Test
  public void jtaTransactionManagerWithExistingTransactionAndException() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThatIllegalStateException().isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
              TransactionSynchronizationManager.registerSynchronization(synch);
              throw new IllegalStateException("I want a rollback");
            }));
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).setRollbackOnly();
    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
  }

  @Test
  public void jtaTransactionManagerWithExistingTransactionAndCommitException() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

    final TransactionSynchronization synch = mock();
    willThrow(new OptimisticLockingFailureException("")).given(synch).beforeCommit(false);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
              TransactionSynchronizationManager.registerSynchronization(synch);
            }));
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).setRollbackOnly();
    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
  }

  @Test
  public void jtaTransactionManagerWithExistingTransactionAndRollbackOnlyAndNoGlobalRollback() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    ptm.setGlobalRollbackOnParticipationFailure(false);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).setRollbackOnly();
    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
  }

  @Test
  public void jtaTransactionManagerWithExistingTransactionAndExceptionAndNoGlobalRollback() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    ptm.setGlobalRollbackOnParticipationFailure(false);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThatIllegalStateException().isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
              TransactionSynchronizationManager.registerSynchronization(synch);
              throw new IllegalStateException("I want a rollback");
            }));
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
  }

  @Test
  public void jtaTransactionManagerWithExistingTransactionAndJtaSynchronization() throws Exception {
    UserTransaction ut = mock();
    TransactionManager tm = mock();
    MockJtaTransaction tx = new MockJtaTransaction();

    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    given(tm.getTransaction()).willReturn(tx);

    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(tx.getSynchronization()).isNotNull();
    tx.getSynchronization().beforeCompletion();
    tx.getSynchronization().afterCompletion(Status.STATUS_ROLLEDBACK);

    verify(ut).setRollbackOnly();
    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
  }

  @Test
  public void jtaTransactionManagerWithExistingTransactionAndSynchronizationOnActual() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).setRollbackOnly();
    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
  }

  @Test
  public void jtaTransactionManagerWithExistingTransactionAndSynchronizationNever() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_NEVER);
    ptm.afterPropertiesSet();

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).setRollbackOnly();
  }

  @Test
  public void jtaTransactionManagerWithExistingAndPropagationSupports() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).setRollbackOnly();
    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
  }

  @Test
  public void jtaTransactionManagerWithPropagationSupports() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

    final TransactionSynchronization synch = mock();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      TransactionSynchronizationManager.registerSynchronization(synch);
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(synch).beforeCompletion();
    verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
  }

  @Test
  public void jtaTransactionManagerWithPropagationSupportsAndSynchronizationOnActual() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    ptm.afterPropertiesSet();

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  @Test
  public void jtaTransactionManagerWithPropagationSupportsAndSynchronizationNever() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_NEVER);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    ptm.afterPropertiesSet();

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  @Test
  public void jtaTransactionManagerWithPropagationNotSupported() throws Exception {
    UserTransaction ut = mock();
    TransactionManager tm = mock();
    Transaction tx = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    given(tm.suspend()).willReturn(tx);

    JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      status.setRollbackOnly();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(tm).resume(tx);
  }

  @Test
  public void jtaTransactionManagerWithPropagationRequiresNew() throws Exception {
    UserTransaction ut = mock();
    TransactionManager tm = mock();
    Transaction tx = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE,
            Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    given(tm.suspend()).willReturn(tx);

    final JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    tt.setName("txName");

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName");
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

      TransactionTemplate tt2 = new TransactionTemplate(ptm);
      tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      tt2.setReadOnly(true);
      tt2.setName("txName2");
      tt2.executeWithoutResult(status1 -> {
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName2");
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
      });

      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName");
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut, times(2)).begin();
    verify(ut, times(2)).commit();
    verify(tm).resume(tx);
  }

  @Test
  public void jtaTransactionManagerWithPropagationRequiresNewWithinSupports() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
            Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

    final JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

      TransactionTemplate tt2 = new TransactionTemplate(ptm);
      tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      tt2.executeWithoutResult(status1 -> {
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      });

      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).begin();
    verify(ut).commit();
  }

  @Test
  public void jtaTransactionManagerWithPropagationRequiresNewAndExisting() throws Exception {
    UserTransaction ut = mock();
    TransactionManager tm = mock();
    Transaction tx = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    given(tm.suspend()).willReturn(tx);

    JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue());
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(ut).begin();
    verify(ut).commit();
    verify(tm).resume(tx);
  }

  @Test
  public void jtaTransactionManagerWithPropagationRequiresNewAndExistingWithSuspendException() throws Exception {
    UserTransaction ut = mock();
    TransactionManager tm = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    willThrow(new SystemException()).given(tm).suspend();

    JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
            }));
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  @Test
  public void jtaTransactionManagerWithPropagationRequiresNewAndExistingWithBeginException() throws Exception {
    UserTransaction ut = mock();
    TransactionManager tm = mock();
    Transaction tx = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    given(tm.suspend()).willReturn(tx);
    willThrow(new SystemException()).given(ut).begin();

    JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue()));
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    verify(tm).resume(tx);
  }

  @Test
  public void jtaTransactionManagerWithPropagationRequiresNewAndAdapter() throws Exception {
    TransactionManager tm = mock();
    Transaction tx = mock();
    given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
    given(tm.suspend()).willReturn(tx);

    JtaTransactionManager ptm = newJtaTransactionManager(tm);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue());
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(tm).begin();
    verify(tm).commit();
    verify(tm).resume(tx);
  }

  @Test
  public void jtaTransactionManagerWithPropagationRequiresNewAndSuspensionNotSupported() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThatExceptionOfType(TransactionSuspensionNotSupportedException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
            }));
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

  @Test
  public void jtaTransactionManagerWithIsolationLevel() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

    assertThatExceptionOfType(InvalidIsolationLevelException.class).isThrownBy(() -> {
      JtaTransactionManager ptm = newJtaTransactionManager(ut);
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
      tt.executeWithoutResult(status -> {
        // something transactional
      });
    });
  }

  @Test
  public void jtaTransactionManagerWithSystemExceptionOnIsExisting() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willThrow(new SystemException("system exception"));

    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
      JtaTransactionManager ptm = newJtaTransactionManager(ut);
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.executeWithoutResult(status -> {
        // something transactional
      });
    });
  }

  @Test
  public void jtaTransactionManagerWithNestedBegin() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(status.getTransactionName()).isEmpty();
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isTrue();
      assertThat(status.isReadOnly()).isFalse();
      assertThat(status.isRollbackOnly()).isFalse();
      assertThat(status.isCompleted()).isFalse();
    });

    verify(ut).begin();
    verify(ut).commit();
  }

  @Test
  public void jtaTransactionManagerWithNotSupportedExceptionOnNestedBegin() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    willThrow(new NotSupportedException("not supported")).given(ut).begin();

    assertThatExceptionOfType(NestedTransactionNotSupportedException.class).isThrownBy(() -> {
      JtaTransactionManager ptm = newJtaTransactionManager(ut);
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
      tt.executeWithoutResult(status -> {
        // something transactional
      });
    });
  }

  @Test
  public void jtaTransactionManagerWithUnsupportedOperationExceptionOnNestedBegin() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    willThrow(new UnsupportedOperationException("not supported")).given(ut).begin();

    assertThatExceptionOfType(NestedTransactionNotSupportedException.class).isThrownBy(() -> {
      JtaTransactionManager ptm = newJtaTransactionManager(ut);
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
      tt.executeWithoutResult(status -> {
        // something transactional
      });
    });
  }

  @Test
  public void jtaTransactionManagerWithSystemExceptionOnBegin() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);
    willThrow(new SystemException("system exception")).given(ut).begin();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    ptm.addListener(tl);

    assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() -> {
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.executeWithoutResult(status -> {
        // something transactional
      });
    });

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beginFailure).isInstanceOf(CannotCreateTransactionException.class);
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.commitFailure).isNull();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
    assertThat(tl.rollbackFailure).isNull();
  }

  @Test
  public void jtaTransactionManagerWithRollbackExceptionOnCommit() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    willThrow(new RollbackException("unexpected rollback")).given(ut).commit();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    ptm.addListener(tl);

    assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() -> {
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.executeWithoutResult(status -> {
        // something transactional
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            assertThat(status).as("Correct completion status")
                    .isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
          }
        });
      });
    });

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beginFailure).isNull();
    assertThat(tl.beforeCommitCalled).isTrue();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.commitFailure).isNull();
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isTrue();
    assertThat(tl.rollbackFailure).isNull();

    verify(ut).begin();
  }

  @Test
  public void jtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly() throws Exception {
    doTestJtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly(false);
  }

  @Test
  public void jtaTransactionManagerWithNoExceptionOnGlobalRollbackOnlyAndFailEarly() throws Exception {
    doTestJtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly(true);
  }

  private void doTestJtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly(boolean failEarly) throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
            Status.STATUS_MARKED_ROLLBACK, Status.STATUS_MARKED_ROLLBACK,
            Status.STATUS_MARKED_ROLLBACK);

    JtaTransactionManager tm = newJtaTransactionManager(ut);
    if (failEarly) {
      tm.setFailEarlyOnGlobalRollbackOnly(true);
    }

    TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
    boolean outerTransactionBoundaryReached = false;
    try {
      assertThat(ts.isNewTransaction()).as("Is new transaction").isTrue();

      TransactionTemplate tt = new TransactionTemplate(tm);
      tt.executeWithoutResult(status -> {
        // something transactional
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            assertThat(status).as("Correct completion status")
                    .isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
          }
        });
      });

      outerTransactionBoundaryReached = true;
      tm.commit(ts);

      fail("Should have thrown UnexpectedRollbackException");
    }
    catch (UnexpectedRollbackException ex) {
      // expected
      if (!outerTransactionBoundaryReached) {
        tm.rollback(ts);
      }
      assertThat(outerTransactionBoundaryReached).isNotEqualTo(failEarly);
    }

    verify(ut).begin();
    if (failEarly) {
      verify(ut).rollback();
    }
    else {
      verify(ut).commit();
    }
  }

  @Test
  public void jtaTransactionManagerWithHeuristicMixedExceptionOnCommit() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    willThrow(new HeuristicMixedException("heuristic exception")).given(ut).commit();

    assertThatExceptionOfType(HeuristicCompletionException.class).isThrownBy(() -> {
      JtaTransactionManager ptm = newJtaTransactionManager(ut);
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.executeWithoutResult(status -> {
        // something transactional
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            assertThat(status == TransactionSynchronization.STATUS_UNKNOWN).as("Correct completion status").isTrue();
          }
        });
      });
    }).satisfies(ex -> assertThat(ex.getOutcomeState()).isEqualTo(HeuristicCompletionException.STATE_MIXED));

    verify(ut).begin();
  }

  @Test
  public void jtaTransactionManagerWithHeuristicRollbackExceptionOnCommit() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    willThrow(new HeuristicRollbackException("heuristic exception")).given(ut).commit();

    assertThatExceptionOfType(HeuristicCompletionException.class).isThrownBy(() -> {
      JtaTransactionManager ptm = newJtaTransactionManager(ut);
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.executeWithoutResult(status -> {
        // something transactional
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            assertThat(status == TransactionSynchronization.STATUS_UNKNOWN).as("Correct completion status").isTrue();
          }
        });
      });
    }).satisfies(ex -> assertThat(ex.getOutcomeState()).isEqualTo(HeuristicCompletionException.STATE_ROLLED_BACK));

    verify(ut).begin();
  }

  @Test
  public void jtaTransactionManagerWithSystemExceptionOnCommit() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    willThrow(new SystemException("system exception")).given(ut).commit();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    ptm.addListener(tl);

    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.executeWithoutResult(status -> {
        // something transactional
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            assertThat(status).as("Correct completion status")
                    .isEqualTo(TransactionSynchronization.STATUS_UNKNOWN);
          }
        });
      });
    });

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beginFailure).isNull();
    assertThat(tl.beforeCommitCalled).isTrue();
    assertThat(tl.afterCommitCalled).isTrue();
    assertThat(tl.commitFailure).isInstanceOf(TransactionSystemException.class);
    assertThat(tl.beforeRollbackCalled).isFalse();
    assertThat(tl.afterRollbackCalled).isFalse();
    assertThat(tl.rollbackFailure).isNull();

    verify(ut).begin();
  }

  @Test
  public void jtaTransactionManagerWithSystemExceptionOnRollback() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
    willThrow(new SystemException("system exception")).given(ut).rollback();

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
    ptm.addListener(tl);

    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.executeWithoutResult(status -> {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            assertThat(status).as("Correct completion status")
                    .isEqualTo(TransactionSynchronization.STATUS_UNKNOWN);
          }
        });
        status.setRollbackOnly();
      });
    });

    assertThat(tl.beforeBeginCalled).isTrue();
    assertThat(tl.afterBeginCalled).isTrue();
    assertThat(tl.beginFailure).isNull();
    assertThat(tl.beforeCommitCalled).isFalse();
    assertThat(tl.afterCommitCalled).isFalse();
    assertThat(tl.commitFailure).isNull();
    assertThat(tl.beforeRollbackCalled).isTrue();
    assertThat(tl.afterRollbackCalled).isTrue();
    assertThat(tl.rollbackFailure).isInstanceOf(TransactionSystemException.class);

    verify(ut).begin();
  }

  @Test
  public void jtaTransactionManagerWithIllegalStateExceptionOnRollbackOnly() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    willThrow(new IllegalStateException("no existing transaction")).given(ut).setRollbackOnly();

    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
      JtaTransactionManager ptm = newJtaTransactionManager(ut);
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.executeWithoutResult(status -> status.setRollbackOnly());
    });
  }

  @Test
  public void jtaTransactionManagerWithSystemExceptionOnRollbackOnly() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
    willThrow(new SystemException("system exception")).given(ut).setRollbackOnly();

    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
      JtaTransactionManager ptm = newJtaTransactionManager(ut);
      TransactionTemplate tt = new TransactionTemplate(ptm);
      tt.executeWithoutResult(status -> {
        status.setRollbackOnly();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            assertThat(status).as("Correct completion status")
                    .isEqualTo(TransactionSynchronization.STATUS_UNKNOWN);
          }
        });
      });
    });
  }

  @Test
  public void jtaTransactionManagerWithDoubleCommit() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
    // first commit
    ptm.commit(status);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    // second commit attempt
    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
            ptm.commit(status));
    verify(ut).begin();
    verify(ut).commit();
  }

  @Test
  public void jtaTransactionManagerWithDoubleRollback() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
    // first rollback
    ptm.rollback(status);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    // second rollback attempt
    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
            ptm.rollback(status));

    verify(ut).begin();
    verify(ut).rollback();
  }

  @Test
  public void jtaTransactionManagerWithRollbackAndCommit() throws Exception {
    UserTransaction ut = mock();
    given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

    JtaTransactionManager ptm = newJtaTransactionManager(ut);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
    // first: rollback
    ptm.rollback(status);
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    // second: commit attempt
    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
            ptm.commit(status));

    verify(ut).begin();
    verify(ut).rollback();
  }

  protected JtaTransactionManager newJtaTransactionManager(UserTransaction ut) {
    return new JtaTransactionManager(ut);
  }

  protected JtaTransactionManager newJtaTransactionManager(TransactionManager tm) {
    return new JtaTransactionManager(tm);
  }

  protected JtaTransactionManager newJtaTransactionManager(UserTransaction ut, TransactionManager tm) {
    return new JtaTransactionManager(ut, tm);
  }

  /**
   * Prevent any side effects due to this test modifying ThreadLocals that might
   * affect subsequent tests when all tests are run in the same JVM, as with Eclipse.
   */
  @AfterEach
  public void tearDown() {
    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).isNull();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

}
