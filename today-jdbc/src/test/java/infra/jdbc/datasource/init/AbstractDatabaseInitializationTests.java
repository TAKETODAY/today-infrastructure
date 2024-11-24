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

package infra.jdbc.datasource.init;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import infra.core.io.ClassRelativeResourceLoader;
import infra.core.io.Resource;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.embedded.EmbeddedDatabase;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;
import infra.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests involving database initialization.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractDatabaseInitializationTests {

  private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());

  EmbeddedDatabase db;

  JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    db = new EmbeddedDatabaseBuilder().setType(getEmbeddedDatabaseType()).build();
    jdbcTemplate = new JdbcTemplate(db);
  }

  @AfterEach
  void shutDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clear();
      TransactionSynchronizationManager.unbindResource(db);
    }
    db.shutdown();
  }

  abstract EmbeddedDatabaseType getEmbeddedDatabaseType();

  Resource resource(String path) {
    return resourceLoader.getResource(path);
  }

  Resource defaultSchema() {
    return resource("db-schema.sql");
  }

  Resource usersSchema() {
    return resource("users-schema.sql");
  }

  void assertUsersDatabaseCreated(String... lastNames) {
    for (String lastName : lastNames) {
      String sql = "select count(0) from users where last_name = ?";
      Integer result = jdbcTemplate.queryForObject(sql, Integer.class, lastName);
      assertThat(result).as("user with last name [" + lastName + "]").isEqualTo(1);
    }
  }

}
