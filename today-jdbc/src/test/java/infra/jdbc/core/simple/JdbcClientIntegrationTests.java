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

package infra.jdbc.core.simple;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.core.io.ClassRelativeResourceLoader;
import infra.jdbc.datasource.embedded.EmbeddedDatabase;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.jdbc.datasource.init.DatabasePopulator;
import infra.jdbc.support.GeneratedKeyHolder;
import infra.jdbc.support.KeyHolder;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JdbcClient} using an embedded H2 database.
 *
 * @author Sam Brannen
 * @see JdbcClientIndexedParameterTests
 * @see JdbcClientNamedParameterTests
 */
class JdbcClientIntegrationTests {

  private static final String INSERT_WITH_JDBC_PARAMS =
          "INSERT INTO users (first_name, last_name) VALUES(?, ?)";

  private static final String INSERT_WITH_NAMED_PARAMS =
          "INSERT INTO users (first_name, last_name) VALUES(:firstName, :lastName)";

  private final EmbeddedDatabase embeddedDatabase =
          new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(DatabasePopulator.class))
                  .generateUniqueName(true)
                  .setType(EmbeddedDatabaseType.H2)
                  .addScripts("users-schema.sql", "users-data.sql")
                  .build();

  private final JdbcClient jdbcClient = JdbcClient.create(this.embeddedDatabase);

  @BeforeEach
  void checkDatabase() {
    assertNumUsers(1);
  }

  @AfterEach
  void shutdownDatabase() {
    this.embeddedDatabase.shutdown();
  }

  @Test
  void updateWithGeneratedKeys() {
    int expectedId = 2;
    String firstName = "Jane";
    String lastName = "Smith";

    KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

    int rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
            .params(firstName, lastName)
            .update(generatedKeyHolder);

    assertThat(rowsAffected).isEqualTo(1);
    assertThat(generatedKeyHolder.getKey()).isEqualTo(expectedId);
    assertNumUsers(2);
    assertUser(expectedId, firstName, lastName);
  }

  @Test
  void updateWithGeneratedKeysAndKeyColumnNames() {
    int expectedId = 2;
    String firstName = "Jane";
    String lastName = "Smith";

    KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

    int rowsAffected = this.jdbcClient.sql(INSERT_WITH_JDBC_PARAMS)
            .params(firstName, lastName)
            .update(generatedKeyHolder, "id");

    assertThat(rowsAffected).isEqualTo(1);
    assertThat(generatedKeyHolder.getKey()).isEqualTo(expectedId);
    assertNumUsers(2);
    assertUser(expectedId, firstName, lastName);
  }

  @Test
  void updateWithGeneratedKeysUsingNamedParameters() {
    int expectedId = 2;
    String firstName = "Jane";
    String lastName = "Smith";

    KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

    int rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
            .param("firstName", firstName)
            .param("lastName", lastName)
            .update(generatedKeyHolder);

    assertThat(rowsAffected).isEqualTo(1);
    assertThat(generatedKeyHolder.getKey()).isEqualTo(expectedId);
    assertNumUsers(2);
    assertUser(expectedId, firstName, lastName);
  }

  @Test
  void updateWithGeneratedKeysAndKeyColumnNamesUsingNamedParameters() {
    int expectedId = 2;
    String firstName = "Jane";
    String lastName = "Smith";

    KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

    int rowsAffected = this.jdbcClient.sql(INSERT_WITH_NAMED_PARAMS)
            .param("firstName", firstName)
            .param("lastName", lastName)
            .update(generatedKeyHolder, "id");

    assertThat(rowsAffected).isEqualTo(1);
    assertThat(generatedKeyHolder.getKey()).isEqualTo(expectedId);
    assertNumUsers(2);
    assertUser(expectedId, firstName, lastName);
  }

  private void assertNumUsers(long count) {
    long numUsers = this.jdbcClient.sql("select count(id) from users").query(Long.class).single();
    assertThat(numUsers).isEqualTo(count);
  }

  private void assertUser(long id, String firstName, String lastName) {
    User user = this.jdbcClient.sql("select * from users where id = ?").param(id).query(User.class).single();
    assertThat(user).isEqualTo(new User(id, firstName, lastName));
  }

  record User(long id, String firstName, String lastName) { }

}
