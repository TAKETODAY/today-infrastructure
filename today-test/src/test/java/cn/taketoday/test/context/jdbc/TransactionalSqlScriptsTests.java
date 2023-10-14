/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context.jdbc;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

/**
 * Transactional integration tests for {@link Sql @Sql} support.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(EmptyDatabaseConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@Sql({ "recreate-schema.sql", "data.sql" })
@DirtiesContext
class TransactionalSqlScriptsTests extends AbstractTransactionalTests {

  @Test
  void classLevelScripts() {
    assertNumUsers(1);
  }

  @Test
  @Sql({ "recreate-schema.sql", "data.sql", "data-add-dogbert.sql" })
  void methodLevelScripts() {
    assertNumUsers(2);
  }

  @Nested
  class NestedTransactionalSqlScriptsTests {

    @Test
    void classLevelScripts() {
      assertNumUsers(1);
    }

    @Test
    @Sql({ "recreate-schema.sql", "data.sql", "data-add-dogbert.sql" })
    void methodLevelScripts() {
      assertNumUsers(2);
    }

  }

}

