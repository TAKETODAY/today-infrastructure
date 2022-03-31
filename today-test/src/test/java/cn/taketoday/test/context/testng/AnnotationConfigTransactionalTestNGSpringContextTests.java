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

package cn.taketoday.test.context.testng;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.annotation.Propagation;
import cn.taketoday.transaction.annotation.Transactional;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

/**
 * Integration tests that verify support for
 * {@link cn.taketoday.context.annotation.Configuration @Configuration} classes
 * with transactional TestNG-based tests.
 *
 * <p>Configuration will be loaded from
 * {@link ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
public class AnnotationConfigTransactionalTestNGSpringContextTests
		extends AbstractTransactionalTestNGSpringContextTests {

	private static final String JANE = "jane";
	private static final String SUE = "sue";
	private static final String YODA = "yoda";

	private static final int NUM_TESTS = 2;
	private static final int NUM_TX_TESTS = 1;

	private static int numSetUpCalls = 0;
	private static int numSetUpCallsInTransaction = 0;
	private static int numTearDownCalls = 0;
	private static int numTearDownCallsInTransaction = 0;

	@Autowired
	private Employee employee;

	@Autowired
	private Pet pet;


	private int createPerson(String name) {
		return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	private int deletePerson(String name) {
		return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

	private void assertNumRowsInPersonTable(int expectedNumRows, String testState) {
		assertThat(countRowsInTable("person"))
			.as("the number of rows in the person table (" + testState + ").")
			.isEqualTo(expectedNumRows);
	}

	private void assertAddPerson(String name) {
		assertThat(createPerson(name)).as("Adding '%s'", name).isEqualTo(1);
	}

	@BeforeClass
	void beforeClass() {
		numSetUpCalls = 0;
		numSetUpCallsInTransaction = 0;
		numTearDownCalls = 0;
		numTearDownCallsInTransaction = 0;
	}

	@AfterClass
	void afterClass() {
		assertThat(numSetUpCalls).as("number of calls to setUp().").isEqualTo(NUM_TESTS);
		assertThat(numSetUpCallsInTransaction).as("number of calls to setUp() within a transaction.").isEqualTo(NUM_TX_TESTS);
		assertThat(numTearDownCalls).as("number of calls to tearDown().").isEqualTo(NUM_TESTS);
		assertThat(numTearDownCallsInTransaction).as("number of calls to tearDown() within a transaction.").isEqualTo(NUM_TX_TESTS);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void autowiringFromConfigClass() {
		assertThat(employee).as("The employee should have been autowired.").isNotNull();
		assertThat(employee.getName()).isEqualTo("John Smith");

		assertThat(pet).as("The pet should have been autowired.").isNotNull();
		assertThat(pet.getName()).isEqualTo("Fido");
	}

	@BeforeTransaction
	void beforeTransaction() {
		assertNumRowsInPersonTable(1, "before a transactional test method");
		assertAddPerson(YODA);
	}

	@BeforeMethod
	void setUp() throws Exception {
		numSetUpCalls++;
		if (isActualTransactionActive()) {
			numSetUpCallsInTransaction++;
		}
		assertNumRowsInPersonTable((isActualTransactionActive() ? 2 : 1), "before a test method");
	}

	@Test
	public void modifyTestDataWithinTransaction() {
		TransactionAssert.assertThatTransaction().isActive();
		assertAddPerson(JANE);
		assertAddPerson(SUE);
		assertNumRowsInPersonTable(4, "in modifyTestDataWithinTransaction()");
	}

	@AfterMethod
	void tearDown() throws Exception {
		numTearDownCalls++;
		if (isActualTransactionActive()) {
			numTearDownCallsInTransaction++;
		}
		assertNumRowsInPersonTable((isActualTransactionActive() ? 4 : 1), "after a test method");
	}

	@AfterTransaction
	void afterTransaction() {
		assertThat(deletePerson(YODA)).as("Deleting yoda").isEqualTo(1);
		assertNumRowsInPersonTable(1, "after a transactional test method");
	}


	@Configuration
	static class ContextConfiguration {

		@Bean
		Employee employee() {
			Employee employee = new Employee();
			employee.setName("John Smith");
			employee.setAge(42);
			employee.setCompany("Acme Widgets, Inc.");
			return employee;
		}

		@Bean
		Pet pet() {
			return new Pet("Fido");
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
			.addScript("classpath:/org/springframework/test/jdbc/schema.sql")//
			.addScript("classpath:/org/springframework/test/jdbc/data.sql")//
			.build();
		}

	}

}
