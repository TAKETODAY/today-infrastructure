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

package infra.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import infra.beans.factory.annotation.Autowired;
import infra.jdbc.core.JdbcTemplate;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.NestedTestConfiguration.EnclosingConfiguration;
import infra.test.context.jdbc.PopulatedSchemaDatabaseConfig;
import infra.test.context.jdbc.Sql;
import infra.test.context.jdbc.SqlMergeMode;
import infra.test.context.jdbc.SqlMergeMode.MergeMode;
import infra.test.context.jdbc.merging.AbstractSqlMergeModeTests;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.transaction.AfterTransaction;
import infra.test.context.transaction.BeforeTransaction;
import infra.test.jdbc.JdbcTestUtils;
import infra.transaction.annotation.Transactional;

import static infra.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static infra.test.context.jdbc.SqlMergeMode.MergeMode.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@link Nested @Nested} test classes in
 * conjunction with the {@link InfraExtension}, {@link Sql @Sql}, and
 * {@link Transactional @Transactional} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(PopulatedSchemaDatabaseConfig.class)
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
class SqlScriptNestedTests {

  @Autowired
  JdbcTemplate jdbcTemplate;

  @BeforeTransaction
  @AfterTransaction
  void checkInitialDatabaseState() {
    assertThat(countRowsInTable("user")).isEqualTo(0);
  }

  @Test
  @Sql("/infra/test/context/jdbc/data.sql")
  void sqlScripts() {
    assertThat(countRowsInTable("user")).isEqualTo(1);
  }

  private int countRowsInTable(String tableName) {
    return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
  }

  @Nested
  class NestedTests {

    @BeforeTransaction
    @AfterTransaction
    void checkInitialDatabaseState() {
      assertThat(countRowsInTable("user")).isEqualTo(0);
    }

    @Test
    @Sql("/infra/test/context/jdbc/data.sql")
    void nestedSqlScripts() {
      assertThat(countRowsInTable("user")).isEqualTo(1);
    }
  }

  @Nested
  @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
  @Sql({
          "/infra/test/context/jdbc/recreate-schema.sql",
          "/infra/test/context/jdbc/data-add-catbert.sql"
  })
  class NestedSqlMergeModeTests extends AbstractSqlMergeModeTests {

    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.INHERIT)
    @SqlMergeMode(MergeMode.MERGE)
    class NestedClassLevelMergeSqlMergeModeTests {

      @Test
      void classLevelScripts() {
        assertUsers("Catbert");
      }

      @Test
      @Sql("/infra/test/context/jdbc/data-add-dogbert.sql")
      void merged() {
        assertUsers("Catbert", "Dogbert");
      }

      @Test
      @Sql({
              "/infra/test/context/jdbc/recreate-schema.sql",
              "/infra/test/context/jdbc/data.sql",
              "/infra/test/context/jdbc/data-add-dogbert.sql",
              "/infra/test/context/jdbc/data-add-catbert.sql"
      })
      @SqlMergeMode(MergeMode.OVERRIDE)
      void overridden() {
        assertUsers("Dilbert", "Dogbert", "Catbert");
      }
    }

    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.INHERIT)
    @SqlMergeMode(OVERRIDE)
    class ClassLevelOverrideSqlMergeModeTests {

      @Test
      void classLevelScripts() {
        assertUsers("Catbert");
      }

      @Test
      @Sql("/infra/test/context/jdbc/data-add-dogbert.sql")
      @SqlMergeMode(MERGE)
      void merged() {
        assertUsers("Catbert", "Dogbert");
      }

      @Test
      @Sql({
              "/infra/test/context/jdbc/recreate-schema.sql",
              "/infra/test/context/jdbc/data.sql",
              "/infra/test/context/jdbc/data-add-dogbert.sql",
              "/infra/test/context/jdbc/data-add-catbert.sql"
      })
      void overridden() {
        assertUsers("Dilbert", "Dogbert", "Catbert");
      }
    }

  }

}
