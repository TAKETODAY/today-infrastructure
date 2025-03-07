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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

import infra.jdbc.BadSqlGrammarException;
import infra.test.annotation.DirtiesContext;
import infra.test.context.jdbc.Sql.ExecutionPhase;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.transaction.AfterTransaction;

import static infra.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Transactional integration tests for {@link Sql @Sql} that verify proper
 * support for {@link ExecutionPhase#AFTER_TEST_METHOD}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(EmptyDatabaseConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@DirtiesContext
class TransactionalAfterTestMethodSqlScriptsTests extends AbstractTransactionalTests {

  String testName;

  @BeforeEach
  void trackTestName(TestInfo testInfo) {
    this.testName = testInfo.getTestMethod().get().getName();
  }

  @AfterTransaction
  void afterTransaction() {
    if ("test01".equals(testName)) {
      // Should throw a BadSqlGrammarException after test01, assuming 'drop-schema.sql' was executed
      assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() -> assertNumUsers(99));
    }
  }

  @Test
  @SqlGroup({
          @Sql({ "schema.sql", "data.sql" }),
          @Sql(scripts = "drop-schema.sql", executionPhase = AFTER_TEST_METHOD)
  })
    // test## is required for @TestMethodOrder.
  void test01() {
    assertNumUsers(1);
  }

  @Test
  @Sql({ "schema.sql", "data.sql", "data-add-dogbert.sql" })
    // test## is required for @TestMethodOrder.
  void test02() {
    assertNumUsers(2);
  }

}
