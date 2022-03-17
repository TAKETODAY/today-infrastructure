/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.datasource.embedded.AutoCommitDisabledH2EmbeddedDatabaseConfigurer;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabase;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
class H2DatabasePopulatorTests extends AbstractDatabasePopulatorTests {

  @Override
  protected EmbeddedDatabaseType getEmbeddedDatabaseType() {
    return EmbeddedDatabaseType.H2;
  }

  /**
   * https://jira.spring.io/browse/SPR-15896
   *
   * @since 4.0
   */
  @Test
  void scriptWithH2Alias() throws Exception {
    databasePopulator.addScript(usersSchema());
    databasePopulator.addScript(resource("db-test-data-h2-alias.sql"));
    // Set statement separator to double newline so that ";" is not
    // considered a statement separator within the source code of the
    // aliased function 'REVERSE'.
    databasePopulator.setSeparator("\n\n");
    DatabasePopulator.execute(databasePopulator, db);
    String sql = "select REVERSE(first_name) from users where last_name='Brannen'";
    assertThat(jdbcTemplate.queryForObject(sql, String.class)).isEqualTo("maS");
  }

  /**
   * https://github.com/spring-projects/spring-framework/issues/27008
   *
   * @since 4.0
   */
  @Test
  void automaticallyCommitsIfAutoCommitIsDisabled() throws Exception {
    EmbeddedDatabase database = null;
    try {
      EmbeddedDatabaseFactory databaseFactory = new EmbeddedDatabaseFactory();
      databaseFactory.setDatabaseConfigurer(new AutoCommitDisabledH2EmbeddedDatabaseConfigurer());
      database = databaseFactory.getDatabase();

      assertAutoCommitDisabledPreconditions(database);

      // Set up schema
      databasePopulator.setScripts(usersSchema());
      DatabasePopulator.execute(databasePopulator, database);
      assertThat(selectFirstNames(database)).isEmpty();

      // Insert data
      databasePopulator.setScripts(resource("users-data.sql"));
      DatabasePopulator.execute(databasePopulator, database);
      assertThat(selectFirstNames(database)).containsExactly("Sam");
    }
    finally {
      if (database != null) {
        database.shutdown();
      }
    }
  }

  /**
   * DatabasePopulator.execute() will obtain a new Connection, so we're
   * really just testing the configuration of the database here.
   */
  private void assertAutoCommitDisabledPreconditions(DataSource dataSource) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    assertThat(connection.getAutoCommit()).as("auto-commit").isFalse();
    assertThat(DataSourceUtils.isConnectionTransactional(connection, dataSource)).as("transactional").isFalse();
    connection.close();
  }

  private List<String> selectFirstNames(DataSource dataSource) {
    return new JdbcTemplate(dataSource).queryForList("select first_name from users", String.class);
  }

}
