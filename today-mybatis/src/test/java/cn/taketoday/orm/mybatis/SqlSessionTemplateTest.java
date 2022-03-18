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
package cn.taketoday.orm.mybatis;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

// tests basic usage and implementation only
// MapperFactoryBeanTest handles testing the transactional functions in SqlSessionTemplate
public class SqlSessionTemplateTest extends AbstractMyBatisTest {

  private SqlSession sqlSessionTemplate;

  @BeforeEach
  void setupSqlTemplate() {
    sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
  }

  @AfterEach
  void tearDown() {
    try {
      connection.close();
    }
    catch (SQLException ignored) {
    }
  }

  @Test
  void testGetConnection() throws java.sql.SQLException {
    java.sql.Connection conn = sqlSessionTemplate.getConnection();

    // outside of an explicit tx, getConnection() will start a tx, get an open connection then
    // end the tx, which closes the connection
    assertThat(conn.isClosed()).isTrue();
  }

  @Test
  void testGetConnectionInTx() throws java.sql.SQLException {
    TransactionStatus status = null;

    try {
      status = txManager.getTransaction(new DefaultTransactionDefinition());

      java.sql.Connection conn = sqlSessionTemplate.getConnection();

      assertThat(conn.isClosed()).isFalse();

    }
    finally {
      // rollback required to close connection
      txManager.rollback(status);
    }
  }

  @Test
  void testCommit() {
    assertThrows(UnsupportedOperationException.class, sqlSessionTemplate::commit);
  }

  @Test
  void testClose() {
    assertThrows(UnsupportedOperationException.class, sqlSessionTemplate::close);
  }

  @Test
  void testRollback() {
    assertThrows(UnsupportedOperationException.class, sqlSessionTemplate::rollback);
  }

  @Test
  void testExecutorType() {
    SqlSessionTemplate template = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    assertThat(template.getExecutorType()).isEqualTo(ExecutorType.BATCH);

    DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);

    TransactionStatus status = null;

    try {
      status = manager.getTransaction(new DefaultTransactionDefinition());

      // will synchronize the template with the current tx
      template.getConnection();

      SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sqlSessionFactory);

      assertThat(holder.getExecutorType()).isEqualTo(ExecutorType.BATCH);
    }
    finally {
      // rollback required to close connection
      txManager.rollback(status);
    }
  }

  @Test
  void testExceptionTranslationShouldThrowMyBatisSystemException() throws SQLException {
    try {
      sqlSessionTemplate.selectOne("undefined");
      fail("exception not thrown when expected");
    }
    catch (MyBatisSystemException mbse) {
      // success
    }
    catch (Throwable t) {
      fail("SqlSessionTemplate should translate MyBatis PersistenceExceptions");
    }
    finally {
      connection.close(); // the template do not open the connection so it do not close it
    }
  }

  @Test
  void testExceptionTranslationShouldThrowDataAccessException() {

    // this query must be the same as the query in TestMapper.xml
    connection.getPreparedStatementResultSetHandler().prepareThrowsSQLException("SELECT 'fail'");

    try {
      sqlSessionTemplate.selectOne("cn.taketoday.orm.mybatis.TestMapper.findFail");
      fail("exception not thrown when expected");
    }
    catch (MyBatisSystemException mbse) {
      fail("SqlSessionTemplate should translate SQLExceptions into DataAccessExceptions");
    }
    catch (DataAccessException dae) {
      // success
    }
    catch (Throwable t) {
      fail("SqlSessionTemplate should translate MyBatis PersistenceExceptions");
    }
  }

  @Test
  void testTemplateWithNoTxInsert() {

    sqlSessionTemplate.getMapper(TestMapper.class).insertTest("test1");
    assertCommitJdbc();
    assertCommitSession();

  }

  @Test
  void testTemplateWithNoTxSelect() {

    sqlSessionTemplate.getMapper(TestMapper.class).findTest();
    assertCommit();

  }

  @Test
  void testWithTxRequired() {
    DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
    txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

    TransactionStatus status = txManager.getTransaction(txDef);

    sqlSessionTemplate.getMapper(TestMapper.class).findTest();

    txManager.commit(status);

    assertCommit();
    assertSingleConnection();
  }

}
