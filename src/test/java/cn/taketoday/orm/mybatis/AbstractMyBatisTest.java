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

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockResultSet;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.SQLException;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractMyBatisTest {

  protected PooledMockDataSource dataSource = new PooledMockDataSource();

  protected SqlSessionFactory sqlSessionFactory;

  protected ExecutorInterceptor executorInterceptor = new ExecutorInterceptor();

  protected DataSourceTransactionManager txManager;

  protected PersistenceExceptionTranslator exceptionTranslator;

  protected MockConnection connection;

  protected MockConnection connectionTwo;

  @BeforeEach
  public void setupBase() throws Exception {
    // create an SqlSessionFactory that will use SpringManagedTransactions
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setMapperLocations(new ClassPathResource("cn/taketoday/orm/mybatis/TestMapper.xml"));
    // note running without SqlSessionFactoryBean.configLocation set => default configuration
    factoryBean.setDataSource(dataSource);
    factoryBean.setPlugins(executorInterceptor);

    exceptionTranslator = new MyBatisExceptionTranslator(dataSource, true);

    sqlSessionFactory = factoryBean.getObject();

    txManager = new DataSourceTransactionManager(dataSource);
  }

  protected void assertNoCommit() {
    assertNoCommitJdbc();
    assertNoCommitSession();
  }

  protected void assertNoCommitJdbc() {
    assertThat(connection.getNumberCommits()).as("should not call commit on Connection").isEqualTo(0);
    assertThat(connection.getNumberRollbacks()).as("should not call rollback on Connection").isEqualTo(0);
  }

  protected void assertNoCommitSession() {
    assertThat(executorInterceptor.getCommitCount()).as("should not call commit on SqlSession").isEqualTo(0);
    assertThat(executorInterceptor.getRollbackCount()).as("should not call rollback on SqlSession").isEqualTo(0);
  }

  protected void assertCommit() {
    assertCommitJdbc();
    assertCommitSession();
  }

  protected void assertCommitJdbc() {
    assertThat(connection.getNumberCommits()).as("should call commit on Connection").isEqualTo(1);
    assertThat(connection.getNumberRollbacks()).as("should not call rollback on Connection").isEqualTo(0);
  }

  protected void assertCommitSession() {
    assertThat(executorInterceptor.getCommitCount()).as("should call commit on SqlSession").isEqualTo(1);
    assertThat(executorInterceptor.getRollbackCount()).as("should not call rollback on SqlSession").isEqualTo(0);
  }

  protected void assertRollback() {
    assertThat(connection.getNumberCommits()).as("should not call commit on Connection").isEqualTo(0);
    assertThat(connection.getNumberRollbacks()).as("should call rollback on Connection").isEqualTo(1);
    assertThat(executorInterceptor.getCommitCount()).as("should not call commit on SqlSession").isEqualTo(0);
    assertThat(executorInterceptor.getRollbackCount()).as("should call rollback on SqlSession").isEqualTo(1);
  }

  protected void assertSingleConnection() {
    assertThat(dataSource.getConnectionCount()).as("should only call DataSource.getConnection() once").isEqualTo(1);
  }

  protected void assertExecuteCount(int count) {
    assertThat(connection.getPreparedStatementResultSetHandler().getExecutedStatements().size())
            .as("should have executed %d SQL statements", count).isEqualTo(count);
  }

  protected void assertConnectionClosed(MockConnection connection) {
    try {
      if ((connection != null) && !connection.isClosed()) {
        fail("Connection is not closed");
      }
    }
    catch (SQLException sqle) {
      fail("cannot call Connection.isClosed() " + sqle.getMessage());
    }
  }

  protected MockConnection createMockConnection() {
    // this query must be the same as the query in TestMapper.xml
    MockResultSet rs = new MockResultSet("SELECT 1");
    rs.addRow(new Object[] { 1 });

    MockConnection con = new MockConnection();
    con.getPreparedStatementResultSetHandler().prepareResultSet("SELECT 1", rs);

    return con;
  }

  /*
   * Setup a new Connection before each test since its closed state will need to be checked afterwards and there is no
   * Connection.open().
   */
  @BeforeEach
  public void setupConnection() throws SQLException {
    dataSource.reset();
    connection = createMockConnection();
    connectionTwo = createMockConnection();
    dataSource.addConnection(connectionTwo);
    dataSource.addConnection(connection);
  }

  @BeforeEach
  public void resetExecutorInterceptor() {
    executorInterceptor.reset();
  }

  @AfterEach
  public void validateConnectionClosed() {
    assertConnectionClosed(connection);

    connection = null;
  }

}
