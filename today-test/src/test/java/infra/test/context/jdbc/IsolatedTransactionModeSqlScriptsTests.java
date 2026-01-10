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

import org.junit.jupiter.api.Test;

import infra.test.annotation.DirtiesContext;
import infra.test.context.jdbc.SqlConfig.TransactionMode;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.transaction.AfterTransaction;
import infra.test.context.transaction.BeforeTransaction;

/**
 * Transactional integration tests that verify commit semantics for
 * {@link SqlConfig#transactionMode} and {@link TransactionMode#ISOLATED}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(PopulatedSchemaDatabaseConfig.class)
@DirtiesContext
class IsolatedTransactionModeSqlScriptsTests extends AbstractTransactionalTests {

  @BeforeTransaction
  void beforeTransaction() {
    assertNumUsers(0);
  }

  @Test
  @SqlGroup(@Sql(scripts = "data-add-dogbert.sql", config = @SqlConfig(transactionMode = TransactionMode.ISOLATED)))
  void methodLevelScripts() {
    assertNumUsers(1);
  }

  @AfterTransaction
  void afterTransaction() {
    assertNumUsers(1);
  }

}
