/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.context.jdbc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.sql.DataSource;

import infra.context.support.GenericApplicationContext;
import infra.aot.hint.RuntimeHints;
import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DriverManagerDataSource;
import infra.jdbc.datasource.TransactionAwareDataSourceProxy;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.test.context.TestContext;
import infra.test.jdbc.JdbcTestUtils;
import infra.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SqlListenerAdditionalTests {

  private final SqlScriptsTestExecutionListener listener = new SqlScriptsTestExecutionListener();

  @Test
  void statementsExecuteWithTransactionAwareProxyAndJdbcUtilities() throws Exception {
    DataSource dataSource = dataSource("sqlAdditional");
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      application.registerBean("dataSource", DataSource.class, () -> new TransactionAwareDataSourceProxy(dataSource));
      application.registerBean("transactionManager", PlatformTransactionManager.class, () -> new DataSourceTransactionManager(dataSource));
      application.refresh();
      TestContext context = context(application, Statements.class, "test");
      listener.beforeTestMethod(context);
      JdbcTemplate jdbc = new JdbcTemplate(dataSource);
      assertThat(JdbcTestUtils.countRowsInTable(jdbc, "coverage_items")).isEqualTo(2);
      assertThat(JdbcTestUtils.countRowsInTableWhere(jdbc, "coverage_items", "id > 1")).isEqualTo(1);
      assertThat(JdbcTestUtils.deleteFromTableWhere(jdbc, "coverage_items", "id = ?", 1)).isEqualTo(1);
      assertThat(JdbcTestUtils.deleteFromTableWhere(jdbc, "coverage_items", " ")).isEqualTo(1);
      assertThat(JdbcTestUtils.deleteFromTables(jdbc, "coverage_items")).isZero();
      JdbcTestUtils.dropTables(jdbc, "coverage_items");
      listener.afterTestMethod(context);
    }
  }

  @Test
  void detectsMismatchedDataSourceAndMissingTransactionManager() throws Exception {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      application.registerBean("dataSource", DataSource.class, () -> dataSource("one"));
      application.registerBean("transactionManager", PlatformTransactionManager.class,
              () -> new DataSourceTransactionManager(dataSource("two")));
      application.refresh();
      TestContext context = context(application, Statements.class, "test");
      assertThatIllegalStateException().isThrownBy(() -> listener.beforeTestMethod(context))
              .withMessageContaining("is not the one associated with");
    }
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      application.refresh();
      assertThatIllegalStateException().isThrownBy(() -> listener.beforeTestMethod(context(application, Statements.class, "isolated")))
              .withMessageContaining("without a PlatformTransactionManager");
      assertThatIllegalStateException().isThrownBy(() -> listener.beforeTestMethod(context(application, Statements.class, "test")))
              .withMessageContaining("supply at least a DataSource");
    }
  }

  @Test
  void transactionManagerWithoutDataSourceCannotExecuteScripts() throws Exception {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      application.registerBean("transactionManager", PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class));
      application.refresh();
      assertThatIllegalStateException().isThrownBy(() -> listener.beforeTestMethod(context(application, Statements.class, "test")))
              .withMessageContaining("could not obtain DataSource");
    }
  }

  @Test
  void methodCannotUseClassExecutionPhaseAndDefaultScriptMustExist() throws Exception {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      assertThatIllegalArgumentException().isThrownBy(() -> listener.beforeTestMethod(context(application, Statements.class, "invalid")))
              .withMessageContaining("cannot be used on methods");
      assertThatIllegalStateException().isThrownBy(() -> listener.beforeTestMethod(context(application, Statements.class, "missing")))
              .withMessageContaining("Could not detect default SQL script");
    }
  }

  private DataSource dataSource(String name) {
    return new DriverManagerDataSource("jdbc:hsqldb:mem:" + name, "sa", "");
  }

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "TRACE", "DEBUG", "OFF" })
  void sqlErrorModesAndClassPhasesAreIndependentOfDiagnostics(LogLevel level) throws Exception {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = SqlScriptsTestExecutionListener.class.getName();
    LogLevel previous = logging.getLoggerConfiguration(name).getConfiguredLevel();
    logging.setLogLevel(name, level);
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      DataSource dataSource = dataSource("sqlModes" + level);
      application.registerBean("dataSource", DataSource.class, () -> dataSource);
      application.refresh();
      TestContext context = context(application, ErrorModes.class, "test");
      listener.beforeTestClass(context);
      listener.beforeTestMethod(context);
      assertThat(JdbcTestUtils.countRowsInTable(new JdbcTemplate(dataSource), "mode_items")).isEqualTo(1);
      listener.afterTestMethod(context);
      listener.afterTestClass(context);
      assertThatIllegalStateException().isThrownBy(() ->
              listener.beforeTestClass(context(application, MissingClassScript.class, "test")))
              .withMessageContaining("Could not detect default SQL script for test class");
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

  @Test
  void aotRegistersClassAndMethodClasspathScriptsWithoutExecutingThem() {
    RuntimeHints hints = new RuntimeHints();
    listener.processAheadOfTime(hints, ScriptHints.class, getClass().getClassLoader());
    assertThat(hints.resources().resourcePatternHints()).isNotEmpty();
  }

  private TestContext context(GenericApplicationContext application, Class<?> type, String method) throws Exception {
    TestContext context = mock(TestContext.class);
    org.mockito.BDDMockito.<Class<?>>given(context.getTestClass()).willReturn(type);
    given(context.getTestMethod()).willReturn(type.getDeclaredMethod(method));
    given(context.getApplicationContext()).willReturn(application);
    return context;
  }

  static class Statements {
    @Sql(statements = { "CREATE TABLE coverage_items (id int)", " ", " INSERT INTO coverage_items VALUES (1), (2) " })
    void test() { }

    @Sql(statements = "SELECT 1", config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
    void isolated() { }

    @Sql(statements = "SELECT 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
    void invalid() { }

    @Sql
    void missing() { }
  }

  @Sql(statements = "CREATE TABLE mode_items (id int)", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
  @Sql(statements = "DROP TABLE mode_items", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
  static class ErrorModes {
    @Sql(statements = { "INVALID SQL", "INSERT INTO mode_items VALUES (1)" },
            config = @SqlConfig(errorMode = SqlConfig.ErrorMode.CONTINUE_ON_ERROR))
    @Sql(statements = "DROP TABLE missing_items", config = @SqlConfig(errorMode = SqlConfig.ErrorMode.IGNORE_FAILED_DROPS))
    void test() { }
  }

  @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
  static class MissingClassScript {
    void test() { }
  }

  @Sql("classpath:/infra/test/context/jdbc/schema.sql")
  static class ScriptHints {
    @Sql({ "classpath:/infra/test/context/jdbc/data.sql", "file:/not-executed.sql" })
    void test() { }
  }

}
