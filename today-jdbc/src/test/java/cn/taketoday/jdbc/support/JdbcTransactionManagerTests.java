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

package cn.taketoday.jdbc.support;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.dao.ConcurrencyFailureException;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManagerTests;
import cn.taketoday.transaction.TransactionExecution;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.support.TransactionCallbackWithoutResult;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import cn.taketoday.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @see cn.taketoday.jdbc.datasource.DataSourceTransactionManagerTests
 */
public class JdbcTransactionManagerTests extends DataSourceTransactionManagerTests<JdbcTransactionManager> {

  @Override
  protected JdbcTransactionManager createTransactionManager(DataSource ds) {
    return new JdbcTransactionManager(ds);
  }

  @Override
  @Test
  public void testTransactionWithExceptionOnCommit() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();
    TransactionTemplate tt = new TransactionTemplate(tm);

    // plain TransactionSystemException
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).close();
  }

  @Test
  public void testTransactionWithDataAccessExceptionOnCommit() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();
    ((JdbcTransactionManager) tm).setExceptionTranslator((task, sql, ex) -> new ConcurrencyFailureException(task));
    TransactionTemplate tt = new TransactionTemplate(tm);

    // specific ConcurrencyFailureException
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).close();
  }

  @Test
  public void testTransactionWithDataAccessExceptionOnCommitFromLazyExceptionTranslator() throws Exception {
    willThrow(new SQLException("Cannot commit", "40")).given(con).commit();
    TransactionTemplate tt = new TransactionTemplate(tm);

    // specific ConcurrencyFailureException
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).close();
  }

  @Override
  @Test
  public void testTransactionWithExceptionOnCommitAndRollbackOnCommitFailure() throws Exception {
    willThrow(new SQLException("Cannot commit")).given(con).commit();

    tm.setRollbackOnCommitFailure(true);
    TransactionTemplate tt = new TransactionTemplate(tm);

    // plain TransactionSystemException
    assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) status -> {
              // something transactional
            }));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
    verify(con).rollback();
    verify(con).close();
  }

  @Override
  @Test
  public void testTransactionWithExceptionOnRollback() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    willThrow(new SQLException("Cannot rollback")).given(con).rollback();
    TransactionTemplate tt = new TransactionTemplate(tm);

    // plain TransactionSystemException
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
  public void testTransactionWithDataAccessExceptionOnRollback() throws Exception {
    given(con.getAutoCommit()).willReturn(true);
    willThrow(new SQLException("Cannot rollback")).given(con).rollback();
    ((JdbcTransactionManager) tm).setExceptionTranslator((task, sql, ex) -> new ConcurrencyFailureException(task));
    TransactionTemplate tt = new TransactionTemplate(tm);

    // specific ConcurrencyFailureException
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
            tt.execute((TransactionCallbackWithoutResult) TransactionExecution::setRollbackOnly));

    assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
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

    // specific ConcurrencyFailureException
    assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
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

}
