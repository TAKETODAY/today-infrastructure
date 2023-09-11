/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.core.simple;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.core.io.ClassRelativeResourceLoader;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabase;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;
import cn.taketoday.jdbc.datasource.init.DatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SimpleJdbcInsert} using an embedded H2 database.
 *
 * @author Sam Brannen
 */
class SimpleJdbcInsertIntegrationTests {

  @Nested
  class DefaultSchemaTests extends AbstractSimpleJdbcInsertIntegrationTests {

    @Test
    void retrieveColumnNamesFromMetadata() throws Exception {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
              .withTableName("users")
              .usingGeneratedKeyColumns("id");

      insert.compile();
      // NOTE: column names looked up via metadata in H2/HSQL will be UPPERCASE!
      assertThat(insert.getInsertString()).isEqualTo("INSERT INTO users (FIRST_NAME, LAST_NAME) VALUES(?, ?)");

      insertJaneSmith(insert);
    }

    @Test
      //  gh-24013
    void retrieveColumnNamesFromMetadataAndUsingQuotedIdentifiers() throws Exception {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
              .withTableName("users")
              .usingGeneratedKeyColumns("id")
              .usingQuotedIdentifiers();

      insert.compile();
      // NOTE: quoted identifiers in H2/HSQL will be UPPERCASE!
      assertThat(insert.getInsertString()).isEqualTo("INSERT INTO \"USERS\" (\"FIRST_NAME\", \"LAST_NAME\") VALUES(?, ?)");

      insertJaneSmith(insert);
    }

    @Test
    void usingColumns() {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
              .withTableName("users")
              .usingColumns("first_name", "last_name");

      insert.compile();
      assertThat(insert.getInsertString()).isEqualTo("INSERT INTO users (first_name, last_name) VALUES(?, ?)");

      insertJaneSmith(insert);
    }

    @Test
      //  gh-24013
    void usingColumnsAndQuotedIdentifiers() throws Exception {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
              .withTableName("users")
              .usingColumns("first_name", "last_name")
              .usingQuotedIdentifiers();

      insert.compile();
      // NOTE: quoted identifiers in H2/HSQL will be UPPERCASE!
      assertThat(insert.getInsertString()).isEqualTo("INSERT INTO \"USERS\" (\"FIRST_NAME\", \"LAST_NAME\") VALUES(?, ?)");

      insertJaneSmith(insert);
    }

    @Override
    protected String getSchemaScript() {
      return "users-schema.sql";
    }

    @Override
    protected String getUsersTableName() {
      return "users";
    }

  }

  @Nested
  class CustomSchemaTests extends AbstractSimpleJdbcInsertIntegrationTests {

    @Test
    void usingColumnsWithSchemaName() {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
              .withSchemaName("my_schema")
              .withTableName("users")
              .usingColumns("first_name", "last_name");

      insert.compile();
      assertThat(insert.getInsertString()).isEqualTo("INSERT INTO my_schema.users (first_name, last_name) VALUES(?, ?)");

      insertJaneSmith(insert);
    }

    @Test
      //  gh-24013
    void usingColumnsAndQuotedIdentifiersWithSchemaName() throws Exception {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(embeddedDatabase)
              .withSchemaName("my_schema")
              .withTableName("users")
              .usingColumns("first_name", "last_name")
              .usingQuotedIdentifiers();

      insert.compile();
      // NOTE: quoted identifiers in H2/HSQL will be UPPERCASE!
      assertThat(insert.getInsertString()).isEqualTo("INSERT INTO \"MY_SCHEMA\".\"USERS\" (\"FIRST_NAME\", \"LAST_NAME\") VALUES(?, ?)");

      insertJaneSmith(insert);
    }

    @Override
    protected String getSchemaScript() {
      return "users-schema-with-custom-schema.sql";
    }

    @Override
    protected String getUsersTableName() {
      return "my_schema.users";
    }

  }

  private static abstract class AbstractSimpleJdbcInsertIntegrationTests {

    protected EmbeddedDatabase embeddedDatabase;

    @BeforeEach
    void createDatabase() {
      this.embeddedDatabase = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(DatabasePopulator.class))
              .setType(EmbeddedDatabaseType.H2)
              .addScript(getSchemaScript())
              .addScript("users-data.sql")
              .build();

      assertNumUsers(1);
    }

    @AfterEach
    void shutdownDatabase() {
      this.embeddedDatabase.shutdown();
    }

    protected void assertNumUsers(long count) {
      JdbcClient jdbcClient = JdbcClient.create(this.embeddedDatabase);
      long numUsers = jdbcClient.sql("select count(*) from " + getUsersTableName()).query(Long.class).single();
      assertThat(numUsers).isEqualTo(count);
    }

    protected void insertJaneSmith(SimpleJdbcInsert insert) {
      insert.execute(Map.of("first_name", "Jane", "last_name", "Smith"));
      assertNumUsers(2);
    }

    protected abstract String getSchemaScript();

    protected abstract String getUsersTableName();

  }

}
