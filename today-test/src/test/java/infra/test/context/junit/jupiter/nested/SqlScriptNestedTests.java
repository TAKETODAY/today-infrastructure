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
