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

package cn.taketoday.test.context.testng.transaction.programmatic;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.test.annotation.Commit;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.test.context.transaction.TestTransaction;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.annotation.Propagation;
import cn.taketoday.transaction.annotation.Transactional;

import org.testng.IHookCallBack;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * This class is a copy of the JUnit-based
 * {@link cn.taketoday.test.context.transaction.programmatic.ProgrammaticTxMgmtTests}
 * class that has been modified to run with TestNG.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
public class ProgrammaticTxMgmtTestNGTests extends AbstractTransactionalTestNGSpringContextTests {

	private String methodName;


	@Override
	public void run(IHookCallBack callBack, ITestResult testResult) {
		this.methodName = testResult.getMethod().getMethodName();
		super.run(callBack, testResult);
	}

	@BeforeTransaction
	public void beforeTransaction() {
		deleteFromTables("user");
		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data.sql", false);
	}

	@AfterTransaction
	public void afterTransaction() {
		switch (this.methodName) {
			case "commitTxAndStartNewTx":
			case "commitTxButDoNotStartNewTx": {
				assertUsers("Dogbert");
				break;
			}
			case "rollbackTxAndStartNewTx":
			case "rollbackTxButDoNotStartNewTx":
			case "startTxWithExistingTransaction": {
				assertUsers("Dilbert");
				break;
			}
			case "rollbackTxAndStartNewTxWithDefaultCommitSemantics": {
				assertUsers("Dilbert", "Dogbert");
				break;
			}
			default: {
				fail("missing 'after transaction' assertion for test method: " + this.methodName);
			}
		}
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void isActiveWithNonExistentTransactionContext() {
		assertThat(TestTransaction.isActive()).isFalse();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void flagForRollbackWithNonExistentTransactionContext() {
		TestTransaction.flagForRollback();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void flagForCommitWithNonExistentTransactionContext() {
		TestTransaction.flagForCommit();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void isFlaggedForRollbackWithNonExistentTransactionContext() {
		TestTransaction.isFlaggedForRollback();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void startTxWithNonExistentTransactionContext() {
		TestTransaction.start();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void startTxWithExistingTransaction() {
		TestTransaction.start();
	}

	@Test(expectedExceptions = IllegalStateException.class)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void endTxWithNonExistentTransactionContext() {
		TestTransaction.end();
	}

	@Test
	public void commitTxAndStartNewTx() {
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Commit
		TestTransaction.flagForCommit();
		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		TestTransaction.end();
		TransactionAssert.assertThatTransaction().isNotActive();
		assertThat(TestTransaction.isActive()).isFalse();
		assertUsers();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dogbert");

		TestTransaction.start();
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
	}

	@Test
	public void commitTxButDoNotStartNewTx() {
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Commit
		TestTransaction.flagForCommit();
		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		TransactionAssert.assertThatTransaction().isNotActive();
		assertUsers();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dogbert");
	}

	@Test
	public void rollbackTxAndStartNewTx() {
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback (automatically)
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		TransactionAssert.assertThatTransaction().isNotActive();
		assertUsers("Dilbert");

		// Start new transaction with default rollback semantics
		TestTransaction.start();
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		assertThat(TestTransaction.isActive()).isTrue();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dilbert", "Dogbert");
	}

	@Test
	public void rollbackTxButDoNotStartNewTx() {
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback (automatically)
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		TransactionAssert.assertThatTransaction().isNotActive();
		assertUsers("Dilbert");
	}

	@Test
	@Commit
	public void rollbackTxAndStartNewTxWithDefaultCommitSemantics() {
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback
		TestTransaction.flagForRollback();
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		TransactionAssert.assertThatTransaction().isNotActive();
		assertUsers("Dilbert");

		// Start new transaction with default commit semantics
		TestTransaction.start();
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		assertThat(TestTransaction.isActive()).isTrue();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dilbert", "Dogbert");
	}

	// -------------------------------------------------------------------------

	private void assertUsers(String... users) {
		List<String> expected = Arrays.asList(users);
		Collections.sort(expected);
		List<String> actual = jdbcTemplate.queryForList("select name from user", String.class);
		Collections.sort(actual);
		assertThat(actual).as("Users in database;").isEqualTo(expected);
	}


	// -------------------------------------------------------------------------

	@Configuration
	static class Config {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
			.setName("programmatic-tx-mgmt-test-db")//
			.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql") //
			.build();
		}
	}

}
