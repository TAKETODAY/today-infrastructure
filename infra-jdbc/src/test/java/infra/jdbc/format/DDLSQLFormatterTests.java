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

package infra.jdbc.format;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 22:10
 */
class DDLSQLFormatterTests {

  @Test
  void shouldFormatCreateTableStatement() {
    String sql = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255), age INT)";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).contains("CREATE TABLE");
    assertThat(formatted).contains(System.lineSeparator());
    assertThat(formatted).contains("id INT PRIMARY KEY");
    assertThat(formatted).contains("name VARCHAR(255)");
  }

  @Test
  void shouldFormatAlterTableStatement() {
    String sql = "ALTER TABLE users ADD COLUMN email VARCHAR(255)";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).contains("ALTER TABLE");
    assertThat(formatted).contains(System.lineSeparator());
    assertThat(formatted).contains("ADD COLUMN");
  }

  @Test
  void shouldFormatCommentOnStatement() {
    String sql = "COMMENT ON TABLE users IS 'User information table'";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).contains("COMMENT ON");
    assertThat(formatted).contains(System.lineSeparator());
    assertThat(formatted).contains("IS 'User information table'");
  }

  @Test
  void shouldFormatCreateStatement() {
    String sql = "CREATE INDEX idx_user_name ON users(name)";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).isEqualTo(sql);
  }

  @Test
  void shouldHandleEmptySQL() {
    String sql = "";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).isEqualTo(sql);
  }

  @Test
  void shouldHandleNullSQL() {
    String sql = null;
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).isNull();
  }

  @Test
  void shouldFormatCreateTableWithMultipleColumns() {
    String sql = "CREATE TABLE products (id BIGINT PRIMARY KEY, name VARCHAR(100) NOT NULL, price DECIMAL(10,2), category_id INT, created_at TIMESTAMP)";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).contains("CREATE TABLE");
    assertThat(formatted).contains(System.lineSeparator());
    assertThat(formatted).contains("id BIGINT PRIMARY KEY");
    assertThat(formatted).contains("name VARCHAR(100) NOT NULL");
    assertThat(formatted).contains("price DECIMAL(10,2)");
  }

  @Test
  void shouldFormatAlterTableWithForeignKeys() {
    String sql = "ALTER TABLE orders ADD CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(id)";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).contains("ALTER TABLE");
    assertThat(formatted).contains(System.lineSeparator());
    assertThat(formatted).contains("ADD CONSTRAINT");
    assertThat(formatted).contains("FOREIGN KEY");
    assertThat(formatted).contains("REFERENCES");
  }

  @Test
  void shouldFormatCommentOnWithQuotes() {
    String sql = "COMMENT ON TABLE \"users\" IS 'Table for storing user information'";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).contains("COMMENT ON");
    assertThat(formatted).contains(System.lineSeparator());
    assertThat(formatted).contains("\"users\"");
    assertThat(formatted).contains("IS 'Table for storing user information'");
  }

  @Test
  void shouldFormatCreateTableWithQuotedIdentifiers() {
    String sql = "CREATE TABLE \"user profiles\" (\"user id\" INT, \"display name\" VARCHAR(50))";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).contains("CREATE TABLE");
    assertThat(formatted).contains(System.lineSeparator());
    assertThat(formatted).contains("\"user id\" INT");
    assertThat(formatted).contains("\"display name\" VARCHAR(50)");
  }

  @Test
  void shouldFormatUnknownStatementWithInitialLine() {
    String sql = "DROP TABLE users";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).startsWith(System.lineSeparator() + "    ");
    assertThat(formatted).contains("DROP TABLE users");
  }

  @Test
  void shouldFormatCreateTableWithNestedParentheses() {
    String sql = "CREATE TABLE test (id INT, data JSON CHECK (data IS JSON), PRIMARY KEY (id))";
    String formatted = DDLSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).contains("CREATE TABLE");
    assertThat(formatted).contains(System.lineSeparator());
    assertThat(formatted).contains("id INT");
    assertThat(formatted).contains("data JSON CHECK (data IS JSON)");
  }

}