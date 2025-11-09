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

package infra.jdbc.format;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 22:05
 */
class BasicSQLFormatterTests {

  @Test
  void shouldFormatSimpleSelectQuery() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT id, name FROM users WHERE age > 18";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("WHERE");
  }

  @Test
  void shouldFormatSelectWithOrderBy() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT * FROM users ORDER BY name ASC";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("ORDER BY");
  }

  @Test
  void shouldFormatUpdateStatement() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "UPDATE users SET name = 'Jane' WHERE id = 1";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("UPDATE");
    assertThat(formatted).contains("SET");
    assertThat(formatted).contains("WHERE");
  }

  @Test
  void shouldFormatDeleteStatement() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "DELETE FROM users WHERE id = 1";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("DELETE ");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("WHERE");
  }

  @Test
  void shouldSkipDDLWhenSkipDDLIsTrue() {
    BasicSQLFormatter formatter = new BasicSQLFormatter(true);
    String sql = "CREATE TABLE users (id INT, name VARCHAR(255))";
    String formatted = formatter.format(sql);

    assertThat(formatted).startsWith(System.lineSeparator());
    assertThat(formatted).contains("CREATE TABLE");
  }

  @Test
  void shouldNotSkipDDLWhenSkipDDLIsFalse() {
    BasicSQLFormatter formatter = new BasicSQLFormatter(false);
    String sql = "CREATE TABLE users (id INT, name VARCHAR(255))";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("CREATE TABLE");
    assertThat(formatted).contains(System.lineSeparator());
  }

  @Test
  void shouldFormatComplexQueryWithJoins() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT u.name, p.title FROM users u INNER JOIN posts p ON u.id = p.user_id WHERE u.active = true";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("INNER JOIN");
    assertThat(formatted).contains("ON");
    assertThat(formatted).contains("WHERE");
  }

  @Test
  void shouldFormatQueryWithSubquery() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("WHERE");
    assertThat(formatted).contains("IN");
  }

  @Test
  void shouldFormatQueryWithCaseStatement() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT id, CASE WHEN age < 18 THEN 'minor' ELSE 'adult' END as status FROM users";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("CASE");
    assertThat(formatted).contains("WHEN");
    assertThat(formatted).contains("THEN");
    assertThat(formatted).contains("ELSE");
    assertThat(formatted).contains("END");
  }

  @Test
  void shouldFormatQueryWithBetween() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT * FROM users WHERE age BETWEEN 18 AND 65";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("WHERE");
    assertThat(formatted).contains("BETWEEN");
    assertThat(formatted).contains("AND");
  }

  @Test
  void shouldHandleSingleQuotesInQuery() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT * FROM users WHERE name = 'John Doe'";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("WHERE");
    assertThat(formatted).contains("'John Doe'");
  }

  @Test
  void shouldHandleDoubleQuotesInQuery() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT * FROM \"users\" WHERE \"name\" = 'John'";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("\"users\"");
    assertThat(formatted).contains("\"name\"");
  }

  @Test
  void shouldFormatGroupByQuery() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT department, COUNT(*) FROM users GROUP BY department";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("GROUP BY");
  }

  @Test
  void shouldFormatUnionQuery() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT name FROM users UNION SELECT name FROM admins";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("UNION");
  }

  @Test
  void shouldFormatInsertStatement() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "INSERT INTO users (id, name) VALUES (1, 'John')";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("INSERT ");
    assertThat(formatted).contains("INTO");
    assertThat(formatted).contains("VALUES");
  }

  @Test
  void shouldHandleEmptySQL() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "";
    String formatted = formatter.format(sql);

    assertThat(formatted).isNotNull();
  }

  @Test
  void shouldHandleOnlyWhitespaceSQL() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "   \n\t  ";
    String formatted = formatter.format(sql);

    assertThat(formatted).isNotNull();
  }

  @Test
  void shouldFormatAlterTableStatementWithSkipDDLOff() {
    BasicSQLFormatter formatter = new BasicSQLFormatter(false);
    String sql = "ALTER TABLE users ADD COLUMN age INT";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("ALTER TABLE");
    assertThat(formatted).contains(System.lineSeparator());
  }

  @Test
  void shouldSkipAlterTableStatementWithSkipDDL() {
    BasicSQLFormatter formatter = new BasicSQLFormatter(true);
    String sql = "ALTER TABLE users ADD COLUMN age INT";
    String formatted = formatter.format(sql);

    assertThat(formatted).startsWith(System.lineSeparator());
    assertThat(formatted).contains("ALTER TABLE");
  }

  @Test
  void shouldFormatCommentOnStatementWithSkipDDLOff() {
    BasicSQLFormatter formatter = new BasicSQLFormatter(false);
    String sql = "COMMENT ON TABLE users IS 'User table'";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("COMMENT ");
    assertThat(formatted).contains("ON TABLE users IS 'User table'");
    assertThat(formatted).contains(System.lineSeparator());
  }

  @Test
  void shouldSkipCommentOnStatementWithSkipDDL() {
    BasicSQLFormatter formatter = new BasicSQLFormatter(true);
    String sql = "COMMENT ON TABLE users IS 'User table'";
    String formatted = formatter.format(sql);

    assertThat(formatted).startsWith(System.lineSeparator());
    assertThat(formatted).contains("COMMENT ON");
  }

  @Test
  void shouldFormatSelectWithMultipleJoins() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT u.name, p.title, c.name FROM users u LEFT JOIN posts p ON u.id = p.user_id RIGHT JOIN comments c ON p.id = c.post_id";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("LEFT JOIN");
    assertThat(formatted).contains("RIGHT JOIN");
    assertThat(formatted).contains("ON");
  }

  @Test
  void shouldFormatSelectWithHavingClause() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT department, COUNT(*) as cnt FROM users GROUP BY department HAVING COUNT(*) > 5";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("GROUP BY");
    assertThat(formatted).contains("HAVING");
  }

  @Test
  void shouldFormatSelectWithInnerSelectAndJoins() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT u.name FROM users u JOIN (SELECT user_id FROM orders WHERE amount > 100) o ON u.id = o.user_id";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("JOIN");
    assertThat(formatted).contains("ON");
    assertThat(formatted).contains("WHERE");
  }

  @Test
  void shouldFormatUpdateWithMultipleSet() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "UPDATE users SET name = 'Jane', age = 25, active = true WHERE id = 1";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("UPDATE");
    assertThat(formatted).contains("SET");
    assertThat(formatted).contains("WHERE");
  }

  @Test
  void shouldFormatDeleteWithJoin() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "DELETE u FROM users u JOIN orders o ON u.id = o.user_id WHERE o.amount < 0";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("DELETE");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("JOIN");
    assertThat(formatted).contains("ON");
    assertThat(formatted).contains("WHERE");
  }

  @Test
  void shouldHandleSQLWithSquareBrackets() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT [name] FROM [users] WHERE [age] > 18";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("[name]");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("[users]");
    assertThat(formatted).contains("WHERE");
  }

  @Test
  void shouldFormatQueryWithExists() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT * FROM users WHERE EXISTS (SELECT 1 FROM orders WHERE user_id = users.id)";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("WHERE");
    assertThat(formatted).contains("EXISTS");
  }

  @Test
  void shouldFormatQueryWithAllQuantifier() {
    BasicSQLFormatter formatter = new BasicSQLFormatter();
    String sql = "SELECT * FROM products WHERE price > ALL (SELECT price FROM competitors)";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("WHERE");
    assertThat(formatted).contains("ALL");
  }

}