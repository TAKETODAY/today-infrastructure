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

package infra.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
import infra.test.context.ContextConfiguration;
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
 * This class is a copy of
 * {@link infra.test.context.transaction.programmatic.ProgrammaticTxMgmtTests}
 * that has been modified to use {@link InfraClassRule} and {@link InfraMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4.class)
@ContextConfiguration
@Transactional
public class ProgrammaticTxMgmtSpringRuleTests {

  @ClassRule
  public static final InfraClassRule applicationClassRule = new InfraClassRule();

  @Rule
  public final InfraMethodRule infraMethodRule = new InfraMethodRule();

  @Rule
  public TestName testName = new TestName();

  String sqlScriptEncoding;
  JdbcTemplate jdbcTemplate;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @BeforeTransaction
  void beforeTransaction() {
    deleteFromTables("user");
    executeSqlScript("classpath:/infra/test/context/jdbc/data.sql", false);
  }

  @AfterTransaction
  void afterTransaction() {
    String method = this.testName.getMethodName();
    switch (method) {
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
        fail("missing 'after transaction' assertion for test method: " + method);
      }
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void isActiveWithNonExistentTransactionContext() {
    assertThat(TestTransaction.isActive()).isFalse();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void flagForRollbackWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::flagForRollback);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void flagForCommitWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::flagForCommit);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void isFlaggedForRollbackWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::isFlaggedForRollback);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void startTxWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::start);
  }

  @Test
  public void startTxWithExistingTransaction() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::start);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void endTxWithNonExistentTransactionContext() {
    assertThatIllegalStateException().isThrownBy(TestTransaction::end);
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

    executeSqlScript("classpath:/infra/test/context/jdbc/data-add-dogbert.sql", false);
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

    executeSqlScript("classpath:/infra/test/context/jdbc/data-add-dogbert.sql", false);
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

    executeSqlScript("classpath:/infra/test/context/jdbc/data-add-dogbert.sql", false);
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
