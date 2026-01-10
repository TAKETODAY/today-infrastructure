/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
