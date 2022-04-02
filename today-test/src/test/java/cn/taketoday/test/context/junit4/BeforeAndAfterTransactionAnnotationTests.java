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

package cn.taketoday.test.context.junit4;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.transaction.annotation.Propagation;
import cn.taketoday.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 4 based integration test which verifies
 * {@link BeforeTransaction @BeforeTransaction} and
 * {@link AfterTransaction @AfterTransaction} behavior.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Transactional
public class BeforeAndAfterTransactionAnnotationTests extends AbstractTransactionalSpringRunnerTests {

  protected static JdbcTemplate jdbcTemplate;

  protected static int numBeforeTransactionCalls = 0;
  protected static int numAfterTransactionCalls = 0;

  protected boolean inTransaction = false;

  @Rule
  public final TestName testName = new TestName();

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @BeforeClass
  public static void beforeClass() {
    BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls = 0;
    BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls = 0;
  }

  @AfterClass
  public static void afterClass() {
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(3);
    assertThat(BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls).as("Verifying the total number of calls to beforeTransaction().").isEqualTo(2);
    assertThat(BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls).as("Verifying the total number of calls to afterTransaction().").isEqualTo(2);
  }

  @BeforeTransaction
  void beforeTransaction() {
    TransactionAssert.assertThatTransaction().isNotActive();
    this.inTransaction = true;
    BeforeAndAfterTransactionAnnotationTests.numBeforeTransactionCalls++;
    clearPersonTable(jdbcTemplate);
    assertThat(addPerson(jdbcTemplate, YODA)).as("Adding yoda").isEqualTo(1);
  }

  @AfterTransaction
  void afterTransaction() {
    TransactionAssert.assertThatTransaction().isNotActive();
    this.inTransaction = false;
    BeforeAndAfterTransactionAnnotationTests.numAfterTransactionCalls++;
    assertThat(deletePerson(jdbcTemplate, YODA)).as("Deleting yoda").isEqualTo(1);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table after a transactional test method.").isEqualTo(0);
  }

  @Before
  public void before() {
    assertShouldBeInTransaction();
    long expected = (this.inTransaction ? 1
                                        : 0);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table before a test method.").isEqualTo(expected);
  }

  private void assertShouldBeInTransaction() {
    boolean shouldBeInTransaction = !testName.getMethodName().equals("nonTransactionalMethod");
    TransactionAssert.assertThatTransaction().isInTransaction(shouldBeInTransaction);
  }

  @After
  public void after() {
    assertShouldBeInTransaction();
  }

  @Test
  public void transactionalMethod1() {
    TransactionAssert.assertThatTransaction().isActive();
    assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within transactionalMethod1().").isEqualTo(2);
  }

  @Test
  public void transactionalMethod2() {
    TransactionAssert.assertThatTransaction().isActive();
    assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
    assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within transactionalMethod2().").isEqualTo(3);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void nonTransactionalMethod() {
    TransactionAssert.assertThatTransaction().isNotActive();
    assertThat(addPerson(jdbcTemplate, LUKE)).as("Adding luke").isEqualTo(1);
    assertThat(addPerson(jdbcTemplate, LEIA)).as("Adding leia").isEqualTo(1);
    assertThat(addPerson(jdbcTemplate, YODA)).as("Adding yoda").isEqualTo(1);
    assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table without a transaction.").isEqualTo(3);
  }

}
