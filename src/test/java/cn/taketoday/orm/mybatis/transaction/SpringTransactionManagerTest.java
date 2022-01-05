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
package cn.taketoday.orm.mybatis.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import cn.taketoday.orm.mybatis.AbstractMyBatisSpringTest;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;

class SpringTransactionManagerTest extends AbstractMyBatisSpringTest {

  @Test
  void shouldNoOpWithTx() throws Exception {
    DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
    txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");
    TransactionStatus status = txManager.getTransaction(txDef);

    SpringManagedTransactionFactory transactionFactory = new SpringManagedTransactionFactory();
    SpringManagedTransaction transaction = (SpringManagedTransaction) transactionFactory.newTransaction(dataSource,
        null, false);
    transaction.getConnection();
    transaction.commit();
    transaction.close();
    assertThat(connection.getNumberCommits()).as("should not call commit on Connection").isEqualTo(0);
    assertThat(connection.isClosed()).as("should not close the Connection").isFalse();

    txManager.commit(status);
  }

  // @Test
  // public void shouldManageWithOtherDatasource() throws Exception {
  // DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
  // txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");
  // TransactionStatus status = txManager.getTransaction(txDef);
  //
  // SpringManagedTransactionFactory transactionFactory = new SpringManagedTransactionFactory(new MockDataSource());
  // SpringManagedTransaction transaction = (SpringManagedTransaction) transactionFactory.newTransaction(connection,
  // false);
  // transaction.commit();
  // transaction.close();
  // assertEquals("should call commit on Connection", 1, connection.getNumberCommits());
  // assertTrue("should close the Connection", connection.isClosed());
  //
  // txManager.commit(status);
  // }

  @Test
  void shouldManageWithNoTx() throws Exception {
    SpringManagedTransactionFactory transactionFactory = new SpringManagedTransactionFactory();
    SpringManagedTransaction transaction = (SpringManagedTransaction) transactionFactory.newTransaction(dataSource,
        null, false);
    transaction.getConnection();
    transaction.commit();
    transaction.close();
    assertThat(connection.getNumberCommits()).as("should call commit on Connection").isEqualTo(1);
    assertThat(connection.isClosed()).as("should close the Connection").isTrue();
  }

  @Test
  void shouldNotCommitWithNoTxAndAutocommitIsOn() throws Exception {
    SpringManagedTransactionFactory transactionFactory = new SpringManagedTransactionFactory();
    SpringManagedTransaction transaction = (SpringManagedTransaction) transactionFactory.newTransaction(dataSource,
        null, false);
    connection.setAutoCommit(true);
    transaction.getConnection();
    transaction.commit();
    transaction.close();
    assertThat(connection.getNumberCommits()).as("should not call commit on a Connection with autocommit").isEqualTo(0);
    assertThat(connection.isClosed()).as("should close the Connection").isTrue();
  }

  @Test
  void shouldIgnoreAutocommit() throws Exception {
    SpringManagedTransactionFactory transactionFactory = new SpringManagedTransactionFactory();
    SpringManagedTransaction transaction = (SpringManagedTransaction) transactionFactory.newTransaction(dataSource,
        null, true);
    transaction.getConnection();
    transaction.commit();
    transaction.close();
    assertThat(connection.getNumberCommits()).as("should call commit on Connection").isEqualTo(1);
    assertThat(connection.isClosed()).as("should close the Connection").isTrue();
  }

}
