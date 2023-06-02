/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.dao.ConcurrencyFailureException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.jdbc.UncategorizedSQLException;
import cn.taketoday.jdbc.datasource.ConnectionHolder;
import cn.taketoday.jdbc.datasource.ConnectionProxy;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.datasource.LazyConnectionDataSourceProxy;
import cn.taketoday.jdbc.datasource.TransactionAwareDataSourceProxy;
import cn.taketoday.transaction.CannotCreateTransactionException;
import cn.taketoday.transaction.IllegalTransactionStateException;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionExecution;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.TransactionTimedOutException;
import cn.taketoday.transaction.UnexpectedRollbackException;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @see cn.taketoday.jdbc.datasource.DataSourceTransactionManagerTests
 */
public class JdbcTransactionManagerTests {

  private DataSource ds;

  private Connection con;

  private JdbcTransactionManager tm;

  @BeforeEach
  public void setup() throws Exception {
    ds = mock(DataSource.class);
    con = mock(Connection.class);
    given(ds.getConnection()).willReturn(con);
    tm = new JdbcTransactionManager(ds);
  }

  @AfterEach
  public void verifyTransactionSynchronizationManagerState() {
    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  public void testTransactionCommitWithAutoCommitTrue() throws Exception {
    doTestTransactionCommitRestoringAutoCommit(true, false, false);
  }

  @Test
  public void testTransactionCommitWithAutoCommitFalse() throws Exception {
    doTestTransactionCommitRestoringAutoCommit(false, false, false);
  }

  @Test
  public void testTransactionCommitWithAutoCommitTrueAndLazyConnection() throws Exception {
    doTestTransactionCommitRestoringAutoCommit(true, true, false);
  }

  @Test
  public void testTransactionCommitWithAutoCommitFalseAndLazyConnection() throws Exception {
    doTestTransactionCommitRestoringAutoCommit(false, true, false);
  }

  @Test
  public void testTransactionCommitWithAutoCommitTrueAndLazyConnectionAndStatementCreated() throws Exception {
    doTestTransactionCommitRestoringAutoCommit(true, true, true);
  }

  @Test
  public void testTransactionCommitWithAutoCommitFalseAndLazyConnectionAndStatementCreated() throws Exception {
    doTestTransactionCommitRestoringAutoCommit(false, true, true);
  }

  private void doTestTransactionCommitRestoringAutoCommit(
          boolean autoCommit, boolean lazyConnection, final boolean createStatement) throws Exception {

    if (lazyConnection) {
      given(con.getAutoCommit()).willReturn(autoCommit);
      given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
      given(con.getWarnings()).willThrow(new SQLException());
    }

    if (!lazyConnection || createStatement) {
      given(con.getAutoCommit()).willReturn(autoCommit);
    }

    final DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(ds) : ds);
    tm = new JdbcTransactionManager(dsToUse);
    TransactionTemplate tt = new TransactionTemplate(tm);
    boolean condition3 = !TransactionSynchronizationManager.hasResource(dsToUse);
    assertThat(condition3).as("Hasn't thread connection").isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).as("Has thread connection").isTrue();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      Connection tCon = DataSourceUtils.getConnection(dsToUse);
      try {
        if (createStatement) {
          tCon.createStatement();
        }
        else {
          tCon.getWarnings();
          tCon.clearWarnings();
        }
      }
      catch (SQLException ex) {
        throw new UncategorizedSQLException("", "", ex);
      }
    });

    boolean condition1 = !TransactionSynchronizationManager.hasResource(dsToUse);
    assertThat(condition1).as("Hasn't thread connection").isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).as("Synchronization not active").isTrue();

    if (autoCommit && (!lazyConnection || createStatement)) {
      InOrder ordered = inOrder(con);
      ordered.verify(con).setAutoCommit(false);
      ordered.verify(con).commit();
      ordered.verify(con).setAutoCommit(true);
    }
    if (createStatement) {
      verify(con, times(2)).close();
    }
    else {
      verify(con).close();
    }
  }

  @Test
  public void testTransactionRollbackWithAutoCommitTrue() throws Exception {
    doTestTransactionRollbackRestoringAutoCommit(true, false, false);
  }

  @Test
  public void testTransactionRollbackWithAutoCommitFalse() throws Exception {
    doTestTransactionRollbackRestoringAutoCommit(false, false, false);
  }

  @Test
  public void testTransactionRollbackWithAutoCommitTrueAndLazyConnection() throws Exception {
    doTestTransactionRollbackRestoringAutoCommit(true, true, false);
  }

  @Test
  public void testTransactionRollbackWithAutoCommitFalseAndLazyConnection() throws Exception {
    doTestTransactionRollbackRestoringAutoCommit(false, true, false);
  }

  @Test
  public void testTransactionRollbackWithAutoCommitTrueAndLazyConnectionAndCreateStatement() throws Exception {
    doTestTransactionRollbackRestoringAutoCommit(true, true, true);
  }

  @Test
  public void testTransactionRollbackWithAutoCommitFalseAndLazyConnectionAndCreateStatement() throws Exception {
    doTestTransactionRollbackRestoringAutoCommit(false, true, true);
  }

  private void doTestTransactionRollbackRestoringAutoCommit(
          boolean autoCommit, boolean lazyConnection, final boolean createStatement) throws Exception {

    if (lazyConnection) {
      given(con.getAutoCommit()).willReturn(autoCommit);
      given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
    }

    if (!lazyConnection || createStatement) {
      given(con.getAutoCommit()).willReturn(autoCommit);
    }

    final DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(ds) : ds);
    tm = new JdbcTransactionManager(dsToUse);
    TransactionTemplate tt = new TransactionTemplate(tm);
    boolean condition3 = !TransactionSynchronizationManager.hasResource(dsToUse);
    assertThat(condition3).as("Hasn't thread connection").isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).as("Synchronization not active").isTrue();

    final RuntimeException ex = new RuntimeException("Application exception");
    assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> tt.executeWithoutResult(status -> {
              assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).as("Has thread connection").isTrue();
              assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
              assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
              Connection con = DataSourceUtils.getConnection(dsToUse);
              if (createStatement) {
                try {
                  con.createStatement();
                }
                catch (SQLException ex1) {
                  throw new UncategorizedSQLException("", "", ex1);
                }
              }
              throw ex;
            }))
            .isEqualTo(ex);

    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();
    boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition).as("Synchronization not active").isTrue();

    if (autoCommit && (!lazyConnection || createStatement)) {
      InOrder ordered = inOrder(con);
      ordered.verify(con).setAutoCommit(false);
      ordered.verify(con).rollback();
      ordered.verify(con).setAutoCommit(true);
    }
    if (createStatement) {
      verify(con, times(2)).close();
    }
    else {
      verify(con).close();
    }
  }

  @Test
  public void testTransactionRollbackOnly() throws Exception {
    tm.setTransactionSynchronization(JdbcTransactionManager.SYNCHRONIZATION_NEVER);
    TransactionTemplate tt = new TransactionTemplate(tm);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    ConnectionHolder conHolder = new ConnectionHolder(con, true);
    TransactionSynchronizationManager.bindResource(ds, conHolder);
    final RuntimeException ex = new RuntimeException("Application exception");
    try {
      tt.executeWithoutResult(status -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
        boolean condition11 = !TransactionSynchronizationManager.isSynchronizationActive();
        assertThat(condition11).as("Synchronization not active").isTrue();
        boolean condition = !status.isNewTransaction();
        assertThat(condition).as("Is existing transaction").isTrue();
        throw ex;
      });
      fail("Should have thrown RuntimeException");
    }
    catch (RuntimeException ex2) {
      // expected
      boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
      assertThat(condition).as("Synchronization not active").isTrue();
      assertThat(ex2).as("Correct exception thrown").isEqualTo(ex);
    }
    finally {
      TransactionSynchronizationManager.unbindResource(ds);
    }

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
  }

  @Test
  public void testParticipatingTransactionWithRollbackOnly() throws Exception {
    doTestParticipatingTransactionWithRollbackOnly(false);
  }

  @Test
  public void testParticipatingTransactionWithRollbackOnlyAndFailEarly() throws Exception {
    doTestParticipatingTransactionWithRollbackOnly(true);
  }

  private void doTestParticipatingTransactionWithRollbackOnly(boolean failEarly) throws Exception {
    given(con.isReadOnly()).willReturn(false);
    if (failEarly) {
      tm.setFailEarlyOnGlobalRollbackOnly(true);
    }
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
    TestTransactionSynchronization synch =
            new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_ROLLED_BACK);
    TransactionSynchronizationManager.registerSynchronization(synch);

    boolean outerTransactionBoundaryReached = false;
    try {
      assertThat(ts.isNewTransaction()).as("Is new transaction").isTrue();

      final TransactionTemplate tt = new TransactionTemplate(tm);
      tt.executeWithoutResult(status -> {
        boolean condition11 = !status.isNewTransaction();
        assertThat(condition11).as("Is existing transaction").isTrue();
        assertThat(status.isRollbackOnly()).as("Is not rollback-only").isFalse();
        tt.executeWithoutResult(status1 -> {
          assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
          assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
          boolean condition = !status1.isNewTransaction();
          assertThat(condition).as("Is existing transaction").isTrue();
          status1.setRollbackOnly();
        });
        boolean condition = !status.isNewTransaction();
        assertThat(condition).as("Is existing transaction").isTrue();
        assertThat(status.isRollbackOnly()).as("Is rollback-only").isTrue();
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
      if (failEarly) {
        assertThat(outerTransactionBoundaryReached).isFalse();
      }
      else {
        assertThat(outerTransactionBoundaryReached).isTrue();
      }
    }

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    assertThat(synch.beforeCommitCalled).isFalse();
    assertThat(synch.beforeCompletionCalled).isTrue();
    assertThat(synch.afterCommitCalled).isFalse();
    assertThat(synch.afterCompletionCalled).isTrue();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testParticipatingTransactionWithIncompatibleIsolationLevel() throws Exception {
    tm.setValidateExistingTransaction(true);

    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() -> {
      final TransactionTemplate tt = new TransactionTemplate(tm);
      final TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

      tt.executeWithoutResult(status -> {
        assertThat(status.isRollbackOnly()).as("Is not rollback-only").isFalse();
        tt2.executeWithoutResult(TransactionExecution::setRollbackOnly);
        assertThat(status.isRollbackOnly()).as("Is rollback-only").isTrue();
      });
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testParticipatingTransactionWithIncompatibleReadOnly() throws Exception {
    willThrow(new SQLException("read-only not supported")).given(con).setReadOnly(true);
    tm.setValidateExistingTransaction(true);

    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() -> {
      final TransactionTemplate tt = new TransactionTemplate(tm);
      tt.setReadOnly(true);
      final TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setReadOnly(false);

      tt.executeWithoutResult(status -> {
        assertThat(status.isRollbackOnly()).as("Is not rollback-only").isFalse();
        tt2.executeWithoutResult(TransactionExecution::setRollbackOnly);
        assertThat(status.isRollbackOnly()).as("Is rollback-only").isTrue();
      });
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testParticipatingTransactionWithTransactionStartedFromSynch() throws Exception {
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    final TestTransactionSynchronization synch =
            new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_COMMITTED) {
              @Override
              protected void doAfterCompletion(int status) {
                super.doAfterCompletion(status);
                tt.executeWithoutResult(status1 -> {
                });
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { });
              }
            };

    tt.executeWithoutResult(status -> {
      TransactionSynchronizationManager.registerSynchronization(synch);
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    assertThat(synch.beforeCommitCalled).isTrue();
    assertThat(synch.beforeCompletionCalled).isTrue();
    assertThat(synch.afterCommitCalled).isTrue();
    assertThat(synch.afterCompletionCalled).isTrue();
    boolean condition3 = synch.afterCompletionException instanceof IllegalStateException;
    assertThat(condition3).isTrue();
    verify(con, times(2)).commit();
    verify(con, times(2)).close();
  }

  @Test
  public void testParticipatingTransactionWithDifferentConnectionObtainedFromSynch() throws Exception {
    DataSource ds2 = mock(DataSource.class);
    final Connection con2 = mock(Connection.class);
    given(ds2.getConnection()).willReturn(con2);

    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    final TransactionTemplate tt = new TransactionTemplate(tm);

    final TestTransactionSynchronization synch =
            new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_COMMITTED) {
              @Override
              protected void doAfterCompletion(int status) {
                super.doAfterCompletion(status);
                Connection con = DataSourceUtils.getConnection(ds2);
                DataSourceUtils.releaseConnection(con, ds2);
              }
            };

    tt.executeWithoutResult(status -> {
      TransactionSynchronizationManager.registerSynchronization(synch);
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    assertThat(synch.beforeCommitCalled).isTrue();
    assertThat(synch.beforeCompletionCalled).isTrue();
    assertThat(synch.afterCommitCalled).isTrue();
    assertThat(synch.afterCompletionCalled).isTrue();
    assertThat(synch.afterCompletionException).isNull();
    verify(con).commit();
    verify(con).close();
    verify(con2).close();
  }

  @Test
  public void testParticipatingTransactionWithRollbackOnlyAndInnerSynch() throws Exception {
    tm.setTransactionSynchronization(JdbcTransactionManager.SYNCHRONIZATION_NEVER);
    JdbcTransactionManager tm2 = new JdbcTransactionManager(ds);
    // tm has no synch enabled (used at outer level), tm2 has synch enabled (inner level)

    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
    final TestTransactionSynchronization synch =
            new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_UNKNOWN);

    assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() -> {
      assertThat(ts.isNewTransaction()).as("Is new transaction").isTrue();
      final TransactionTemplate tt = new TransactionTemplate(tm2);
      tt.executeWithoutResult(status -> {
        boolean condition11 = !status.isNewTransaction();
        assertThat(condition11).as("Is existing transaction").isTrue();
        assertThat(status.isRollbackOnly()).as("Is not rollback-only").isFalse();
        tt.executeWithoutResult(status1 -> {
          assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
          assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
          boolean condition = !status1.isNewTransaction();
          assertThat(condition).as("Is existing transaction").isTrue();
          status1.setRollbackOnly();
        });
        boolean condition = !status.isNewTransaction();
        assertThat(condition).as("Is existing transaction").isTrue();
        assertThat(status.isRollbackOnly()).as("Is rollback-only").isTrue();
        TransactionSynchronizationManager.registerSynchronization(synch);
      });

      tm.commit(ts);
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    assertThat(synch.beforeCommitCalled).isFalse();
    assertThat(synch.beforeCompletionCalled).isTrue();
    assertThat(synch.afterCommitCalled).isFalse();
    assertThat(synch.afterCompletionCalled).isTrue();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testPropagationRequiresNewWithExistingTransaction() throws Exception {
    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      tt.executeWithoutResult(status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
        assertThat(status1.isNewTransaction()).as("Is new transaction").isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        status1.setRollbackOnly();
      });
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback();
    verify(con).commit();
    verify(con, times(2)).close();
  }

  @Test
  public void testPropagationRequiresNewWithExistingTransactionAndUnrelatedDataSource() throws Exception {
    Connection con2 = mock(Connection.class);
    final DataSource ds2 = mock(DataSource.class);
    given(ds2.getConnection()).willReturn(con2);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    PlatformTransactionManager tm2 = new JdbcTransactionManager(ds2);
    final TransactionTemplate tt2 = new TransactionTemplate(tm2);
    tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    boolean condition4 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition4).as("Hasn't thread connection").isTrue();
    boolean condition3 = !TransactionSynchronizationManager.hasResource(ds2);
    assertThat(condition3).as("Hasn't thread connection").isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      tt2.executeWithoutResult(status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
        assertThat(status1.isNewTransaction()).as("Is new transaction").isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        status1.setRollbackOnly();
      });
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
    });

    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();
    boolean condition = !TransactionSynchronizationManager.hasResource(ds2);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).commit();
    verify(con).close();
    verify(con2).rollback();
    verify(con2).close();
  }

  @Test
  public void testPropagationRequiresNewWithExistingTransactionAndUnrelatedFailingDataSource() throws Exception {
    final DataSource ds2 = mock(DataSource.class);
    SQLException failure = new SQLException();
    given(ds2.getConnection()).willThrow(failure);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    JdbcTransactionManager tm2 = new JdbcTransactionManager(ds2);
    tm2.setTransactionSynchronization(JdbcTransactionManager.SYNCHRONIZATION_NEVER);
    final TransactionTemplate tt2 = new TransactionTemplate(tm2);
    tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    boolean condition4 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition4).as("Hasn't thread connection").isTrue();
    boolean condition3 = !TransactionSynchronizationManager.hasResource(ds2);
    assertThat(condition3).as("Hasn't thread connection").isTrue();
    boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition2).as("Synchronization not active").isTrue();

    assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
              assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
              assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
              tt2.executeWithoutResult(TransactionExecution::setRollbackOnly);
            })).withCause(failure);

    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();
    boolean condition = !TransactionSynchronizationManager.hasResource(ds2);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testPropagationNotSupportedWithExistingTransaction() throws Exception {
    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
      tt.executeWithoutResult(status1 -> {
        boolean condition11 = !TransactionSynchronizationManager.hasResource(ds);
        assertThat(condition11).as("Hasn't thread connection").isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
        boolean condition = !status1.isNewTransaction();
        assertThat(condition).as("Isn't new transaction").isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        status1.setRollbackOnly();
      });
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testPropagationNeverWithExistingTransaction() throws Exception {
    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
              tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
              tt.executeWithoutResult(status1 -> {
                fail("Should have thrown IllegalTransactionStateException");
              });
              fail("Should have thrown IllegalTransactionStateException");
            }));

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testPropagationSupportsAndRequiresNew() throws Exception {
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
      TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      tt2.executeWithoutResult(status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
        assertThat(status1.isNewTransaction()).as("Is new transaction").isTrue();
        assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con);
        assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con);
      });
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testPropagationSupportsAndRequiresNewWithEarlyAccess() throws Exception {
    final Connection con1 = mock(Connection.class);
    final Connection con2 = mock(Connection.class);
    given(ds.getConnection()).willReturn(con1, con2);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
      assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con1);
      assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con1);
      TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      tt2.executeWithoutResult(status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
        assertThat(status1.isNewTransaction()).as("Is new transaction").isTrue();
        assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con2);
        assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con2);
      });
      assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con1);
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con1).close();
    verify(con2).commit();
    verify(con2).close();
  }

  @Test
  public void testTransactionWithIsolationAndReadOnly() throws Exception {
    given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
    given(con.getAutoCommit()).willReturn(true);

    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    tt.setReadOnly(true);
    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      // something transactional
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    InOrder ordered = inOrder(con);
    ordered.verify(con).setReadOnly(true);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).commit();
    ordered.verify(con).setAutoCommit(true);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    ordered.verify(con).setReadOnly(false);
    verify(con).close();
  }

  @Test
  public void testTransactionWithEnforceReadOnly() throws Exception {
    tm.setEnforceReadOnly(true);

    given(con.getAutoCommit()).willReturn(true);
    Statement stmt = mock(Statement.class);
    given(con.createStatement()).willReturn(stmt);

    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    tt.setReadOnly(true);
    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      // something transactional
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    InOrder ordered = inOrder(con, stmt);
    ordered.verify(con).setReadOnly(true);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(stmt).executeUpdate("SET TRANSACTION READ ONLY");
    ordered.verify(stmt).close();
    ordered.verify(con).commit();
    ordered.verify(con).setAutoCommit(true);
    ordered.verify(con).setReadOnly(false);
    ordered.verify(con).close();
  }

  @ParameterizedTest(name = "transaction with {0} second timeout")
  @ValueSource(ints = { 1, 10 })
  public void transactionWithTimeout(int timeout) throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);
    given(con.getAutoCommit()).willReturn(true);
    given(con.prepareStatement("some SQL statement")).willReturn(ps);

    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setTimeout(timeout);
    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();

    try {
      tt.executeWithoutResult(status -> {
        try {
          Thread.sleep(1500);
        }
        catch (InterruptedException ex) {
        }
        try {
          Connection con = DataSourceUtils.getConnection(ds);
          PreparedStatement ps1 = con.prepareStatement("some SQL statement");
          DataSourceUtils.applyTransactionTimeout(ps1, ds);
        }
        catch (SQLException ex) {
          throw new DataAccessResourceFailureException("", ex);
        }
      });
      if (timeout <= 1) {
        fail("Should have thrown TransactionTimedOutException");
      }
    }
    catch (TransactionTimedOutException ex) {
      if (timeout <= 1) {
        // expected
      }
      else {
        throw ex;
      }
    }

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    if (timeout > 1) {
      verify(ps).setQueryTimeout(timeout - 1);
      verify(con).commit();
    }
    else {
      verify(con).rollback();
    }
    InOrder ordered = inOrder(con);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).setAutoCommit(true);
    verify(con).close();
  }

  @Test
  public void testTransactionAwareDataSourceProxy() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    given(con.getWarnings()).willThrow(new SQLException());

    TransactionTemplate tt = new TransactionTemplate(tm);
    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();
    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
      TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
      try {
        Connection tCon = dsProxy.getConnection();
        tCon.getWarnings();
        tCon.clearWarnings();
        assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
        // should be ignored
        dsProxy.getConnection().close();
      }
      catch (SQLException ex) {
        throw new UncategorizedSQLException("", "", ex);
      }
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    InOrder ordered = inOrder(con);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).commit();
    ordered.verify(con).setAutoCommit(true);
    verify(con).close();
  }

  @Test
  public void testTransactionAwareDataSourceProxyWithSuspension() throws Exception {
    given(con.getAutoCommit()).willReturn(true);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();

    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
      final TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
      try {
        assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
        // should be ignored
        dsProxy.getConnection().close();
      }
      catch (SQLException ex) {
        throw new UncategorizedSQLException("", "", ex);
      }

      tt.executeWithoutResult(status1 -> {
        // something transactional
        assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
        try {
          assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
          // should be ignored
          dsProxy.getConnection().close();
        }
        catch (SQLException ex) {
          throw new UncategorizedSQLException("", "", ex);
        }
      });

      try {
        assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
        // should be ignored
        dsProxy.getConnection().close();
      }
      catch (SQLException ex) {
        throw new UncategorizedSQLException("", "", ex);
      }
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    InOrder ordered = inOrder(con);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).commit();
    ordered.verify(con).setAutoCommit(true);
    verify(con, times(2)).close();
  }

  @Test
  public void testTransactionAwareDataSourceProxyWithSuspensionAndReobtaining() throws Exception {
    given(con.getAutoCommit()).willReturn(true);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();

    tt.executeWithoutResult(status -> {
      // something transactional
      assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
      final TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
      dsProxy.setReobtainTransactionalConnections(true);
      try {
        assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
        // should be ignored
        dsProxy.getConnection().close();
      }
      catch (SQLException ex) {
        throw new UncategorizedSQLException("", "", ex);
      }

      tt.executeWithoutResult(status1 -> {
        // something transactional
        assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
        try {
          assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
          // should be ignored
          dsProxy.getConnection().close();
        }
        catch (SQLException ex) {
          throw new UncategorizedSQLException("", "", ex);
        }
      });

      try {
        assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
        // should be ignored
        dsProxy.getConnection().close();
      }
      catch (SQLException ex) {
        throw new UncategorizedSQLException("", "", ex);
      }
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    InOrder ordered = inOrder(con);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).commit();
    ordered.verify(con).setAutoCommit(true);
    verify(con, times(2)).close();
  }

  /**
   * Test behavior if the first operation on a connection (getAutoCommit) throws SQLException.
   */
  @Test
  public void testTransactionWithExceptionOnBegin() throws Exception {
    willThrow(new SQLException("Cannot begin")).given(con).getAutoCommit();

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              // something transactional
            }));

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).close();
  }

  @Test
  public void testTransactionWithExceptionOnCommit() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              // something transactional
            }));

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).close();
  }

  @Test
  public void testTransactionWithDataAccessExceptionOnCommit() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();
    tm.setExceptionTranslator((task, sql, ex) -> new ConcurrencyFailureException(task));

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              // something transactional
            }));

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).close();
  }

  @Test
  public void testTransactionWithDataAccessExceptionOnCommitFromLazyExceptionTranslator() throws Exception {
    willThrow(new SQLException("Cannot commit", "40")).given(con).commit();

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              // something transactional
            }));

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).close();
  }

  @Test
  public void testTransactionWithExceptionOnCommitAndRollbackOnCommitFailure() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();

    tm.setRollbackOnCommitFailure(true);
    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              // something transactional
            }));

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testTransactionWithExceptionOnRollback() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    willThrow(new SQLException("Cannot rollback")).given(con).rollback();

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.executeWithoutResult(TransactionExecution::setRollbackOnly));

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    InOrder ordered = inOrder(con);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).rollback();
    ordered.verify(con).setAutoCommit(true);
    verify(con).close();
  }

  @Test
  public void testTransactionWithDataAccessExceptionOnRollback() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    willThrow(new SQLException("Cannot rollback")).given(con).rollback();
    tm.setExceptionTranslator((task, sql, ex) -> new ConcurrencyFailureException(task));

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.executeWithoutResult(TransactionExecution::setRollbackOnly));

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    InOrder ordered = inOrder(con);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).rollback();
    ordered.verify(con).setAutoCommit(true);
    verify(con).close();
  }

  @Test
  public void testTransactionWithDataAccessExceptionOnRollbackFromLazyExceptionTranslator() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    willThrow(new SQLException("Cannot rollback", "40")).given(con).rollback();

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.executeWithoutResult(TransactionExecution::setRollbackOnly));

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    InOrder ordered = inOrder(con);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).rollback();
    ordered.verify(con).setAutoCommit(true);
    verify(con).close();
  }

  @Test
  public void testTransactionWithPropagationSupports() throws Exception {
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();

    tt.executeWithoutResult(status -> {
      boolean condition11 = !TransactionSynchronizationManager.hasResource(ds);
      assertThat(condition11).as("Hasn't thread connection").isTrue();
      boolean condition = !status.isNewTransaction();
      assertThat(condition).as("Is not new transaction").isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
  }

  @Test
  public void testTransactionWithPropagationNotSupported() throws Exception {
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();

    tt.executeWithoutResult(status -> {
      boolean condition11 = !TransactionSynchronizationManager.hasResource(ds);
      assertThat(condition11).as("Hasn't thread connection").isTrue();
      boolean condition = !status.isNewTransaction();
      assertThat(condition).as("Is not new transaction").isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
  }

  @Test
  public void testTransactionWithPropagationNever() throws Exception {
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
    boolean condition1 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition1).as("Hasn't thread connection").isTrue();

    tt.executeWithoutResult(status -> {
      boolean condition11 = !TransactionSynchronizationManager.hasResource(ds);
      assertThat(condition11).as("Hasn't thread connection").isTrue();
      boolean condition = !status.isNewTransaction();
      assertThat(condition).as("Is not new transaction").isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
  }

  @Test
  public void testExistingTransactionWithPropagationNested() throws Exception {
    doTestExistingTransactionWithPropagationNested(1);
  }

  @Test
  public void testExistingTransactionWithPropagationNestedTwice() throws Exception {
    doTestExistingTransactionWithPropagationNested(2);
  }

  private void doTestExistingTransactionWithPropagationNested(final int count) throws Exception {
    DatabaseMetaData md = mock(DatabaseMetaData.class);
    Savepoint sp = mock(Savepoint.class);

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    for (int i = 1; i <= count; i++) {
      given(con.setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + i)).willReturn(sp);
    }

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      boolean condition11 = !status.hasSavepoint();
      assertThat(condition11).as("Isn't nested transaction").isTrue();
      for (int i = 0; i < count; i++) {
        tt.executeWithoutResult(status1 -> {
          assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
          assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
          boolean condition = !status1.isNewTransaction();
          assertThat(condition).as("Isn't new transaction").isTrue();
          assertThat(status1.hasSavepoint()).as("Is nested transaction").isTrue();
        });
      }
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      boolean condition = !status.hasSavepoint();
      assertThat(condition).as("Isn't nested transaction").isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con, times(count)).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testExistingTransactionWithPropagationNestedAndRollback() throws Exception {
    DatabaseMetaData md = mock(DatabaseMetaData.class);
    Savepoint sp = mock(Savepoint.class);

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      boolean condition11 = !status.hasSavepoint();
      assertThat(condition11).as("Isn't nested transaction").isTrue();
      tt.executeWithoutResult(status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
        boolean condition = !status1.isNewTransaction();
        assertThat(condition).as("Isn't new transaction").isTrue();
        assertThat(status1.hasSavepoint()).as("Is nested transaction").isTrue();
        status1.setRollbackOnly();
      });
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      boolean condition = !status.hasSavepoint();
      assertThat(condition).as("Isn't nested transaction").isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback(sp);
    verify(con).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testExistingTransactionWithPropagationNestedAndRequiredRollback() throws Exception {
    DatabaseMetaData md = mock(DatabaseMetaData.class);
    Savepoint sp = mock(Savepoint.class);

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      boolean condition11 = !status.hasSavepoint();
      assertThat(condition11).as("Isn't nested transaction").isTrue();
      assertThatIllegalStateException().isThrownBy(() ->
              tt.executeWithoutResult(status1 -> {
                assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
                assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
                boolean condition = !status1.isNewTransaction();
                assertThat(condition).as("Isn't new transaction").isTrue();
                assertThat(status1.hasSavepoint()).as("Is nested transaction").isTrue();
                TransactionTemplate ntt = new TransactionTemplate(tm);
                ntt.executeWithoutResult(status11 -> {
                  assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
                  assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
                  boolean condition111 = !status11.isNewTransaction();
                  assertThat(condition111).as("Isn't new transaction").isTrue();
                  boolean condition3 = !status11.hasSavepoint();
                  assertThat(condition3).as("Is regular transaction").isTrue();
                  throw new IllegalStateException();
                });
              }));
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      boolean condition = !status.hasSavepoint();
      assertThat(condition).as("Isn't nested transaction").isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback(sp);
    verify(con).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testExistingTransactionWithPropagationNestedAndRequiredRollbackOnly() throws Exception {
    DatabaseMetaData md = mock(DatabaseMetaData.class);
    Savepoint sp = mock(Savepoint.class);

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      boolean condition11 = !status.hasSavepoint();
      assertThat(condition11).as("Isn't nested transaction").isTrue();
      assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() ->
              tt.executeWithoutResult(status12 -> {
                assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
                assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
                boolean condition = !status12.isNewTransaction();
                assertThat(condition).as("Isn't new transaction").isTrue();
                assertThat(status12.hasSavepoint()).as("Is nested transaction").isTrue();
                TransactionTemplate ntt = new TransactionTemplate(tm);
                ntt.executeWithoutResult(status1 -> {
                  assertThat(TransactionSynchronizationManager.hasResource(ds)).as("Has thread connection").isTrue();
                  assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
                  boolean condition111 = !status1.isNewTransaction();
                  assertThat(condition111).as("Isn't new transaction").isTrue();
                  boolean condition3 = !status1.hasSavepoint();
                  assertThat(condition3).as("Is regular transaction").isTrue();
                  status1.setRollbackOnly();
                });
              }));
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      boolean condition = !status.hasSavepoint();
      assertThat(condition).as("Isn't nested transaction").isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback(sp);
    verify(con).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testExistingTransactionWithManualSavepoint() throws Exception {
    DatabaseMetaData md = mock(DatabaseMetaData.class);
    Savepoint sp = mock(Savepoint.class);

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      Object savepoint = status.createSavepoint();
      status.releaseSavepoint(savepoint);
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
    verify(ds).getConnection();
  }

  @Test
  public void testExistingTransactionWithManualSavepointAndRollback() throws Exception {
    DatabaseMetaData md = mock(DatabaseMetaData.class);
    Savepoint sp = mock(Savepoint.class);

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      Object savepoint = status.createSavepoint();
      status.rollbackToSavepoint(savepoint);
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testTransactionWithPropagationNested() throws Exception {
    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testTransactionWithPropagationNestedAndRollback() throws Exception {
    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    boolean condition2 = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition2).as("Hasn't thread connection").isTrue();
    boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
    assertThat(condition1).as("Synchronization not active").isTrue();

    tt.executeWithoutResult(status -> {
      assertThat(status.isNewTransaction()).as("Is new transaction").isTrue();
      status.setRollbackOnly();
    });

    boolean condition = !TransactionSynchronizationManager.hasResource(ds);
    assertThat(condition).as("Hasn't thread connection").isTrue();
    verify(con).rollback();
    verify(con).close();
  }

  private static class TestTransactionSynchronization implements TransactionSynchronization {

    private DataSource dataSource;

    private int status;

    public boolean beforeCommitCalled;

    public boolean beforeCompletionCalled;

    public boolean afterCommitCalled;

    public boolean afterCompletionCalled;

    public Throwable afterCompletionException;

    public TestTransactionSynchronization(DataSource dataSource, int status) {
      this.dataSource = dataSource;
      this.status = status;
    }

    @Override
    public void suspend() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void beforeCommit(boolean readOnly) {
      if (this.status != STATUS_COMMITTED) {
        fail("Should never be called");
      }
      assertThat(this.beforeCommitCalled).isFalse();
      this.beforeCommitCalled = true;
    }

    @Override
    public void beforeCompletion() {
      assertThat(this.beforeCompletionCalled).isFalse();
      this.beforeCompletionCalled = true;
    }

    @Override
    public void afterCommit() {
      if (this.status != STATUS_COMMITTED) {
        fail("Should never be called");
      }
      assertThat(this.afterCommitCalled).isFalse();
      this.afterCommitCalled = true;
    }

    @Override
    public void afterCompletion(int status) {
      try {
        doAfterCompletion(status);
      }
      catch (Throwable ex) {
        this.afterCompletionException = ex;
      }
    }

    protected void doAfterCompletion(int status) {
      assertThat(this.afterCompletionCalled).isFalse();
      this.afterCompletionCalled = true;
      assertThat(status == this.status).isTrue();
      assertThat(TransactionSynchronizationManager.hasResource(this.dataSource)).isTrue();
    }
  }

}
