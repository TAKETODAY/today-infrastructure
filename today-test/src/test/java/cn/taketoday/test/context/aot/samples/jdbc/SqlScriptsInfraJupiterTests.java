/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.aot.samples.jdbc;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.jdbc.EmptyDatabaseConfig;
import cn.taketoday.test.context.jdbc.Sql;
import cn.taketoday.test.context.jdbc.SqlMergeMode;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.transaction.annotation.Transactional;

import static cn.taketoday.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static cn.taketoday.test.jdbc.JdbcTestUtils.countRowsInTable;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(EmptyDatabaseConfig.class)
@Transactional
@SqlMergeMode(MERGE)
@Sql("/cn/taketoday/test/context/jdbc/schema.sql")
@DirtiesContext
@TestPropertySource(properties = "test.engine = jupiter")
public class SqlScriptsInfraJupiterTests {

  @Test
  @Sql
    // default script --> cn/taketoday/test/context/aot/samples/jdbc/SqlScriptsInfraJupiterTests.test.sql
  void test(@Autowired JdbcTemplate jdbcTemplate) {
    assertThat(countRowsInTable(jdbcTemplate, "user")).isEqualTo(1);
  }

}
