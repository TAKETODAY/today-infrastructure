/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration;
import cn.taketoday.test.context.jdbc.PopulatedSchemaDatabaseConfig;
import cn.taketoday.test.context.jdbc.Sql;
import cn.taketoday.test.context.jdbc.SqlMergeMode;
import cn.taketoday.test.context.jdbc.SqlMergeMode.MergeMode;
import cn.taketoday.test.context.jdbc.merging.AbstractSqlMergeModeTests;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.test.jdbc.JdbcTestUtils;
import cn.taketoday.transaction.annotation.Transactional;

import static cn.taketoday.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static cn.taketoday.test.context.jdbc.SqlMergeMode.MergeMode.OVERRIDE;
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
  @Sql("/cn/taketoday/test/context/jdbc/data.sql")
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
    @Sql("/cn/taketoday/test/context/jdbc/data.sql")
    void nestedSqlScripts() {
      assertThat(countRowsInTable("user")).isEqualTo(1);
    }
  }

  @Nested
  @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
  @Sql({
          "/cn/taketoday/test/context/jdbc/recreate-schema.sql",
          "/cn/taketoday/test/context/jdbc/data-add-catbert.sql"
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
      @Sql("/cn/taketoday/test/context/jdbc/data-add-dogbert.sql")
      void merged() {
        assertUsers("Catbert", "Dogbert");
      }

      @Test
      @Sql({
              "/cn/taketoday/test/context/jdbc/recreate-schema.sql",
              "/cn/taketoday/test/context/jdbc/data.sql",
              "/cn/taketoday/test/context/jdbc/data-add-dogbert.sql",
              "/cn/taketoday/test/context/jdbc/data-add-catbert.sql"
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
      @Sql("/cn/taketoday/test/context/jdbc/data-add-dogbert.sql")
      @SqlMergeMode(MERGE)
      void merged() {
        assertUsers("Catbert", "Dogbert");
      }

      @Test
      @Sql({
              "/cn/taketoday/test/context/jdbc/recreate-schema.sql",
              "/cn/taketoday/test/context/jdbc/data.sql",
              "/cn/taketoday/test/context/jdbc/data-add-dogbert.sql",
              "/cn/taketoday/test/context/jdbc/data-add-catbert.sql"
      })
      void overridden() {
        assertUsers("Dilbert", "Dogbert", "Catbert");
      }
    }

  }

}
