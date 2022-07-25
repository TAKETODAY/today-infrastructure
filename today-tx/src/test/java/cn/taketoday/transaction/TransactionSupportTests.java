/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import cn.taketoday.transaction.support.DefaultTransactionDefinition;
import cn.taketoday.transaction.support.DefaultTransactionStatus;
import cn.taketoday.transaction.support.TransactionCallbackWithoutResult;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Juergen Hoeller
 */
@Execution(ExecutionMode.SAME_THREAD)
public class TransactionSupportTests {

  @Test
  public void noExistingTransaction() {
    PlatformTransactionManager tm = new TestTransactionManager(false, true);
    DefaultTransactionStatus status1 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS));
    assertThat(status1.hasTransaction()).as("Must not have transaction").isFalse();

    DefaultTransactionStatus status2 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));
    assertThat(status2.hasTransaction()).as("Must have transaction").isTrue();
    assertThat(status2.isNewTransaction()).as("Must be new transaction").isTrue();

    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
            tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY)));
  }

  @Test
  public void existingTransaction() {
    PlatformTransactionManager tm = new TestTransactionManager(true, true);
    DefaultTransactionStatus status1 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS));
    assertThat(status1.getTransaction() != null).as("Must have transaction").isTrue();
    boolean condition2 = !status1.isNewTransaction();
    assertThat(condition2).as("Must not be new transaction").isTrue();

    DefaultTransactionStatus status2 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));
    assertThat(status2.getTransaction() != null).as("Must have transaction").isTrue();
    boolean condition1 = !status2.isNewTransaction();
    assertThat(condition1).as("Must not be new transaction").isTrue();

    DefaultTransactionStatus status3 = (DefaultTransactionStatus)
            tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY));
    assertThat(status3.getTransaction() != null).as("Must have transaction").isTrue();
    boolean condition = !status3.isNewTransaction();
    assertThat(condition).as("Must not be new transaction").isTrue();
  }

  @Test
  public void commitWithoutExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionStatus status = tm.getTransaction(null);
    tm.commit(status);

    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("triggered commit").isTrue();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  public void rollbackWithoutExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionStatus status = tm.getTransaction(null);
    tm.rollback(status);

    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  public void rollbackOnlyWithoutExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionStatus status = tm.getTransaction(null);
    status.setRollbackOnly();
    tm.commit(status);

    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  public void commitWithExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(true, true);
    TransactionStatus status = tm.getTransaction(null);
    tm.commit(status);

    assertThat(tm.begin).as("no begin").isFalse();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  public void rollbackWithExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(true, true);
    TransactionStatus status = tm.getTransaction(null);
    tm.rollback(status);

    assertThat(tm.begin).as("no begin").isFalse();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("triggered rollbackOnly").isTrue();
  }

  @Test
  public void rollbackOnlyWithExistingTransaction() {
    TestTransactionManager tm = new TestTransactionManager(true, true);
    TransactionStatus status = tm.getTransaction(null);
    status.setRollbackOnly();
    tm.commit(status);

    assertThat(tm.begin).as("no begin").isFalse();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("triggered rollbackOnly").isTrue();
  }

  @Test
  public void transactionTemplate() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionTemplate template = new TransactionTemplate(tm);
    template.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
      }
    });

    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("triggered commit").isTrue();
    assertThat(tm.rollback).as("no rollback").isFalse();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  public void transactionTemplateWithCallbackPreference() {
    MockCallbackPreferringTransactionManager ptm = new MockCallbackPreferringTransactionManager();
    TransactionTemplate template = new TransactionTemplate(ptm);
    template.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
      }
    });

    assertThat(ptm.getDefinition()).isSameAs(template);
    assertThat(ptm.getStatus().isRollbackOnly()).isFalse();
  }

  @Test
  public void transactionTemplateWithException() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionTemplate template = new TransactionTemplate(tm);
    final RuntimeException ex = new RuntimeException("Some application exception");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                    template.execute(new TransactionCallbackWithoutResult() {
                      @Override
                      protected void doInTransactionWithoutResult(TransactionStatus status) {
                        throw ex;
                      }
                    }))
            .isSameAs(ex);
    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @SuppressWarnings("serial")
  @Test
  public void transactionTemplateWithRollbackException() {
    final TransactionSystemException tex = new TransactionSystemException("system exception");
    TestTransactionManager tm = new TestTransactionManager(false, true) {
      @Override
      protected void doRollback(DefaultTransactionStatus status) {
        super.doRollback(status);
        throw tex;
      }
    };
    TransactionTemplate template = new TransactionTemplate(tm);
    final RuntimeException ex = new RuntimeException("Some application exception");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
                    template.execute(new TransactionCallbackWithoutResult() {
                      @Override
                      protected void doInTransactionWithoutResult(TransactionStatus status) {
                        throw ex;
                      }
                    }))
            .isSameAs(tex);
    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  public void transactionTemplateWithError() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionTemplate template = new TransactionTemplate(tm);
    assertThatExceptionOfType(Error.class).isThrownBy(() ->
            template.execute(new TransactionCallbackWithoutResult() {
              @Override
              protected void doInTransactionWithoutResult(TransactionStatus status) {
                throw new Error("Some application error");
              }
            }));
    assertThat(tm.begin).as("triggered begin").isTrue();
    assertThat(tm.commit).as("no commit").isFalse();
    assertThat(tm.rollback).as("triggered rollback").isTrue();
    assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
  }

  @Test
  public void transactionTemplateInitialization() {
    TestTransactionManager tm = new TestTransactionManager(false, true);
    TransactionTemplate template = new TransactionTemplate();
    template.setTransactionManager(tm);
    assertThat(template.getTransactionManager() == tm).as("correct transaction manager set").isTrue();

    assertThatIllegalArgumentException().isThrownBy(() ->
            template.setPropagationBehaviorName("TIMEOUT_DEFAULT"));
    template.setPropagationBehaviorName("PROPAGATION_SUPPORTS");
    assertThat(template.getPropagationBehavior() == TransactionDefinition.PROPAGATION_SUPPORTS).as("Correct propagation behavior set").isTrue();

    assertThatIllegalArgumentException().isThrownBy(() ->
            template.setPropagationBehavior(999));
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
    assertThat(template.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY).as("Correct propagation behavior set").isTrue();

    assertThatIllegalArgumentException().isThrownBy(() ->
            template.setIsolationLevelName("TIMEOUT_DEFAULT"));
    template.setIsolationLevelName("ISOLATION_SERIALIZABLE");
    assertThat(template.getIsolationLevel() == TransactionDefinition.ISOLATION_SERIALIZABLE).as("Correct isolation level set").isTrue();

    assertThatIllegalArgumentException().isThrownBy(() ->
            template.setIsolationLevel(999));

    template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    assertThat(template.getIsolationLevel() == TransactionDefinition.ISOLATION_REPEATABLE_READ).as("Correct isolation level set").isTrue();
  }

  @Test
  public void transactionTemplateEquality() {
    TestTransactionManager tm1 = new TestTransactionManager(false, true);
    TestTransactionManager tm2 = new TestTransactionManager(false, true);
    TransactionTemplate template1 = new TransactionTemplate(tm1);
    TransactionTemplate template2 = new TransactionTemplate(tm2);
    TransactionTemplate template3 = new TransactionTemplate(tm2);

    assertThat(template2).isNotEqualTo(template1);
    assertThat(template3).isNotEqualTo(template1);
    assertThat(template3).isEqualTo(template2);
  }

  @AfterEach
  public void clear() {
    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
  }

}
