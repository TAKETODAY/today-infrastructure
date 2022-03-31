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

package cn.taketoday.test.context.junit4.spr9051;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.annotation.DirtiesContext.ClassMode;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;
import cn.taketoday.test.context.testng.AnnotationConfigTransactionalTestNGSpringContextTests;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

/**
 * This set of tests (i.e., all concrete subclasses) investigates the claims made in
 * <a href="https://jira.spring.io/browse/SPR-9051" target="_blank">SPR-9051</a>
 * with regard to transactional tests.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see AnnotationConfigTransactionalTestNGSpringContextTests
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
