/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import infra.core.io.ByteArrayResource;
import infra.core.io.EncodedResource;
import infra.core.io.Resource;
import infra.jdbc.core.DataClassRowMapper;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static infra.jdbc.datasource.init.ScriptUtils.executeSqlScript;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Integration tests for {@link ScriptUtils}.
 *
 * @author Sam Brannen
 * @see ScriptUtilsUnitTests
 * @since 4.0
 */
@ParameterizedClass
@EnumSource(EmbeddedDatabaseType.class)
class ScriptUtilsIntegrationTests extends AbstractDatabaseInitializationTests {

  @Parameter
  EmbeddedDatabaseType databaseType;

  @Override
  protected EmbeddedDatabaseType getEmbeddedDatabaseType() {
    return this.databaseType;
  }

  @BeforeEach
  void setUpSchema() throws SQLException {
    executeSqlScript(db.getConnection(), encodedResource(usersSchema()), false, true, "--", null, "/*", "*/");
  }

  @Test
  void executeSqlScriptContainingMultiLineComments() throws SQLException {
    executeSqlScript(db.getConnection(), resource("test-data-with-multi-line-comments.sql"));
    assertUsersDatabaseCreated("Hoeller", "Brannen");
  }

  /**
   * @since 4.2
   */
  @Test
  void executeSqlScriptContainingSingleQuotesNestedInsideDoubleQuotes() throws SQLException {
    executeSqlScript(db.getConnection(), resource("users-data-with-single-quotes-nested-in-double-quotes.sql"));
    assertUsersDatabaseCreated("Hoeller", "Brannen");
  }

  @Test
  @SuppressWarnings("unchecked")
  void statementWithMultipleResultSets() throws SQLException {
    // Derby does not support multiple statements/ResultSets within a single Statement.
    assumeThat(this.databaseType).isNotSameAs(EmbeddedDatabaseType.DERBY);

    EncodedResource resource = encodedResource(resource("users-data.sql"));
    executeSqlScript(db.getConnection(), resource, false, true, "--", null, "/*", "*/");

    assertUsersInDatabase(user("Sam", "Brannen"));

    resource = encodedResource(inlineResource("""
            SELECT last_name FROM users WHERE id = 0;
            UPDATE users SET first_name = 'Jane' WHERE id = 0;
            UPDATE users SET last_name = 'Smith' WHERE id = 0;
            SELECT last_name FROM users WHERE id = 0;
            GO
            """));

    String separator = "GO\n";
    executeSqlScript(db.getConnection(), resource, false, true, "--", separator, "/*", "*/");

    assertUsersInDatabase(user("Jane", "Smith"));
  }

  private void assertUsersInDatabase(User... expectedUsers) {
    List<User> users = jdbcTemplate.query("SELECT * FROM users WHERE id = 0",
            new DataClassRowMapper<>(User.class));
    assertThat(users).containsExactly(expectedUsers);
  }

  private static EncodedResource encodedResource(Resource resource) {
    return new EncodedResource(resource);
  }

  private static Resource inlineResource(String sql) {
    byte[] bytes = sql.getBytes(StandardCharsets.UTF_8);
    return new ByteArrayResource(bytes, "inline SQL");
  }

  private static User user(String firstName, String lastName) {
    return new User(0, firstName, lastName);
  }

  record User(int id, String firstName, String lastName) {
  }

}
