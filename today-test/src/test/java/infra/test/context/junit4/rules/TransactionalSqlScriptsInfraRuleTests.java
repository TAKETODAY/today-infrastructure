/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.util.concurrent.TimeUnit;

import infra.beans.factory.annotation.Autowired;
import infra.jdbc.core.JdbcTemplate;
import infra.test.annotation.DirtiesContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.jdbc.EmptyDatabaseConfig;
import infra.test.context.jdbc.Sql;
import infra.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class is a JUnit 4 based copy of
 * {@link infra.test.context.jdbc.TransactionalSqlScriptsTests}
 * that has been modified to use {@link InfraClassRule} and {@link InfraMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4.class)
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Sql({ "../../jdbc/schema.sql", "../../jdbc/data.sql" })
@DirtiesContext
public class TransactionalSqlScriptsInfraRuleTests {

  @ClassRule
  public static final InfraClassRule applicationClassRule = new InfraClassRule();

  @Rule
  public final InfraMethodRule infraMethodRule = new InfraMethodRule();

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
