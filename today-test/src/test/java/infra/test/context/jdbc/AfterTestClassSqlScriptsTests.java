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

package infra.test.context.jdbc;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.sql.DataSource;

import infra.jdbc.BadSqlGrammarException;
import infra.jdbc.core.JdbcTemplate;
import infra.test.annotation.Commit;
import infra.test.annotation.DirtiesContext;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListeners;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.support.AbstractTestExecutionListener;
import infra.test.context.transaction.TestContextTransactionUtils;

import static infra.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static infra.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static infra.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verifies that {@link Sql @Sql} with {@link Sql.ExecutionPhase#AFTER_TEST_CLASS}
 * is run after all tests in the class have been run.
 *
 * @author Andreas Ahlenstorf
 * @author Sam Brannen
 */
@JUnitConfig(PopulatedSchemaDatabaseConfig.class)
@TestMethodOrder(OrderAnnotation.class)
@DirtiesContext
@Sql(scripts = "drop-schema.sql", executionPhase = AFTER_TEST_CLASS)
@Commit
@TestExecutionListeners(listeners = AfterTestClassSqlScriptsTests.VerifySchemaDroppedListener.class, mergeMode = MERGE_WITH_DEFAULTS)
class AfterTestClassSqlScriptsTests extends AbstractTransactionalTests {

  @Test
  @Order(1)
  @Sql("data-add-catbert.sql")
  void databaseHasBeenInitialized() {
    assertUsers("Catbert");
  }

  @Test
  @Order(2)
  @Sql("data-add-dogbert.sql")
  void databaseIsNotWipedBetweenTests() {
    assertUsers("Catbert", "Dogbert");
  }

  @Nested
  @Sql(scripts = "recreate-schema.sql", executionPhase = BEFORE_TEST_CLASS)
  @Sql(scripts = "drop-schema.sql", executionPhase = AFTER_TEST_CLASS)
  class NestedAfterTestClassSqlScriptsTests {

    @Test
    @Order(1)
    @Sql("data-add-catbert.sql")
    void databaseHasBeenInitialized() {
      assertUsers("Catbert");
    }

    @Test
    @Order(2)
    @Sql("data-add-dogbert.sql")
    void databaseIsNotWipedBetweenTests() {
      assertUsers("Catbert", "Dogbert");
    }

  }

  static class VerifySchemaDroppedListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
      // Must run before DirtiesContextTestExecutionListener. Otherwise, the
      // old data source will be removed and replaced with a new one.
      return 3001;
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
      DataSource dataSource = TestContextTransactionUtils.retrieveDataSource(testContext, null);
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

      assertThatExceptionOfType(BadSqlGrammarException.class)
              .isThrownBy(() -> jdbcTemplate.queryForList("SELECT name FROM user", String.class))
              .withMessageContaining("user");
    }
  }

}
