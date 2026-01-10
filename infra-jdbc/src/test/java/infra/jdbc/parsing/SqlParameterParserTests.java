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

package infra.jdbc.parsing;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 15:05
 */
class SqlParameterParserTests {

  @Test
  void shouldParseSimpleNamedParameter() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
    QueryParameter param = paramMap.get("userId");
    assertThat(param.getName()).isEqualTo("userId");
  }

  @Test
  void shouldParseMultipleNamedParameters() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE id = :userId AND name = :userName";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE id = ? AND name = ?");
    assertThat(paramMap).hasSize(2);
    assertThat(paramMap).containsKeys("userId", "userName");
  }

  @Test
  void shouldHandleQuotedStringsWithColon() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE name = 'Hello :userId' AND id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE name = 'Hello :userId' AND id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldHandleDoubleQuotedStringsWithColon() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE name = \"Hello :userId\" AND id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE name = \"Hello :userId\" AND id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldIgnoreEscapedColon() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE email = 'user::name' AND id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE email = 'user::name' AND id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldParseParameterAtEnd() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldHandleSameParameterMultipleTimes() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE id = :userId OR manager_id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE id = ? OR manager_id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
    QueryParameter param = paramMap.get("userId");
    assertThat(param.getName()).isEqualTo("userId");
  }

  @Test
  void shouldParseParameterWithUnderscore() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE user_id = :user_id AND role_name = :role_name";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE user_id = ? AND role_name = ?");
    assertThat(paramMap).hasSize(2);
    assertThat(paramMap).containsKeys("user_id", "role_name");
  }

  @Test
  void shouldParseParameterWithNumber() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM table1 WHERE col1 = :param1 AND col2 = :param2value";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM table1 WHERE col1 = ? AND col2 = ?");
    assertThat(paramMap).hasSize(2);
    assertThat(paramMap).containsKeys("param1", "param2value");
  }

  @Test
  void shouldNotParseInvalidParameterStartingWithNumber() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE id = :123invalid";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE id = :123invalid");
    assertThat(paramMap).isEmpty();
  }

  @Test
  void shouldHandleEmptySql() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("");
    assertThat(paramMap).isEmpty();
  }

  @Test
  void shouldHandleSqlWithoutParameters() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users");
    assertThat(paramMap).isEmpty();
  }

  @Test
  void shouldParseComplexQuery() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT u.*, p.title FROM users u " +
            "JOIN posts p ON u.id = p.user_id " +
            "WHERE u.name = :userName " +
            "AND u.status = :status " +
            "AND p.created_at > :startDate " +
            "ORDER BY p.created_at DESC " +
            "LIMIT :limit OFFSET :offset";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo(
            "SELECT u.*, p.title FROM users u " +
                    "JOIN posts p ON u.id = p.user_id " +
                    "WHERE u.name = ? " +
                    "AND u.status = ? " +
                    "AND p.created_at > ? " +
                    "ORDER BY p.created_at DESC " +
                    "LIMIT ? OFFSET ?");
    assertThat(paramMap).hasSize(5);
    assertThat(paramMap).containsKeys("userName", "status", "startDate", "limit", "offset");
  }

  @Test
  void shouldHandleLineCommentWithParameters() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users -- Find user by :userId \n WHERE id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users -- Find user by :userId \n WHERE id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldHandleBlockCommentWithParameters() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users /* Find user by :userId */ WHERE id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users /* Find user by :userId */ WHERE id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldParseParameterAfterQuotedString() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE name = 'John Doe' AND id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE name = 'John Doe' AND id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldParseParameterBeforeQuotedString() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE id = :userId AND name = 'John Doe'";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE id = ? AND name = 'John Doe'");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldHandleMultipleQuotedStrings() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE name = 'John :param' AND email = \"test:param\" AND id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE name = 'John :param' AND email = \"test:param\" AND id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldParseSingleCharacterParameter() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE active = :x";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE active = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("x");
  }

  @Test
  void shouldParseParameterInFunction() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT COUNT(*) FROM users WHERE created_at > DATE_SUB(NOW(), INTERVAL :days DAY)";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT COUNT(*) FROM users WHERE created_at > DATE_SUB(NOW(), INTERVAL ? DAY)");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("days");
  }

  @Test
  void shouldHandleComplexQueryWithAllParserTypes() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT u.name, /* comment :param */ p.title -- line comment :param\n" +
            "FROM users u JOIN posts p ON u.id = p.user_id " +
            "WHERE u.name = 'User Name :param' AND u.status = :status " +
            "AND p.\"content\" = :content";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT u.name, /* comment :param */ p.title -- line comment :param\n" +
            "FROM users u JOIN posts p ON u.id = p.user_id " +
            "WHERE u.name = 'User Name :param' AND u.status = ? " +
            "AND p.\"content\" = ?");
    assertThat(paramMap).hasSize(2);
    assertThat(paramMap).containsKeys("status", "content");
  }

  @Test
  void shouldHandleParameterWithSpecialCharactersInName() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE first_name = :firstName AND last_name = :lastName";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE first_name = ? AND last_name = ?");
    assertThat(paramMap).hasSize(2);
    assertThat(paramMap).containsKeys("firstName", "lastName");
  }

  @Test
  void shouldNotParseParameterStartingWithDigit() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE id = :123abc";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE id = :123abc");
    assertThat(paramMap).isEmpty();
  }

  @Test
  void shouldHandleMultipleSameParameters() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE id = :userId OR manager_id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE id = ? OR manager_id = ?");
    assertThat(paramMap).hasSize(1);
    QueryParameter param = paramMap.get("userId");
    assertThat(param.getName()).isEqualTo("userId");
  }

  @Test
  void shouldParseParameterInOrderByClause() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users ORDER BY :sortColumn :sortDirection";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users ORDER BY ? ?");
    assertThat(paramMap).hasSize(2);
    assertThat(paramMap).containsKeys("sortColumn", "sortDirection");
  }

  @Test
  void shouldHandleNestedQuotes() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE name = 'John \"Doe\"' AND id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE name = 'John \"Doe\"' AND id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldHandleEscapedQuotes() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE name = 'John''s' AND id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE name = 'John''s' AND id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldParseParameterInSubquery() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE status = :status) AND name = :name";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE status = ?) AND name = ?");
    assertThat(paramMap).hasSize(2);
    assertThat(paramMap).containsKeys("status", "name");
  }

  @Test
  void shouldHandleWindowsLineEndingsInComments() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users -- comment :param\r\nWHERE id = :userId";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users -- comment :param\r\nWHERE id = ?");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("userId");
  }

  @Test
  void shouldParseParameterInCaseStatement() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT CASE WHEN age > :minAge THEN 'adult' ELSE 'minor' END FROM users";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT CASE WHEN age > ? THEN 'adult' ELSE 'minor' END FROM users");
    assertThat(paramMap).hasSize(1);
    assertThat(paramMap).containsKey("minAge");
  }

  @Test
  void shouldHandleMultipleLineComments() {
    SqlParameterParser parser = new SqlParameterParser();
    String sql = "SELECT * FROM users -- comment 1 :param1\n" +
            "WHERE id = :userId -- comment 2 :param2\n" +
            "AND status = :status";
    Map<String, QueryParameter> paramMap = new HashMap<>();
    String parsedSql = parser.parse(sql, paramMap);

    assertThat(parsedSql).isEqualTo("SELECT * FROM users -- comment 1 :param1\n" +
            "WHERE id = ? -- comment 2 :param2\n" +
            "AND status = ?");
    assertThat(paramMap).hasSize(2);
    assertThat(paramMap).containsKeys("userId", "status");
  }

}