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

package cn.taketoday.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.jdbc.EmptyDatabaseConfig;
import cn.taketoday.test.context.jdbc.Sql;
import cn.taketoday.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class is a JUnit 4 based copy of
 * {@link cn.taketoday.test.context.jdbc.TransactionalSqlScriptsTests}
 * that has been modified to use {@link ApplicationClassRule} and {@link ApplicationMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4.class)
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Sql({ "../../jdbc/schema.sql", "../../jdbc/data.sql" })
@DirtiesContext
public class TransactionalSqlScriptsSpringRuleTests {

  @ClassRule
  public static final ApplicationClassRule applicationClassRule = new ApplicationClassRule();

  @Rule
  public final ApplicationMethodRule applicationMethodRule = new ApplicationMethodRule();

  @Rule
  public Timeout timeout = Timeout.builder().withTimeout(10, TimeUnit.SECONDS).build();

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Test
  public void classLevelScripts() {
    assertNumUsers(1);
  }

  @Test
  @Sql({ "../../jdbc/drop-schema.sql", "../../jdbc/schema.sql", "../../jdbc/data.sql", "../../jdbc/data-add-dogbert.sql" })
  public void methodLevelScripts() {
    assertNumUsers(2);
  }

  private void assertNumUsers(int expected) {
    assertThat(countRowsInTable("user")).as("Number of rows in the 'user' table.").isEqualTo(expected);
  }

  private int countRowsInTable(String tableName) {
    return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
  }

}
