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

package cn.taketoday.jdbc.datasource.init;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.Connection;
import java.sql.SQLException;

import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Abstract base class for integration tests for {@link ResourceDatabasePopulator}
 * and {@link DatabasePopulator}.
 *
 * @author Dave Syer
 * @author Sam Brannen
 * @author Oliver Gierke
 */
@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractDatabasePopulatorTests extends AbstractDatabaseInitializationTests {

  private static final String COUNT_DAVE_SQL = "select COUNT(NAME) from T_TEST where NAME='Dave'";

  private static final String COUNT_KEITH_SQL = "select COUNT(NAME) from T_TEST where NAME='Keith'";

  protected final ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

  @Test
  void scriptWithSingleLineCommentsAndFailedDrop() throws Exception {
    databasePopulator.addScript(resource("db-schema-failed-drop-comments.sql"));
    databasePopulator.addScript(resource("db-test-data.sql"));
    databasePopulator.setIgnoreFailedDrops(true);
    DatabasePopulator.execute(databasePopulator, db);
    assertTestDatabaseCreated();
  }

  @Test
  void scriptWithStandardEscapedLiteral() throws Exception {
    databasePopulator.addScript(defaultSchema());
    databasePopulator.addScript(resource("db-test-data-escaped-literal.sql"));
    DatabasePopulator.execute(databasePopulator, db);
    assertTestDatabaseCreated("'Keith'");
  }

  @Test
  void scriptWithMySqlEscapedLiteral() throws Exception {
    databasePopulator.addScript(defaultSchema());
    databasePopulator.addScript(resource("db-test-data-mysql-escaped-literal.sql"));
    DatabasePopulator.execute(databasePopulator, db);
    assertTestDatabaseCreated("\\$Keith\\$");
  }

  @Test
  void scriptWithMultipleStatements() throws Exception {
    databasePopulator.addScript(defaultSchema());
    databasePopulator.addScript(resource("db-test-data-multiple.sql"));
    DatabasePopulator.execute(databasePopulator, db);
    assertThat(jdbcTemplate.queryForObject(COUNT_KEITH_SQL, Integer.class)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(COUNT_DAVE_SQL, Integer.class)).isEqualTo(1);
  }

  @Test
  void scriptWithMultipleStatementsAndLongSeparator() throws Exception {
    databasePopulator.addScript(defaultSchema());
    databasePopulator.addScript(resource("db-test-data-endings.sql"));
    databasePopulator.setSeparator("@@");
    DatabasePopulator.execute(databasePopulator, db);
    assertThat(jdbcTemplate.queryForObject(COUNT_KEITH_SQL, Integer.class)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(COUNT_DAVE_SQL, Integer.class)).isEqualTo(1);
  }

  @Test
  void scriptWithMultipleStatementsAndWhitespaceSeparator() throws Exception {
    databasePopulator.addScript(defaultSchema());
    databasePopulator.addScript(resource("db-test-data-whitespace.sql"));
    databasePopulator.setSeparator("/\n");
    DatabasePopulator.execute(databasePopulator, db);
    assertThat(jdbcTemplate.queryForObject(COUNT_KEITH_SQL, Integer.class)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(COUNT_DAVE_SQL, Integer.class)).isEqualTo(1);
  }

  @Test
  void scriptWithMultipleStatementsAndNewlineSeparator() throws Exception {
    databasePopulator.addScript(defaultSchema());
    databasePopulator.addScript(resource("db-test-data-newline.sql"));
    DatabasePopulator.execute(databasePopulator, db);
    assertThat(jdbcTemplate.queryForObject(COUNT_KEITH_SQL, Integer.class)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(COUNT_DAVE_SQL, Integer.class)).isEqualTo(1);
  }

  @Test
  void scriptWithMultipleStatementsAndMultipleNewlineSeparator() throws Exception {
    databasePopulator.addScript(defaultSchema());
    databasePopulator.addScript(resource("db-test-data-multi-newline.sql"));
    databasePopulator.setSeparator("\n\n");
    DatabasePopulator.execute(databasePopulator, db);
    assertThat(jdbcTemplate.queryForObject(COUNT_KEITH_SQL, Integer.class)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(COUNT_DAVE_SQL, Integer.class)).isEqualTo(1);
  }

  @Test
  void scriptWithEolBetweenTokens() throws Exception {
    databasePopulator.addScript(usersSchema());
    databasePopulator.addScript(resource("users-data.sql"));
    DatabasePopulator.execute(databasePopulator, db);
    assertUsersDatabaseCreated("Brannen");
  }

  @Test
  void scriptWithCommentsWithinStatements() throws Exception {
    databasePopulator.addScript(usersSchema());
    databasePopulator.addScript(resource("users-data-with-comments.sql"));
    DatabasePopulator.execute(databasePopulator, db);
    assertUsersDatabaseCreated("Brannen", "Hoeller");
  }

  @Test
  void scriptWithoutStatementSeparator() throws Exception {
    databasePopulator.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR);
    databasePopulator.addScript(resource("drop-users-schema.sql"));
    databasePopulator.addScript(resource("users-schema-without-separator.sql"));
    databasePopulator.addScript(resource("users-data-without-separator.sql"));
    DatabasePopulator.execute(databasePopulator, db);

    assertUsersDatabaseCreated("Brannen");
  }

  @Test
  void constructorWithMultipleScriptResources() throws Exception {
    final ResourceDatabasePopulator populator = new ResourceDatabasePopulator(usersSchema(),
            resource("users-data-with-comments.sql"));
    DatabasePopulator.execute(populator, db);
    assertUsersDatabaseCreated("Brannen", "Hoeller");
  }

  @Test
  void scriptWithSelectStatements() throws Exception {
    databasePopulator.addScript(defaultSchema());
    databasePopulator.addScript(resource("db-test-data-select.sql"));
    DatabasePopulator.execute(databasePopulator, db);
    assertThat(jdbcTemplate.queryForObject(COUNT_KEITH_SQL, Integer.class)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(COUNT_DAVE_SQL, Integer.class)).isEqualTo(1);
  }

  /**
   * See SPR-9457
   */
  @Test
  void usesBoundConnectionIfAvailable() throws SQLException {
    TransactionSynchronizationManager.initSynchronization();
    Connection connection = DataSourceUtils.getConnection(db);
    DatabasePopulator populator = mock(DatabasePopulator.class);
    DatabasePopulator.execute(populator, db);
    verify(populator).populate(connection);
  }

  /**
   * See SPR-9781
   */
  @Test
  @Timeout(1)
  void executesHugeScriptInReasonableTime() throws SQLException {
    databasePopulator.addScript(defaultSchema());
    databasePopulator.addScript(resource("db-test-data-huge.sql"));
    DatabasePopulator.execute(databasePopulator, db);
  }

  private void assertTestDatabaseCreated() {
    assertTestDatabaseCreated("Keith");
  }

  private void assertTestDatabaseCreated(String name) {
    assertThat(jdbcTemplate.queryForObject("select NAME from T_TEST", String.class)).isEqualTo(name);
  }

}
