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

import org.junit.jupiter.api.Test;

import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.transaction.AfterTransaction;
import infra.test.context.transaction.BeforeTransaction;

/**
 * Transactional integration tests that verify rollback semantics for
 * {@link Sql @Sql} support.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(PopulatedSchemaDatabaseConfig.class)
@DirtiesContext
class PopulatedSchemaTransactionalSqlScriptsTests extends AbstractTransactionalTests {

  @BeforeTransaction
  @AfterTransaction
  void verifyPreAndPostTransactionDatabaseState() {
    assertNumUsers(0);
  }

  @Test
  @SqlGroup(@Sql("data-add-dogbert.sql"))
  void methodLevelScripts() {
    assertNumUsers(1);
  }

}
