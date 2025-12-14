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

package infra.jdbc.support;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.SQLException;

import javax.sql.DataSource;

import infra.dao.ConcurrencyFailureException;
import infra.jdbc.datasource.DataSourceTransactionManagerTests;
import infra.transaction.TransactionSystemException;
import infra.transaction.support.TransactionSynchronizationManager;
import infra.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @see DataSourceTransactionManagerTests
 */
public class JdbcTransactionManagerTests extends DataSourceTransactionManagerTests {

  @Override
  protected JdbcTransactionManager createTransactionManager(DataSource ds) {
    return new JdbcTransactionManager(ds);
  }

  @Override
  @Test
  protected void transactionWithExceptionOnCommit() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();
    TransactionTemplate tt = new TransactionTemplate(tm);

    // plain TransactionSystemException
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).close();
  }

  @Test
  void transactionWithDataAccessExceptionOnCommit() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();
    ((JdbcTransactionManager) tm).setExceptionTranslator((task, sql, ex) -> new ConcurrencyFailureException(task));
    TransactionTemplate tt = new TransactionTemplate(tm);

    // specific ConcurrencyFailureException
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).close();
  }

  @Test
  void transactionWithDataAccessExceptionOnCommitFromLazyExceptionTranslator() throws Exception {
    willThrow(new SQLException("Cannot commit", "40")).given(con).commit();
    TransactionTemplate tt = new TransactionTemplate(tm);

    // specific ConcurrencyFailureException
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).close();
  }

  @Override
  @Test
  protected void transactionWithExceptionOnCommitAndRollbackOnCommitFailure() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();

    tm.setRollbackOnCommitFailure(true);
    TransactionTemplate tt = new TransactionTemplate(tm);

    // plain TransactionSystemException
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback();
    verify(con).close();
  }

  @Override
  @Test
  protected void transactionWithExceptionOnRollback() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    willThrow(new SQLException("Cannot rollback")).given(con).rollback();
    TransactionTemplate tt = new TransactionTemplate(tm);

    // plain TransactionSystemException
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
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
  void transactionWithDataAccessExceptionOnRollback() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    willThrow(new SQLException("Cannot rollback")).given(con).rollback();
    ((JdbcTransactionManager) tm).setExceptionTranslator((task, sql, ex) -> new ConcurrencyFailureException(task));
    TransactionTemplate tt = new TransactionTemplate(tm);

    // specific ConcurrencyFailureException
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> status.setRollbackOnly()));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    InOrder ordered = inOrder(con);
    ordered.verify(con).setAutoCommit(false);
    ordered.verify(con).rollback();
    ordered.verify(con).setAutoCommit(true);
    verify(con).close();
  }

  @Test
  void transactionWithDataAccessExceptionOnRollbackFromLazyExceptionTranslator() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    willThrow(new SQLException("Cannot rollback", "40")).given(con).rollback();
    TransactionTemplate tt = new TransactionTemplate(tm);

    // specific ConcurrencyFailureException
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.executeWithoutResult(status -> {
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

}
