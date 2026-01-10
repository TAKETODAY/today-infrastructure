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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.test.annotation.DirtiesContext;
import infra.test.context.jdbc.Sql.ExecutionPhase;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static infra.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static infra.test.context.jdbc.SqlMergeMode.MergeMode.OVERRIDE;

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
