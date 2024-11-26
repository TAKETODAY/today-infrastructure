/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.jdbc.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import infra.beans.factory.support.StaticListableBeanFactory;
import infra.jdbc.datasource.lookup.BeanFactoryDataSourceLookup;
import infra.jdbc.datasource.lookup.IsolationLevelDataSourceRouter;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;
import infra.transaction.jta.JtaTransactionManager;
import infra.transaction.jta.JtaTransactionObject;
import infra.transaction.support.TransactionSynchronization;
import infra.transaction.support.TransactionSynchronizationManager;
import infra.transaction.support.TransactionTemplate;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 4.0
 */
@Execution(ExecutionMode.SAME_THREAD)
public class DataSourceJtaTransactionTests {

  private final DataSource dataSource = mock();

  private final Connection connection = mock();

  private final UserTransaction userTransaction = mock();

  private final TransactionManager transactionManager = mock();

  private final Transaction transaction = mock();

  @BeforeEach
  public void setup() throws Exception {
    given(dataSource.getConnection()).willReturn(connection);
  }

  @AfterEach
  public void verifyTransactionSynchronizationManagerState() {
    assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).isNull();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @Test
  public void testJtaTransactionCommit() throws Exception {
    doTestJtaTransaction(false);
  }

  @Test
  public void testJtaTransactionRollback() throws Exception {
    doTestJtaTransaction(true);
  }

  private void doTestJtaTransaction(final boolean rollback) throws Exception {
    if (rollback) {
      given(userTransaction.getStatus()).willReturn(
              Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
    }
    else {
      given(userTransaction.getStatus()).willReturn(
              Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    }

    JtaTransactionManager ptm = new JtaTransactionManager(userTransaction);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();

      Connection con = DataSourceUtils.getConnection(dataSource);
      assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isTrue();
      DataSourceUtils.releaseConnection(con, dataSource);

      con = DataSourceUtils.getConnection(dataSource);
      assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isTrue();
      DataSourceUtils.releaseConnection(con, dataSource);

      if (rollback) {
        status.setRollbackOnly();
      }
    });

    assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    verify(userTransaction).begin();
    if (rollback) {
      verify(userTransaction).rollback();
    }
    verify(connection).close();
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNew() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(false, false, false, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(false, false, true, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(false, true, false, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(false, true, true, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSource() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(false, false, true, true);
  }

  @Test
  public void testJtaTransactionRollbackWithPropagationRequiresNew() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(true, false, false, false);
  }

  @Test
  public void testJtaTransactionRollbackWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(true, false, true, false);
  }

  @Test
  public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(true, true, false, false);
  }

  @Test
  public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(true, true, true, false);
  }

  @Test
  public void testJtaTransactionRollbackWithPropagationRequiresNewWithTransactionAwareDataSource() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNew(true, false, true, true);
  }

  private void doTestJtaTransactionWithPropagationRequiresNew(
          final boolean rollback, final boolean openOuterConnection, final boolean accessAfterResume,
          final boolean useTransactionAwareDataSource) throws Exception {

    given(transactionManager.suspend()).willReturn(transaction);
    if (rollback) {
      given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
              Status.STATUS_ACTIVE);
    }
    else {
      given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
              Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
    }

    given(connection.isReadOnly()).willReturn(true);

    final DataSource dsToUse = useTransactionAwareDataSource ?
                               new TransactionAwareDataSourceProxy(dataSource) : dataSource;

    JtaTransactionManager ptm = new JtaTransactionManager(userTransaction, transactionManager);
    final TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(status.isNewTransaction()).isTrue();

      Connection con = DataSourceUtils.getConnection(dsToUse);
      try {
        assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
        con.isReadOnly();
        DataSourceUtils.releaseConnection(con, dsToUse);

        con = DataSourceUtils.getConnection(dsToUse);
        assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
        if (!openOuterConnection) {
          DataSourceUtils.releaseConnection(con, dsToUse);
        }
      }
      catch (SQLException ex) {
      }

      for (int i = 0; i < 5; i++) {

        tt.executeWithoutResult(status1 -> {
          assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
          assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
          assertThat(status1.isNewTransaction()).isTrue();

          try {
            Connection con1 = DataSourceUtils.getConnection(dsToUse);
            con1.isReadOnly();
            assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
            DataSourceUtils.releaseConnection(con1, dsToUse);

            con1 = DataSourceUtils.getConnection(dsToUse);
            assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
            DataSourceUtils.releaseConnection(con1, dsToUse);
          }
          catch (SQLException ex) {
          }
        });

      }

      if (rollback) {
        status.setRollbackOnly();
      }

      if (accessAfterResume) {
        try {
          if (!openOuterConnection) {
            con = DataSourceUtils.getConnection(dsToUse);
          }
          assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
          con.isReadOnly();
          DataSourceUtils.releaseConnection(con, dsToUse);

          con = DataSourceUtils.getConnection(dsToUse);
          assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
          DataSourceUtils.releaseConnection(con, dsToUse);
        }
        catch (SQLException ex) {
        }
      }

      else {
        if (openOuterConnection) {
          DataSourceUtils.releaseConnection(con, dsToUse);
        }
      }
    });

    assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    verify(userTransaction, times(6)).begin();
    verify(transactionManager, times(5)).resume(transaction);
    if (rollback) {
      verify(userTransaction, times(5)).commit();
      verify(userTransaction).rollback();
    }
    else {
      verify(userTransaction, times(6)).commit();
    }
    if (accessAfterResume && !openOuterConnection) {
      verify(connection, times(7)).close();
    }
    else {
      verify(connection, times(6)).close();
    }
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiredWithinSupports() throws Exception {
    doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(false, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiredWithinNotSupported() throws Exception {
    doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(false, true);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithinSupports() throws Exception {
    doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(true, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithinNotSupported() throws Exception {
    doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(true, true);
  }

  private void doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(
          final boolean requiresNew, boolean notSupported) throws Exception {

    if (notSupported) {
      given(userTransaction.getStatus()).willReturn(
              Status.STATUS_ACTIVE,
              Status.STATUS_NO_TRANSACTION,
              Status.STATUS_ACTIVE,
              Status.STATUS_ACTIVE);
      given(transactionManager.suspend()).willReturn(transaction);
    }
    else {
      given(userTransaction.getStatus()).willReturn(
              Status.STATUS_NO_TRANSACTION,
              Status.STATUS_NO_TRANSACTION,
              Status.STATUS_ACTIVE,
              Status.STATUS_ACTIVE);
    }

    final DataSource dataSource = mock();
    final Connection connection1 = mock();
    final Connection connection2 = mock();
    given(dataSource.getConnection()).willReturn(connection1, connection2);

    final JtaTransactionManager ptm = new JtaTransactionManager(userTransaction, transactionManager);
    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(notSupported ?
                              TransactionDefinition.PROPAGATION_NOT_SUPPORTED : TransactionDefinition.PROPAGATION_SUPPORTS);

    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    tt.executeWithoutResult(status -> {
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
      assertThat(DataSourceUtils.getConnection(dataSource)).isSameAs(connection1);
      assertThat(DataSourceUtils.getConnection(dataSource)).isSameAs(connection1);

      TransactionTemplate tt2 = new TransactionTemplate(ptm);
      tt2.setPropagationBehavior(requiresNew ?
                                 TransactionDefinition.PROPAGATION_REQUIRES_NEW : TransactionDefinition.PROPAGATION_REQUIRED);
      tt2.executeWithoutResult(status1 -> {
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        assertThat(DataSourceUtils.getConnection(dataSource)).isSameAs(connection2);
        assertThat(DataSourceUtils.getConnection(dataSource)).isSameAs(connection2);
      });

      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
      assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
      assertThat(DataSourceUtils.getConnection(dataSource)).isSameAs(connection1);
    });
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    verify(userTransaction).begin();
    verify(userTransaction).commit();
    if (notSupported) {
      verify(transactionManager).resume(transaction);
    }
    verify(connection2).close();
    verify(connection1).close();
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewAndSuspendException() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, false, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndSuspendException() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, true, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSourceAndSuspendException() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, false, true);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndTransactionAwareDataSourceAndSuspendException() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, true, true);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewAndBeginException() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, false, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndBeginException() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, true, false);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndTransactionAwareDataSourceAndBeginException() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, true, true);
  }

  @Test
  public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSourceAndBeginException() throws Exception {
    doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, false, true);
  }

  private void doTestJtaTransactionWithPropagationRequiresNewAndBeginException(boolean suspendException,
          final boolean openOuterConnection, final boolean useTransactionAwareDataSource) throws Exception {

    given(userTransaction.getStatus()).willReturn(
            Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE,
            Status.STATUS_ACTIVE);
    if (suspendException) {
      given(transactionManager.suspend()).willThrow(new SystemException());
    }
    else {
      given(transactionManager.suspend()).willReturn(transaction);
      willThrow(new SystemException()).given(userTransaction).begin();
    }

    given(connection.isReadOnly()).willReturn(true);

    final DataSource dsToUse = useTransactionAwareDataSource ?
                               new TransactionAwareDataSourceProxy(dataSource) : dataSource;
    if (dsToUse instanceof TransactionAwareDataSourceProxy) {
      ((TransactionAwareDataSourceProxy) dsToUse).setReobtainTransactionalConnections(true);
    }

    JtaTransactionManager ptm = new JtaTransactionManager(userTransaction, transactionManager);
    final TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    assertThatExceptionOfType(TransactionException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
              assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
              assertThat(status.isNewTransaction()).isTrue();

              Connection con = DataSourceUtils.getConnection(dsToUse);
              try {
                assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
                con.isReadOnly();
                DataSourceUtils.releaseConnection(con, dsToUse);

                con = DataSourceUtils.getConnection(dsToUse);
                assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
                if (!openOuterConnection) {
                  DataSourceUtils.releaseConnection(con, dsToUse);
                }
              }
              catch (SQLException ex) {
              }

              try {
                tt.executeWithoutResult(status1 -> {
                  assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
                  assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
                  assertThat(status1.isNewTransaction()).isTrue();

                  Connection con1 = DataSourceUtils.getConnection(dsToUse);
                  assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
                  DataSourceUtils.releaseConnection(con1, dsToUse);

                  con1 = DataSourceUtils.getConnection(dsToUse);
                  assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
                  DataSourceUtils.releaseConnection(con1, dsToUse);
                });
              }
              finally {
                if (openOuterConnection) {
                  try {
                    con.isReadOnly();
                    DataSourceUtils.releaseConnection(con, dsToUse);
                  }
                  catch (SQLException ex) {
                  }
                }
              }
            }));

    assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    verify(userTransaction).begin();
    if (suspendException) {
      verify(userTransaction).rollback();
    }

    if (suspendException) {
      verify(connection, atLeastOnce()).close();
    }
    else {
      verify(connection, never()).close();
    }
  }

  @Test
  public void testJtaTransactionWithConnectionHolderStillBound() throws Exception {
    JtaTransactionManager ptm = new JtaTransactionManager(userTransaction) {

      @Override
      protected void doRegisterAfterCompletionWithJtaTransaction(
              JtaTransactionObject txObject, final List<TransactionSynchronization> synchronizations) {
        Thread async = new Thread() {
          @Override
          public void run() {
            invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_COMMITTED);
          }
        };
        async.start();
        try {
          async.join();
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }
      }
    };
    TransactionTemplate tt = new TransactionTemplate(ptm);
    assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

    given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
    for (int i = 0; i < 3; i++) {
      final boolean releaseCon = (i != 1);

      tt.executeWithoutResult(status -> {
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(status.isNewTransaction()).isFalse();

        Connection con = DataSourceUtils.getConnection(dataSource);
        assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isTrue();
        DataSourceUtils.releaseConnection(con, dataSource);

        con = DataSourceUtils.getConnection(dataSource);
        assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isTrue();
        if (releaseCon) {
          DataSourceUtils.releaseConnection(con, dataSource);
        }
      });

      if (!releaseCon) {
        assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isTrue();
      }
      else {
        assertThat(TransactionSynchronizationManager.hasResource(dataSource)).isFalse();
      }
      assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }
    verify(connection, times(3)).close();
  }

  @Test
  public void testJtaTransactionWithIsolationLevelDataSourceAdapter() throws Exception {
    given(userTransaction.getStatus()).willReturn(
            Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE,
            Status.STATUS_ACTIVE,
            Status.STATUS_NO_TRANSACTION,
            Status.STATUS_ACTIVE,
            Status.STATUS_ACTIVE);

    final IsolationLevelDataSourceAdapter dsToUse = new IsolationLevelDataSourceAdapter();
    dsToUse.setTargetDataSource(dataSource);
    dsToUse.afterPropertiesSet();

    JtaTransactionManager ptm = new JtaTransactionManager(userTransaction);
    ptm.setAllowCustomIsolationLevels(true);

    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.executeWithoutResult(status -> {
      Connection con = DataSourceUtils.getConnection(dsToUse);
      assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
      assertThat(con).isSameAs(connection);
      DataSourceUtils.releaseConnection(con, dsToUse);
    });

    tt.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    tt.setReadOnly(true);
    tt.executeWithoutResult(status -> {
      Connection con = DataSourceUtils.getConnection(dsToUse);
      assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
      assertThat(con).isSameAs(connection);
      DataSourceUtils.releaseConnection(con, dsToUse);
    });

    verify(userTransaction, times(2)).begin();
    verify(userTransaction, times(2)).commit();
    verify(connection).setReadOnly(true);
    verify(connection).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    verify(connection, times(2)).close();
  }

  @Test
  public void testJtaTransactionWithIsolationLevelDataSourceRouter() throws Exception {
    doTestJtaTransactionWithIsolationLevelDataSourceRouter(false);
  }

  @Test
  public void testJtaTransactionWithIsolationLevelDataSourceRouterWithDataSourceLookup() throws Exception {
    doTestJtaTransactionWithIsolationLevelDataSourceRouter(true);
  }

  private void doTestJtaTransactionWithIsolationLevelDataSourceRouter(boolean dataSourceLookup) throws Exception {
    given(userTransaction.getStatus())
            .willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

    final DataSource dataSource1 = mock();
    final Connection connection1 = mock();
    given(dataSource1.getConnection()).willReturn(connection1);

    final DataSource dataSource2 = mock();
    final Connection connection2 = mock();
    given(dataSource2.getConnection()).willReturn(connection2);

    final IsolationLevelDataSourceRouter dsToUse = new IsolationLevelDataSourceRouter();
    Map<Object, Object> targetDataSources = new HashMap<>();
    if (dataSourceLookup) {
      targetDataSources.put("ISOLATION_REPEATABLE_READ", "ds2");
      dsToUse.setDefaultTargetDataSource("ds1");
      StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
      beanFactory.addBean("ds1", dataSource1);
      beanFactory.addBean("ds2", dataSource2);
      dsToUse.setDataSourceLookup(new BeanFactoryDataSourceLookup(beanFactory));
    }
    else {
      targetDataSources.put("ISOLATION_REPEATABLE_READ", dataSource2);
      dsToUse.setDefaultTargetDataSource(dataSource1);
    }
    dsToUse.setTargetDataSources(targetDataSources);
    dsToUse.afterPropertiesSet();

    JtaTransactionManager ptm = new JtaTransactionManager(userTransaction);
    ptm.setAllowCustomIsolationLevels(true);

    TransactionTemplate tt = new TransactionTemplate(ptm);
    tt.executeWithoutResult(status -> {
      Connection con = DataSourceUtils.getConnection(dsToUse);
      assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
      assertThat(con).isSameAs(connection1);
      DataSourceUtils.releaseConnection(con, dsToUse);
    });

    tt.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    tt.executeWithoutResult(status -> {
      Connection con = DataSourceUtils.getConnection(dsToUse);
      assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
      assertThat(con).isSameAs(connection2);
      DataSourceUtils.releaseConnection(con, dsToUse);
    });

    verify(userTransaction, times(2)).begin();
    verify(userTransaction, times(2)).commit();
    verify(connection1).close();
    verify(connection2).close();
  }
}
