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

package infra.test.context.transaction.programmatic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.io.Resource;
import infra.dao.DataAccessException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.jdbc.datasource.init.ResourceDatabasePopulator;
import infra.test.annotation.Commit;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.transaction.AfterTransaction;
import infra.test.context.transaction.BeforeTransaction;
import infra.test.context.transaction.TestTransaction;
import infra.test.jdbc.JdbcTestUtils;
import infra.test.transaction.TransactionAssert;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.annotation.Propagation;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;

/**
 * JUnit-based integration tests that verify support for programmatic transaction
 * management within the <em>TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@Transactional
class ProgrammaticTxMgmtTests {

  String sqlScriptEncoding;
  JdbcTemplate jdbcTemplate;
  String methodName;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @BeforeEach
  void trackTestName(TestInfo testInfo) {
    this.methodName = testInfo.getTestMethod().get().getName();
  }

  @BeforeTransaction
  void beforeTransaction() {
    deleteFromTables("user");
    executeSqlScript("classpath:/infra/test/context/jdbc/data.sql", false);
  }

  @AfterTransaction
  void afterTransaction() {
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
  void isActiveWithNonExistentTransactionContext() {
    assertThat(TestTransaction.isActive()).isFalse();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void flagForRollbackWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::flagForRollback);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void flagForCommitWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::flagForCommit);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void isFlaggedForRollbackWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::isFlaggedForRollback);
  }

  @Test
  @Transactional(propagation = Propagation.NEVER)
  void startTxWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::start);
  }

  @Test
  void startTxWithExistingTransaction() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::start);
  }

  @Test
  @Transactional(propagation = Propagation.NEVER)
  void endTxWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::end);
  }

  @Test
  void commitTxAndStartNewTx() {
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

    executeSqlScript("classpath:/infra/test/context/jdbc/data-add-dogbert.sql", false);
    assertUsers("Dogbert");

    TestTransaction.start();
    TransactionAssert.assertThatTransaction().isActive();
    assertThat(TestTransaction.isActive()).isTrue();
  }

  @Test
  void commitTxButDoNotStartNewTx() {
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

    executeSqlScript("classpath:/infra/test/context/jdbc/data-add-dogbert.sql", false);
    assertUsers("Dogbert");
  }

  @Test
  void rollbackTxAndStartNewTx() {
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

    executeSqlScript("classpath:/infra/test/context/jdbc/data-add-dogbert.sql", false);
    assertUsers("Dilbert", "Dogbert");
  }

  @Test
  void rollbackTxButDoNotStartNewTx() {
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
  void rollbackTxAndStartNewTxWithDefaultCommitSemantics() {
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

    executeSqlScript("classpath:/infra/test/context/jdbc/data-add-dogbert.sql", false);
    assertUsers("Dilbert", "Dogbert");
  }

  protected int deleteFromTables(String... names) {
    return JdbcTestUtils.deleteFromTables(this.jdbcTemplate, names);
  }

  protected void executeSqlScript(String sqlResourcePath, boolean continueOnError) throws DataAccessException {
    Resource resource = this.applicationContext.getResource(sqlResourcePath);
    new ResourceDatabasePopulator(continueOnError, false, this.sqlScriptEncoding, resource).execute(jdbcTemplate.getDataSource());
  }

  private void assertUsers(String... users) {
    List<String> expected = Arrays.asList(users);
    Collections.sort(expected);
    List<String> actual = jdbcTemplate.queryForList("select name from user", String.class);
    Collections.sort(actual);
    assertThat(actual).as("Users in database;").isEqualTo(expected);
  }

  @Configuration
  static class Config {

    @Bean
    PlatformTransactionManager transactionManager() {
      return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()//
              .generateUniqueName(true)//
              .addScript("classpath:/infra/test/context/jdbc/schema.sql") //
              .build();
    }
  }

}
