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

package infra.test.context.junit4.spr9051;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.beans.testfixture.beans.Employee;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.test.annotation.DirtiesContext;
import infra.test.annotation.DirtiesContext.ClassMode;
import infra.test.context.junit4.JUnit4ClassRunner;
import infra.test.context.transaction.AfterTransaction;
import infra.test.context.transaction.BeforeTransaction;
import infra.test.transaction.TransactionAssert;
import infra.transaction.annotation.Transactional;

import static infra.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This set of tests (i.e., all concrete subclasses) investigates the claims made in
 *
 * with regard to transactional tests.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractTransactionalAnnotatedConfigClassTests {

  protected static final String JANE = "jane";
  protected static final String SUE = "sue";
  protected static final String YODA = "yoda";

  protected DataSource dataSourceFromTxManager;
  protected DataSource dataSourceViaInjection;

  protected JdbcTemplate jdbcTemplate;

  @Autowired
  private Employee employee;

  @Autowired
  public void setTransactionManager(DataSourceTransactionManager transactionManager) {
    this.dataSourceFromTxManager = transactionManager.getDataSource();
  }

  @Autowired
  public void setDataSource(DataSource dataSource) {
    this.dataSourceViaInjection = dataSource;
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  private int countRowsInTable(String tableName) {
    return jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName, Integer.class);
  }

  private int createPerson(String name) {
    return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
  }

  protected int deletePerson(String name) {
    return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
  }

  protected void assertNumRowsInPersonTable(int expectedNumRows, String testState) {
    assertThat(countRowsInTable("person")).as("the number of rows in the person table (" + testState + ").").isEqualTo(expectedNumRows);
  }

  protected void assertAddPerson(final String name) {
    assertThat(createPerson(name)).as("Adding '" + name + "'").isEqualTo(1);
  }

  @Test
  public void autowiringFromConfigClass() {
    assertThat(employee).as("The employee should have been autowired.").isNotNull();
    assertThat(employee.getName()).isEqualTo("John Smith");
  }

  @BeforeTransaction
  public void beforeTransaction() {
    assertNumRowsInPersonTable(0, "before a transactional test method");
    assertAddPerson(YODA);
  }

  @Before
  public void setUp() throws Exception {
    assertNumRowsInPersonTable((isActualTransactionActive() ? 1 : 0), "before a test method");
  }

  @Test
  @Transactional
  public void modifyTestDataWithinTransaction() {
    TransactionAssert.assertThatTransaction().isActive();
    assertAddPerson(JANE);
    assertAddPerson(SUE);
    assertNumRowsInPersonTable(3, "in modifyTestDataWithinTransaction()");
  }

  @After
  public void tearDown() throws Exception {
    assertNumRowsInPersonTable((isActualTransactionActive() ? 3 : 0), "after a test method");
  }

  @AfterTransaction
  public void afterTransaction() {
    assertThat(deletePerson(YODA)).as("Deleting yoda").isEqualTo(1);
    assertNumRowsInPersonTable(0, "after a transactional test method");
  }

}
