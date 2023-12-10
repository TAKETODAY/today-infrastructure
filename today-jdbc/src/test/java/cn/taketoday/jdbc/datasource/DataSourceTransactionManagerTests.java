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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.datasource;

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

import cn.taketoday.core.testfixture.EnabledForTestGroups;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.jdbc.UncategorizedSQLException;
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
import cn.taketoday.transaction.support.TransactionCallbackWithoutResult;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;

import static cn.taketoday.core.testfixture.TestGroup.LONG_RUNNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Juergen Hoeller
 * @see cn.taketoday.jdbc.support.JdbcTransactionManagerTests
 * @since 04.07.2003
 */
public class DataSourceTransactionManagerTests<T extends DataSourceTransactionManager> {

  protected DataSource ds = mock();

  protected Connection con = mock();

  protected DataSourceTransactionManager tm;

  @BeforeEach
  public void setup() throws Exception {
    tm = createTransactionManager(ds);
    given(ds.getConnection()).willReturn(con);
  }

  protected DataSourceTransactionManager createTransactionManager(DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }

  @AfterEach
  public void verifyTransactionSynchronizationManagerState() {
    assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
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

    given(con.getAutoCommit()).willReturn(autoCommit);

    if (lazyConnection) {
      given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
      given(con.getWarnings()).willThrow(new SQLException());
    }

    final DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(ds) : ds);
    tm = createTransactionManager(dsToUse);
    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
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

    assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

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

    given(con.getAutoCommit()).willReturn(autoCommit);

    if (lazyConnection) {
      given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
    }

    final DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(ds) : ds);
    tm = createTransactionManager(dsToUse);
    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    final RuntimeException ex = new RuntimeException("Application exception");
    assertThatRuntimeException().isThrownBy(() ->
                    tt.execute((TransactionCallbackWithoutResult) status -> {
                      assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
                      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
                      assertThat(status.isNewTransaction()).isTrue();
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

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

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
    tm.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    ConnectionHolder conHolder = new ConnectionHolder(con, true);
    TransactionSynchronizationManager.bindResource(ds, conHolder);
    final RuntimeException ex = new RuntimeException("Application exception");
    try {
      tt.execute((TransactionCallbackWithoutResult) status -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
        assertThat(status.isNewTransaction()).isFalse();
        throw ex;
      });
      fail("Should have thrown RuntimeException");
    }
    catch (RuntimeException ex2) {
      // expected
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
      assertThat(ex2).as("Correct exception thrown").isEqualTo(ex);
    }
    finally {
      TransactionSynchronizationManager.unbindResource(ds);
    }

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
    TestTransactionSynchronization synch =
            new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_ROLLED_BACK);
    TransactionSynchronizationManager.registerSynchronization(synch);

    boolean outerTransactionBoundaryReached = false;
    try {
      assertThat(ts.isNewTransaction()).isTrue();

      final TransactionTemplate tt = new TransactionTemplate(tm);
      tt.execute((TransactionCallbackWithoutResult) status -> {
        assertThat(status.isNewTransaction()).isFalse();
        assertThat(status.isRollbackOnly()).isFalse();
        tt.execute((TransactionCallbackWithoutResult) status1 -> {
          assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
          assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
          assertThat(status1.isNewTransaction()).isFalse();
          status1.setRollbackOnly();
        });
        assertThat(status.isNewTransaction()).isFalse();
        assertThat(status.isRollbackOnly()).isTrue();
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

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() -> {
      final TransactionTemplate tt = new TransactionTemplate(tm);
      final TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

      tt.execute((TransactionCallbackWithoutResult) status -> {
        assertThat(status.isRollbackOnly()).isFalse();
        tt2.execute((TransactionCallbackWithoutResult) TransactionExecution::setRollbackOnly);
        assertThat(status.isRollbackOnly()).isTrue();
      });
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testParticipatingTransactionWithIncompatibleReadOnly() throws Exception {
    willThrow(new SQLException("read-only not supported")).given(con).setReadOnly(true);
    tm.setValidateExistingTransaction(true);

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() -> {
      final TransactionTemplate tt = new TransactionTemplate(tm);
      tt.setReadOnly(true);
      final TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setReadOnly(false);

      tt.executeWithoutResult(status -> {
        assertThat(status.isRollbackOnly()).isFalse();
        tt2.executeWithoutResult(TransactionExecution::setRollbackOnly);
        assertThat(status.isRollbackOnly()).isTrue();
      });
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testParticipatingTransactionWithTransactionStartedFromSynch() throws Exception {
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

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

    tt.executeWithoutResult(status -> TransactionSynchronizationManager.registerSynchronization(synch));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(synch.beforeCommitCalled).isTrue();
    assertThat(synch.beforeCompletionCalled).isTrue();
    assertThat(synch.afterCommitCalled).isTrue();
    assertThat(synch.afterCompletionCalled).isTrue();
    assertThat(synch.afterCompletionException).isInstanceOf(IllegalStateException.class);
    verify(con, times(2)).commit();
    verify(con, times(2)).close();
  }

  @Test
  public void testParticipatingTransactionWithDifferentConnectionObtainedFromSynch() throws Exception {
    DataSource ds2 = mock();
    final Connection con2 = mock();
    given(ds2.getConnection()).willReturn(con2);

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

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

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
    tm.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
    DataSourceTransactionManager tm2 = createTransactionManager(ds);
    // tm has no synch enabled (used at outer level), tm2 has synch enabled (inner level)

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
    final TestTransactionSynchronization synch =
            new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_UNKNOWN);

    assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() -> {
      assertThat(ts.isNewTransaction()).isTrue();
      final TransactionTemplate tt = new TransactionTemplate(tm2);
      tt.execute((TransactionCallbackWithoutResult) status -> {
        assertThat(status.isNewTransaction()).isFalse();
        assertThat(status.isRollbackOnly()).isFalse();
        tt.execute((TransactionCallbackWithoutResult) status1 -> {
          assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
          assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
          assertThat(status1.isNewTransaction()).isFalse();
          status1.setRollbackOnly();
        });
        assertThat(status.isNewTransaction()).isFalse();
        assertThat(status.isRollbackOnly()).isTrue();
        TransactionSynchronizationManager.registerSynchronization(synch);
      });

      tm.commit(ts);
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      tt.execute((TransactionCallbackWithoutResult) status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(status1.isNewTransaction()).isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        status1.setRollbackOnly();
      });
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback();
    verify(con).commit();
    verify(con, times(2)).close();
  }

  @Test
  public void testPropagationRequiresNewWithExistingTransactionAndUnrelatedDataSource() throws Exception {
    Connection con2 = mock();
    final DataSource ds2 = mock();
    given(ds2.getConnection()).willReturn(con2);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    PlatformTransactionManager tm2 = createTransactionManager(ds2);
    final TransactionTemplate tt2 = new TransactionTemplate(tm2);
    tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.hasResource(ds2)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      tt2.execute((TransactionCallbackWithoutResult) status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(status1.isNewTransaction()).isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        status1.setRollbackOnly();
      });
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.hasResource(ds2)).isFalse();
    verify(con).commit();
    verify(con).close();
    verify(con2).rollback();
    verify(con2).close();
  }

  @Test
  public void testPropagationRequiresNewWithExistingTransactionAndUnrelatedFailingDataSource() throws Exception {
    final DataSource ds2 = mock();
    SQLException failure = new SQLException();
    given(ds2.getConnection()).willThrow(failure);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    DataSourceTransactionManager tm2 = createTransactionManager(ds2);
    tm2.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
    final TransactionTemplate tt2 = new TransactionTemplate(tm2);
    tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.hasResource(ds2)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) status -> {
              assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
              assertThat(status.hasTransaction()).isTrue();
              assertThat(status.isNewTransaction()).isTrue();
              assertThat(status.isNested()).isFalse();
              assertThat(status.isReadOnly()).isFalse();
              assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
              tt2.execute((TransactionCallbackWithoutResult) status1 -> {
                status1.setRollbackOnly();
              });
            })).withCause(failure);

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.hasResource(ds2)).isFalse();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testPropagationNotSupportedWithExistingTransaction() throws Exception {
    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
      tt.execute((TransactionCallbackWithoutResult) status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(status1.hasTransaction()).isFalse();
        assertThat(status1.isNewTransaction()).isFalse();
        assertThat(status1.isNested()).isFalse();
        assertThat(status1.isReadOnly()).isFalse();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        status1.setRollbackOnly();
      });
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testPropagationNeverWithExistingTransaction() throws Exception {
    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) status -> {
              assertThat(status.hasTransaction()).isTrue();
              assertThat(status.isNewTransaction()).isTrue();
              assertThat(status.isNested()).isFalse();
              tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
              tt.execute((TransactionCallbackWithoutResult) status1 -> {
                fail("Should have thrown IllegalTransactionStateException");
              });
              fail("Should have thrown IllegalTransactionStateException");
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testPropagationSupportsAndRequiresNew() throws Exception {
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(status.hasTransaction()).isFalse();
      assertThat(status.isNewTransaction()).isFalse();
      assertThat(status.isNested()).isFalse();
      TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      tt2.execute((TransactionCallbackWithoutResult) status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(status1.hasTransaction()).isTrue();
        assertThat(status1.isNewTransaction()).isTrue();
        assertThat(status1.isNested()).isFalse();
        assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con);
        assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con);
      });
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testPropagationSupportsAndRequiresNewWithEarlyAccess() throws Exception {
    final Connection con1 = mock();
    final Connection con2 = mock();
    given(ds.getConnection()).willReturn(con1, con2);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con1);
      assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con1);
      assertThat(status.hasTransaction()).isFalse();
      assertThat(status.isNewTransaction()).isFalse();
      assertThat(status.isNested()).isFalse();
      TransactionTemplate tt2 = new TransactionTemplate(tm);
      tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      tt2.execute((TransactionCallbackWithoutResult) status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con2);
        assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con2);
        assertThat(status1.hasTransaction()).isTrue();
        assertThat(status1.isNewTransaction()).isTrue();
        assertThat(status1.isNested()).isFalse();
      });
      assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con1);
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con1).close();
    verify(con2).commit();
    verify(con2).close();
  }

  @Test
  public void testTransactionWithIsolationAndReadOnly() throws Exception {
    given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_UNCOMMITTED);
    given(con.getAutoCommit()).willReturn(true);

    doTestTransactionReadOnly(TransactionDefinition.ISOLATION_REPEATABLE_READ, false);

    InOrder ordered = inOrder(con);
    ordered.verify(con).setReadOnly(true);
    ordered.verify(con).getTransactionIsolation();
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    ordered.verify(con).getAutoCommit();
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).commit();
    ordered.verify(con).setAutoCommit(true);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    ordered.verify(con).setReadOnly(false);
    ordered.verify(con).close();
    verifyNoMoreInteractions(con);
  }

  @Test
  public void testTransactionWithEnforceReadOnly() throws Exception {
    tm.setEnforceReadOnly(true);

    given(con.getAutoCommit()).willReturn(true);
    Statement stmt = mock();
    given(con.createStatement()).willReturn(stmt);

    doTestTransactionReadOnly(TransactionDefinition.ISOLATION_DEFAULT, false);

    InOrder ordered = inOrder(con, stmt);
    ordered.verify(con).setReadOnly(true);
    ordered.verify(con).getAutoCommit();
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).createStatement();
    ordered.verify(stmt).executeUpdate("SET TRANSACTION READ ONLY");
    ordered.verify(stmt).close();
    ordered.verify(con).commit();
    ordered.verify(con).setAutoCommit(true);
    ordered.verify(con).setReadOnly(false);
    ordered.verify(con).close();
    verifyNoMoreInteractions(con);
  }

  @Test
  public void testTransactionWithLazyConnectionDataSourceAndStatement() throws Exception {
    LazyConnectionDataSourceProxy dsProxy = new LazyConnectionDataSourceProxy();
    dsProxy.setTargetDataSource(ds);
    dsProxy.setDefaultAutoCommit(true);
    dsProxy.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    tm = createTransactionManager(dsProxy);

    doTestTransactionReadOnly(TransactionDefinition.ISOLATION_SERIALIZABLE, true);

    InOrder ordered = inOrder(con);
    ordered.verify(con).setReadOnly(true);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).createStatement();
    ordered.verify(con).commit();
    ordered.verify(con).setAutoCommit(true);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    ordered.verify(con).setReadOnly(false);
    ordered.verify(con).close();
    verifyNoMoreInteractions(con);
  }

  @Test
  public void testTransactionWithLazyConnectionDataSourceNoStatement() throws Exception {
    LazyConnectionDataSourceProxy dsProxy = new LazyConnectionDataSourceProxy();
    dsProxy.setTargetDataSource(ds);
    dsProxy.setDefaultAutoCommit(true);
    dsProxy.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    tm = createTransactionManager(dsProxy);

    doTestTransactionReadOnly(TransactionDefinition.ISOLATION_SERIALIZABLE, false);

    verifyNoMoreInteractions(con);
  }

  @Test
  public void testTransactionWithReadOnlyDataSourceAndStatement() throws Exception {
    LazyConnectionDataSourceProxy dsProxy = new LazyConnectionDataSourceProxy();
    dsProxy.setReadOnlyDataSource(ds);
    dsProxy.setDefaultAutoCommit(false);
    dsProxy.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    tm = createTransactionManager(dsProxy);

    doTestTransactionReadOnly(TransactionDefinition.ISOLATION_SERIALIZABLE, true);

    InOrder ordered = inOrder(con);
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    ordered.verify(con).createStatement();
    ordered.verify(con).commit();
    ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    ordered.verify(con).close();
    verifyNoMoreInteractions(con);
  }

  @Test
  public void testTransactionWithReadOnlyDataSourceNoStatement() throws Exception {
    LazyConnectionDataSourceProxy dsProxy = new LazyConnectionDataSourceProxy();
    dsProxy.setReadOnlyDataSource(ds);
    dsProxy.setDefaultAutoCommit(false);
    dsProxy.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    tm = createTransactionManager(dsProxy);

    doTestTransactionReadOnly(TransactionDefinition.ISOLATION_SERIALIZABLE, false);

    verifyNoMoreInteractions(con);
  }

  private void doTestTransactionReadOnly(int isolationLevel, boolean withStatement) {
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    tt.setIsolationLevel(isolationLevel);
    tt.setReadOnly(true);
    tt.setName("my-transaction");

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      // something transactional
      assertThat(status.getTransactionName()).isEqualTo("my-transaction");
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
      assertThat(status.isReadOnly()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
      assertThat(status.isRollbackOnly()).isFalse();
      assertThat(status.isCompleted()).isFalse();
      if (withStatement) {
        try {
          DataSourceUtils.getConnection(tm.getDataSource()).createStatement();
        }
        catch (SQLException ex) {
          throw new IllegalStateException(ex);
        }
      }
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
  }

  @ParameterizedTest(name = "transaction with {0} second timeout")
  @ValueSource(ints = { 1, 10 })
  @EnabledForTestGroups(LONG_RUNNING)
  public void transactionWithTimeout(int timeout) throws Exception {
    PreparedStatement ps = mock();
    given(con.getAutoCommit()).willReturn(true);
    given(con.prepareStatement("some SQL statement")).willReturn(ps);

    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setTimeout(timeout);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

    try {
      tt.execute((TransactionCallbackWithoutResult) status -> {
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

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    tt.execute((TransactionCallbackWithoutResult) status -> {
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

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    InOrder ordered = inOrder(con);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).commit();
    ordered.verify(con).setAutoCommit(true);
    verify(con).close();
  }

  @Test
  public void testTransactionAwareDataSourceProxyWithLazyFalse() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    given(con.getWarnings()).willThrow(new SQLException());

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    tt.execute((TransactionCallbackWithoutResult) status -> {
      // something transactional
      assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
      TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
      dsProxy.setLazyTransactionalConnections(false);
      try {
        Connection tCon = dsProxy.getConnection();
        assertThatExceptionOfType(SQLException.class).isThrownBy(tCon::getWarnings);
        tCon.clearWarnings();
        assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
        // should be ignored
        dsProxy.getConnection().close();
      }
      catch (SQLException ex) {
        throw new UncategorizedSQLException("", "", ex);
      }
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
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

      tt.execute((TransactionCallbackWithoutResult) status1 -> {
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

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
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

      tt.execute((TransactionCallbackWithoutResult) status1 -> {
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

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
            tt.execute((TransactionCallbackWithoutResult) status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).close();
  }

  @Test
  public void testTransactionWithExceptionOnCommit() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).close();
  }

  @Test
  public void testTransactionWithExceptionOnCommitAndRollbackOnCommitFailure() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();

    tm.setRollbackOnCommitFailure(true);
    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback();
    verify(con).close();
  }

  @Test
  public void testTransactionWithExceptionOnRollback() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    willThrow(new SQLException("Cannot rollback")).given(con).rollback();

    TransactionTemplate tt = new TransactionTemplate(tm);
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) status -> {
              assertThat(status.getTransactionName()).isEmpty();
              assertThat(status.hasTransaction()).isTrue();
              assertThat(status.isNewTransaction()).isTrue();
              assertThat(status.isNested()).isFalse();
              assertThat(status.hasSavepoint()).isFalse();
              assertThat(status.isReadOnly()).isFalse();
              assertThat(status.isRollbackOnly()).isFalse();
              status.setRollbackOnly();
              assertThat(status.isRollbackOnly()).isTrue();
              assertThat(status.isCompleted()).isFalse();
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
      assertThat(status.isNewTransaction()).isFalse();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
  }

  @Test
  public void testTransactionWithPropagationNotSupported() throws Exception {
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
      assertThat(status.isNewTransaction()).isFalse();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
  }

  @Test
  public void testTransactionWithPropagationNever() throws Exception {
    TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
      assertThat(status.isNewTransaction()).isFalse();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
    DatabaseMetaData md = mock();
    Savepoint sp = mock();

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    for (int i = 1; i <= count; i++) {
      given(con.setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + i)).willReturn(sp);
    }

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
      for (int i = 0; i < count; i++) {
        tt.execute((TransactionCallbackWithoutResult) status1 -> {
          assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
          assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
          assertThat(status1.hasTransaction()).isTrue();
          assertThat(status1.isNewTransaction()).isFalse();
          assertThat(status1.isNested()).isTrue();
          assertThat(status1.hasSavepoint()).isTrue();
        });
      }
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con, times(count)).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testExistingTransactionWithPropagationNestedAndRollback() throws Exception {
    DatabaseMetaData md = mock();
    Savepoint sp = mock();

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
      tt.execute((TransactionCallbackWithoutResult) status1 -> {
        assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(status1.hasTransaction()).isTrue();
        assertThat(status1.isNewTransaction()).isFalse();
        assertThat(status1.isNested()).isTrue();
        assertThat(status1.hasSavepoint()).isTrue();
        status1.setRollbackOnly();
      });
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback(sp);
    verify(con).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testExistingTransactionWithPropagationNestedAndRequiredRollback() throws Exception {
    DatabaseMetaData md = mock();
    Savepoint sp = mock();

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
      assertThatIllegalStateException().isThrownBy(() ->
              tt.execute((TransactionCallbackWithoutResult) status1 -> {
                assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
                assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
                assertThat(status1.hasTransaction()).isTrue();
                assertThat(status1.isNewTransaction()).isFalse();
                assertThat(status1.isNested()).isTrue();
                assertThat(status1.hasSavepoint()).isTrue();
                TransactionTemplate ntt = new TransactionTemplate(tm);
                ntt.execute((TransactionCallbackWithoutResult) status11 -> {
                  assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
                  assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
                  assertThat(status11.hasTransaction()).isTrue();
                  assertThat(status11.isNewTransaction()).isFalse();
                  assertThat(status11.isNested()).isFalse();
                  assertThat(status11.hasSavepoint()).isFalse();
                  throw new IllegalStateException();
                });
              }));
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback(sp);
    verify(con).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testExistingTransactionWithPropagationNestedAndRequiredRollbackOnly() throws Exception {
    DatabaseMetaData md = mock();
    Savepoint sp = mock();

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
      assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() ->
              tt.execute((TransactionCallbackWithoutResult) status12 -> {
                assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
                assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
                assertThat(status12.hasTransaction()).isTrue();
                assertThat(status12.isNewTransaction()).isFalse();
                assertThat(status12.isNested()).isTrue();
                assertThat(status12.hasSavepoint()).isTrue();
                TransactionTemplate ntt = new TransactionTemplate(tm);
                ntt.execute((TransactionCallbackWithoutResult) status1 -> {
                  assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
                  assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
                  assertThat(status1.hasTransaction()).isTrue();
                  assertThat(status1.isNewTransaction()).isFalse();
                  assertThat(status1.isNested()).isFalse();
                  assertThat(status1.hasSavepoint()).isFalse();
                  status1.setRollbackOnly();
                });
              }));
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback(sp);
    verify(con).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testExistingTransactionWithManualSavepoint() throws Exception {
    DatabaseMetaData md = mock();
    Savepoint sp = mock();

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
      Object savepoint = status.createSavepoint();
      status.releaseSavepoint(savepoint);
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).releaseSavepoint(sp);
    verify(con).commit();
    verify(con).close();
    verify(ds).getConnection();
  }

  @Test
  public void testExistingTransactionWithManualSavepointAndRollback() throws Exception {
    DatabaseMetaData md = mock();
    Savepoint sp = mock();

    given(md.supportsSavepoints()).willReturn(true);
    given(con.getMetaData()).willReturn(md);
    given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
      Object savepoint = status.createSavepoint();
      status.rollbackToSavepoint(savepoint);
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback(sp);
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testTransactionWithPropagationNested() throws Exception {
    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.getTransactionName()).isEmpty();
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
      assertThat(status.isReadOnly()).isFalse();
      assertThat(status.isRollbackOnly()).isFalse();
      assertThat(status.isCompleted()).isFalse();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).commit();
    verify(con).close();
  }

  @Test
  public void testTransactionWithPropagationNestedAndRollback() throws Exception {
    final TransactionTemplate tt = new TransactionTemplate(tm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.execute((TransactionCallbackWithoutResult) status -> {
      assertThat(status.getTransactionName()).isEmpty();
      assertThat(status.hasTransaction()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();
      assertThat(status.isNested()).isFalse();
      assertThat(status.hasSavepoint()).isFalse();
      assertThat(status.isReadOnly()).isFalse();
      assertThat(status.isRollbackOnly()).isFalse();
      status.setRollbackOnly();
      assertThat(status.isRollbackOnly()).isTrue();
      assertThat(status.isCompleted()).isFalse();
    });

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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
      if (this.status != TransactionSynchronization.STATUS_COMMITTED) {
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
      if (this.status != TransactionSynchronization.STATUS_COMMITTED) {
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
      assertThat(status).isEqualTo(this.status);
      assertThat(TransactionSynchronizationManager.hasResource(this.dataSource)).isTrue();
    }
  }

}
