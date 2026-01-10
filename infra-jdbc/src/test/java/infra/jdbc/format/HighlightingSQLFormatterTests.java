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
class HighlightingSQLFormatterTests {

  @Test
  void shouldFormatSimpleSelectQueryWithHighlighting() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "SELECT id, name FROM users WHERE age > 18";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("SELECT");
    assertThat(formatted).contains("FROM");
    assertThat(formatted).contains("WHERE");
    assertThat(formatted).contains("\u001b[34m"); // keyword escape code
    assertThat(formatted).contains("\u001b[0m"); // normal escape code
  }

  @Test
  void shouldHighlightStringLiterals() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "SELECT * FROM users WHERE name = 'John Doe'";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("\u001b[36m'"); // string escape code
    assertThat(formatted).contains("John Doe");
  }

  @Test
  void shouldHighlightQuotedIdentifiers() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "SELECT \"name\" FROM \"users\" WHERE \"age\" > 18";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("\u001b[32m\""); // quoted escape code
    assertThat(formatted).contains("name");
    assertThat(formatted).contains("users");
    assertThat(formatted).contains("age");
  }

  @Test
  void shouldHandleEmptySQL() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "";
    String formatted = formatter.format(sql);

    assertThat(formatted).isEmpty();
  }

  @Test
  void shouldHandleSQLWithOnlyWhitespace() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "   \n\t  ";
    String formatted = formatter.format(sql);

    assertThat(formatted).isEqualTo(sql);
  }

  @Test
  void shouldHandleBacktickQuotes() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "SELECT `name` FROM `users` WHERE `age` > 18";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("\u001b[32m`"); // quoted escape code
    assertThat(formatted).contains("name");
    assertThat(formatted).contains("users");
    assertThat(formatted).contains("age");
  }

  @Test
  void shouldFormatInsertStatement() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "INSERT INTO users (id, name) VALUES (1, 'John')";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("\u001b[34mINSERT\u001b[0m");
    assertThat(formatted).contains("\u001b[34mINTO\u001b[0m");
    assertThat(formatted).contains("\u001b[34mVALUES\u001b[0m");
    assertThat(formatted).contains("\u001b[36m'");
  }

  @Test
  void shouldFormatUpdateStatement() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "UPDATE users SET name = 'Jane' WHERE id = 1";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("\u001b[34mUPDATE\u001b[0m");
    assertThat(formatted).contains("\u001b[34mSET\u001b[0m");
    assertThat(formatted).contains("\u001b[34mWHERE\u001b[0m");
  }

  @Test
  void shouldFormatDeleteStatement() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "DELETE FROM users WHERE id = 1";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("\u001b[34mDELETE\u001b[0m");
    assertThat(formatted).contains("\u001b[34mFROM\u001b[0m");
    assertThat(formatted).contains("\u001b[34mWHERE\u001b[0m");
  }

  @Test
  void shouldUseDefaultInstance() {
    String sql = "SELECT * FROM users";
    String formatted = HighlightingSQLFormatter.INSTANCE.format(sql);

    assertThat(formatted).contains("\u001b[34mSELECT\u001b[0m");
    assertThat(formatted).contains("\u001b[34mFROM\u001b[0m");
  }

  @Test
  void shouldNotHighlightNonKeywords() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "SELECT user_name, user_id FROM users";
    String formatted = formatter.format(sql);

    // user_name and user_id should not be highlighted as keywords
    assertThat(formatted).contains("user_name");
    assertThat(formatted).contains("user_id");
    // Check that they are not wrapped with escape codes
    assertThat(formatted).doesNotContain("\u001b[34muser_name\u001b[0m");
    assertThat(formatted).doesNotContain("\u001b[34muser_id\u001b[0m");
  }

  @Test
  void shouldHandleMixedCaseKeywords() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "select ID from USERS where NAME = 'test'";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("\u001b[34mselect\u001b[0m");
    assertThat(formatted).contains("\u001b[34mfrom\u001b[0m");
    assertThat(formatted).contains("\u001b[34mwhere\u001b[0m");
  }

  @Test
  void shouldHandleSpecialCharactersInStrings() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "SELECT * FROM users WHERE name = 'John*Doe'";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("\u001b[36m'");
    assertThat(formatted).contains("John*Doe");
  }

  @Test
  void shouldHandleAllQuoteTypesInSameQuery() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "SELECT \"column\", `table`, 'value' FROM data";
    String formatted = formatter.format(sql);

    assertThat(formatted).contains("\u001b[32m\"");
    assertThat(formatted).contains("\u001b[32m`");
    assertThat(formatted).contains("\u001b[36m'");
  }

  @Test
  void shouldHandleKeywordsInQuotedIdentifiers() {
    HighlightingSQLFormatter formatter = new HighlightingSQLFormatter("34", "36", "32");
    String sql = "SELECT \"SELECT\", 'FROM' FROM \"TABLE\"";
    String formatted = formatter.format(sql);

    // SELECT keyword should be highlighted
    assertThat(formatted).contains("\u001b[34mSELECT\u001b[0m");
    // SELECT, FROM, TABLE inside quotes should NOT be highlighted
    assertThat(formatted).doesNotContain("\u001b[34m\"SELECT\"\u001b[0m");
    assertThat(formatted).doesNotContain("\u001b[34m'FROM'\u001b[0m");
    assertThat(formatted).doesNotContain("\u001b[34m\"TABLE\"\u001b[0m");
  }

}