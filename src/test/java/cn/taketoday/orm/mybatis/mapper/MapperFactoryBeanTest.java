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
package cn.taketoday.orm.mybatis.mapper;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockDataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.dao.TransientDataAccessResourceException;
import cn.taketoday.orm.mybatis.AbstractMyBatisTest;
import cn.taketoday.orm.mybatis.MyBatisSystemException;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.orm.mybatis.TestMapper;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class MapperFactoryBeanTest extends AbstractMyBatisTest {

  private SqlSessionTemplate sqlSessionTemplate;

  @BeforeEach
  void setupSqlTemplate() {
    sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
  }

  // test normal MapperFactoryBean usage
  @Test
  void testBasicUsage() throws Exception {
    find();

    assertCommit(); // SqlSesssionTemplate autocommits
    assertSingleConnection();
    assertExecuteCount(1);
  }

  @Test
  void testAddToConfigTrue() throws Exception {
    // the default SqlSessionFactory in AbstractMyBatisTodayTest is created with an explicitly set
    // MapperLocations list, so create a new factory here that tests auto-loading the config
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDatabaseIdProvider(null);
    // mapperLocations properties defaults to null
    factoryBean.setDataSource(dataSource);
    factoryBean.setPlugins(executorInterceptor);

    SqlSessionFactory sqlSessionFactory = factoryBean.getObject();

    find(new SqlSessionTemplate(sqlSessionFactory), true);
    assertCommit(); // SqlSesssionTemplate autocommits
    assertSingleConnection();
    assertExecuteCount(1);
  }

  // will fail because TestDao's mapper config is never loaded
  @Test
  void testAddToConfigFalse() throws Throwable {
    try {
      // the default SqlSessionFactory in AbstractMyBatisTodayTest is created with an explicitly
      // set MapperLocations list, so create a new factory here that tests auto-loading the
      // config
      SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
      // mapperLocations properties defaults to null
      factoryBean.setDataSource(dataSource);

      SqlSessionFactory sqlSessionFactory = factoryBean.getObject();

      assertThrows(org.apache.ibatis.binding.BindingException.class,
              () -> find(new SqlSessionTemplate(sqlSessionFactory), false));
      // fail("TestDao's mapper xml should not be loaded");
    }
    catch (MyBatisSystemException mbse) {
      // unwrap exception so the exact MyBatis exception can be tested
      throw mbse.getCause();
    }
    finally {
      // connection not used; force close to avoid failing in validateConnectionClosed()
      connection.close();
    }
  }

  @Test
  void testWithTx() throws Exception {
    TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

    find();

    txManager.commit(status);

    assertCommit();
    assertSingleConnection();
    assertExecuteCount(1);
  }

  // MapperFactoryBeans should be usable outside of TX, as long as a there is no active
  // transaction
  @Test
  void testWithNonTodayTransactionFactory() throws Exception {
    Environment original = sqlSessionFactory.getConfiguration().getEnvironment();
    Environment nonToday = new Environment("non-today", new JdbcTransactionFactory(), dataSource);
    sqlSessionFactory.getConfiguration().setEnvironment(nonToday);

    try {
      find(new SqlSessionTemplate(sqlSessionFactory));

      assertCommit(); // SqlSessionTemplate autocommits
      assertCommitSession();
      assertSingleConnection();
      assertExecuteCount(1);
    }
    finally {
      sqlSessionFactory.getConfiguration().setEnvironment(original);
    }
  }

  // active transaction using the DataSource, but without a TodayTransactionFactory
  // this should error
  @Test
  void testNonTodayTxMgrWithTx() throws Exception {
    Environment original = sqlSessionFactory.getConfiguration().getEnvironment();
    Environment nonToday = new Environment("non-today", new JdbcTransactionFactory(), dataSource);
    sqlSessionFactory.getConfiguration().setEnvironment(nonToday);

    TransactionStatus status = null;

    try {
      status = txManager.getTransaction(new DefaultTransactionDefinition());

      find();

      fail("should not be able to get an SqlSession using non-Today tx manager when there is an active Today tx");
    }
    catch (TransientDataAccessResourceException e) {
      assertThat(e.getMessage())
              .isEqualTo("SqlSessionFactory must be using a ManagedTransactionFactory in order to use"
                      + " transaction synchronization");
    }
    finally {
      // rollback required to close connection
      txManager.rollback(status);

      sqlSessionFactory.getConfiguration().setEnvironment(original);
    }
  }

  // similar to testNonTodayTxFactoryNonTodayDSWithTx() in MyBatisTodayTest
  @Test
  void testNonTodayWithTx() throws Exception {
    Environment original = sqlSessionFactory.getConfiguration().getEnvironment();

    MockDataSource mockDataSource = new MockDataSource();
    mockDataSource.setupConnection(createMockConnection());

    Environment nonToday = new Environment("non-today", new JdbcTransactionFactory(), mockDataSource);
    sqlSessionFactory.getConfiguration().setEnvironment(nonToday);

    SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);

    TransactionStatus status;

    try {
      status = txManager.getTransaction(new DefaultTransactionDefinition());

      find(sqlSessionTemplate);

      txManager.commit(status);

      // txManager still uses original connection
      assertCommit();
      assertSingleConnection();

      // SqlSessionTemplate uses its own connection
      MockConnection mockConnection = (MockConnection) mockDataSource.getConnection();
      assertThat(mockConnection.getNumberCommits()).as("should call commit on Connection").isEqualTo(1);
      assertThat(mockConnection.getNumberRollbacks()).as("should not call rollback on Connection").isEqualTo(0);
      assertCommitSession();
    }
    finally {

      sqlSessionFactory.getConfiguration().setEnvironment(original);
    }
  }

  private void find() throws Exception {
    find(sqlSessionTemplate, true);
  }

  private void find(SqlSessionTemplate sqlSessionTemplate) throws Exception {
    find(sqlSessionTemplate, true);
  }

  private void find(SqlSessionTemplate sqlSessionTemplate, boolean addToConfig) throws Exception {
    // recreate the mapper for each test since sqlSessionTemplate or the underlying
    // SqlSessionFactory could change for each test
    MapperFactoryBean<TestMapper> mapper = new MapperFactoryBean<>();
    mapper.setMapperInterface(TestMapper.class);
    mapper.setSqlSessionTemplate(sqlSessionTemplate);
    mapper.setAddToConfig(addToConfig);
    mapper.afterPropertiesSet();

    TestMapper object = mapper.getObject();
    object.findTest();
  }
}
