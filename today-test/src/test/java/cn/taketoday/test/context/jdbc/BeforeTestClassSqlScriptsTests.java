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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.jdbc.Sql.ExecutionPhase;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static cn.taketoday.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static cn.taketoday.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static cn.taketoday.test.context.jdbc.SqlMergeMode.MergeMode.OVERRIDE;

/**
 * Verifies that {@link Sql @Sql} with {@link ExecutionPhase#BEFORE_TEST_CLASS}
 * is run before all tests in the class have been run.
 *
 * @author Andreas Ahlenstorf
 * @author Sam Brannen
 */
@JUnitConfig(EmptyDatabaseConfig.class)
@DirtiesContext
@Sql(scripts = { "recreate-schema.sql", "data-add-catbert.sql" }, executionPhase = BEFORE_TEST_CLASS)
class BeforeTestClassSqlScriptsTests extends AbstractTransactionalTests {

  @Test
  void classLevelScriptsHaveBeenRun() {
    assertUsers("Catbert");
  }

  @Test
  @Sql("data-add-dogbert.sql")
  @SqlMergeMode(MERGE)
  void mergeDoesNotAffectClassLevelPhase() {
    assertUsers("Catbert", "Dogbert");
  }

  @Test
  @Sql({ "data-add-dogbert.sql" })
  @SqlMergeMode(OVERRIDE)
  void overrideDoesNotAffectClassLevelPhase() {
    assertUsers("Catbert", "Dogbert");
  }

  @Nested
  class NestedBeforeTestClassSqlScriptsTests {

    @Test
    void classLevelScriptsHaveBeenRun() {
      assertUsers("Catbert");
    }

    @Test
    @Sql("data-add-dogbert.sql")
    @SqlMergeMode(MERGE)
    void mergeDoesNotAffectClassLevelPhase() {
      assertUsers("Catbert", "Dogbert");
    }

    @Test
    @Sql({ "data-add-dogbert.sql" })
    @SqlMergeMode(OVERRIDE)
    void overrideDoesNotAffectClassLevelPhase() {
      assertUsers("Catbert", "Dogbert");
    }

  }

}
