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

package infra.test.context.jdbc.merging;

import org.junit.jupiter.api.Test;

import infra.test.context.jdbc.Sql;
import infra.test.context.jdbc.SqlMergeMode;

import static infra.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static infra.test.context.jdbc.SqlMergeMode.MergeMode.OVERRIDE;

/**
 * Transactional integration tests that verify proper merging and overriding support
 * for class-level and method-level {@link Sql @Sql} declarations when
 * {@link SqlMergeMode @SqlMergeMode} is declared at the class level with
 * {@link SqlMergeMode.MergeMode#MERGE MERGE} mode.
 *
 * @author Sam Brannen
 * @author Dmitry Semukhin
 * @since 4.0
 */
@Sql({ "../recreate-schema.sql", "../data-add-catbert.sql" })
@SqlMergeMode(MERGE)
class ClassLevelMergeSqlMergeModeTests extends AbstractSqlMergeModeTests {

  @Test
  void classLevelScripts() {
    assertUsers("Catbert");
  }

  @Test
  @Sql("../data-add-dogbert.sql")
  void merged() {
    assertUsers("Catbert", "Dogbert");
  }

  @Test
  @Sql({ "../recreate-schema.sql", "../data.sql", "../data-add-dogbert.sql", "../data-add-catbert.sql" })
  @SqlMergeMode(OVERRIDE)
  void overridden() {
    assertUsers("Dilbert", "Dogbert", "Catbert");
  }

}
