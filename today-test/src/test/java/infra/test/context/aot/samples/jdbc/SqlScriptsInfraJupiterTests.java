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

package infra.test.context.aot.samples.jdbc;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.jdbc.core.JdbcTemplate;
import infra.test.annotation.DirtiesContext;
import infra.test.context.TestPropertySource;
import infra.test.context.jdbc.EmptyDatabaseConfig;
import infra.test.context.jdbc.Sql;
import infra.test.context.jdbc.SqlMergeMode;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.transaction.annotation.Transactional;

import static infra.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static infra.test.jdbc.JdbcTestUtils.countRowsInTable;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(EmptyDatabaseConfig.class)
@Transactional
@SqlMergeMode(MERGE)
@Sql("/infra/test/context/jdbc/schema.sql")
@DirtiesContext
@TestPropertySource(properties = "test.engine = jupiter")
public class SqlScriptsInfraJupiterTests {

  @Test
  @Sql
    // default script --> infra/test/context/aot/samples/jdbc/SqlScriptsInfraJupiterTests.test.sql
  void test(@Autowired JdbcTemplate jdbcTemplate) {
    assertThat(countRowsInTable(jdbcTemplate, "user")).isEqualTo(1);
  }

}
