/*
 * Copyright 2017 - 2026 the TODAY authors.
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
