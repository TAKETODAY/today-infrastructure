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

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import infra.core.io.ClassPathResource;
import infra.core.io.EncodedResource;

import static infra.jdbc.datasource.init.ScriptUtils.splitSqlScript;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ScriptUtils}.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Chris Baldwin
 * @author Nicolas Debeissat
 * @see ScriptUtilsIntegrationTests
 * @since 4.0
 */
public class ScriptUtilsUnitTests {

  @Test
  @SuppressWarnings("deprecation")
  public void splitSqlScriptDelimitedWithSemicolon() {
    String rawStatement1 = "insert into customer (id, name)\nvalues (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
    String cleanedStatement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
    String rawStatement2 = "insert into orders(id, order_date, customer_id)\nvalues (1, '2008-01-02', 2)";
    String cleanedStatement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
    String rawStatement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
    String cleanedStatement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";

    String delimiter = ";";
    String script = Strings.join(rawStatement1, rawStatement2, rawStatement3).with(delimiter);

    List<String> statements = new ArrayList<>();
    ScriptUtils.splitSqlScript(script, delimiter, statements);

    assertThat(statements).containsExactly(cleanedStatement1, cleanedStatement2, cleanedStatement3);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void splitSqlScriptDelimitedWithNewLine() {
    String statement1 = "insert into customer (id, name) values (1, 'Rod ; Johnson'), (2, 'Adrian \n Collier')";
    String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
    String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";

    String delimiter = "\n";
    String script = Strings.join(statement1, statement2, statement3).with(delimiter);

    List<String> statements = new ArrayList<>();
    ScriptUtils.splitSqlScript(script, delimiter, statements);

    assertThat(statements).containsExactly(statement1, statement2, statement3);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void splitSqlScriptDelimitedWithNewLineButDefaultDelimiterSpecified() {
    String statement1 = "do something";
    String statement2 = "do something else";

    String script = Strings.join(statement1, statement2).with("\n");

    List<String> statements = new ArrayList<>();

    ScriptUtils.splitSqlScript(script, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR, statements);

    assertThat(statements).as("stripped but not split statements").containsExactly(script.replace('\n', ' '));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void splitScriptWithSingleQuotesNestedInsideDoubleQuotes() {
    String statement1 = "select '1' as \"Dogbert's owner's\" from dual";
    String statement2 = "select '2' as \"Dilbert's\" from dual";

    String delimiter = ";";
    String script = Strings.join(statement1, statement2).with(delimiter);

    List<String> statements = new ArrayList<>();
    ScriptUtils.splitSqlScript(script, delimiter, statements);

    assertThat(statements).containsExactly(statement1, statement2);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void readAndSplitScriptWithMultipleNewlinesAsSeparator() throws Exception {
    String script = readScript("db-test-data-multi-newline.sql");
    List<String> statements = new ArrayList<>();
    ScriptUtils.splitSqlScript(script, "\n\n", statements);

    String statement1 = "insert into T_TEST (NAME) values ('Keith')";
    String statement2 = "insert into T_TEST (NAME) values ('Dave')";

    assertThat(statements).containsExactly(statement1, statement2);
  }

  @Test
  public void readAndSplitScriptContainingComments() throws Exception {
    String script = readScript("test-data-with-comments.sql");
    splitScriptContainingComments(script, ScriptUtils.DEFAULT_COMMENT_PREFIXES);
  }

  @Test
  public void readAndSplitScriptContainingCommentsWithWindowsLineEnding() throws Exception {
    String script = readScript("test-data-with-comments.sql").replaceAll("\n", "\r\n");
    splitScriptContainingComments(script, ScriptUtils.DEFAULT_COMMENT_PREFIXES);
  }

  @Test
  public void readAndSplitScriptContainingCommentsWithMultiplePrefixes() throws Exception {
    String script = readScript("test-data-with-multi-prefix-comments.sql");
    splitScriptContainingComments(script, "--", "#", "^");
  }

  @SuppressWarnings("deprecation")
  private void splitScriptContainingComments(String script, String... commentPrefixes) {
    List<String> statements = new ArrayList<>();
    ScriptUtils.splitSqlScript(null, script, ";", commentPrefixes, ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER, statements);

    String statement1 = "insert into customer (id, name) values (1, 'Rod; Johnson'), (2, 'Adrian Collier')";
    String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
    String statement3 = "insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2)";
    // Statement 4 addresses the error described in SPR-9982.
    String statement4 = "INSERT INTO persons( person_id , name) VALUES( 1 , 'Name' )";

    assertThat(statements).containsExactly(statement1, statement2, statement3, statement4);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void readAndSplitScriptContainingCommentsWithLeadingTabs() throws Exception {
    String script = readScript("test-data-with-comments-and-leading-tabs.sql");
    List<String> statements = new ArrayList<>();
    ScriptUtils.splitSqlScript(script, ';', statements);

    String statement1 = "insert into customer (id, name) values (1, 'Sam Brannen')";
    String statement2 = "insert into orders(id, order_date, customer_id) values (1, '2013-06-08', 1)";
    String statement3 = "insert into orders(id, order_date, customer_id) values (2, '2013-06-08', 1)";

    assertThat(statements).containsExactly(statement1, statement2, statement3);
  }

  @Test  //
  @SuppressWarnings("deprecation")
  public void readAndSplitScriptContainingMultiLineComments() throws Exception {
    String script = readScript("test-data-with-multi-line-comments.sql");
    List<String> statements = new ArrayList<>();
    ScriptUtils.splitSqlScript(script, ';', statements);

    String statement1 = "INSERT INTO users(first_name, last_name) VALUES('Juergen', 'Hoeller')";
    String statement2 = "INSERT INTO users(first_name, last_name) VALUES( 'Sam' , 'Brannen' )";

    assertThat(statements).containsExactly(statement1, statement2);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void readAndSplitScriptContainingMultiLineNestedComments() throws Exception {
    String script = readScript("test-data-with-multi-line-nested-comments.sql");
    List<String> statements = new ArrayList<>();
    ScriptUtils.splitSqlScript(script, ';', statements);

    String statement1 = "INSERT INTO users(first_name, last_name) VALUES('Juergen', 'Hoeller')";
    String statement2 = "INSERT INTO users(first_name, last_name) VALUES( 'Sam' , 'Brannen' )";

    assertThat(statements).containsExactly(statement1, statement2);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', quoteCharacter = '~', textBlock = """
          # semicolon
          ~select 1\n select ';'~                                                            | ;      | false
          ~select 1\n select ";"~                                                            | ;      | false
          ~select 1; select 2~                                                               | ;      | true

          # newline
          ~select 1; select '\n'~                                                            | ~\n~   | false
          ~select 1; select "\n"~                                                            | ~\n~   | false
          ~select 1\n select 2~                                                              | ~\n~   | true

          # double newline
          ~select 1\n select 2~                                                              | ~\n\n~ | false
          ~select 1\n\n select 2~                                                            | ~\n\n~ | true

          # semicolon with MySQL style escapes '\\'
          ~insert into users(first, last)\n values('a\\\\', 'b;')~                           | ;      | false
          ~insert into users(first, last)\n values('Charles', 'd\\'Artagnan'); select 1~     | ;      | true

          # semicolon inside comments
          ~-- a;b;c\n insert into colors(color_num) values(42);~                             | ;      | true
          ~/* a;b;c */\n insert into colors(color_num) values(42);~                          | ;      | true
          ~-- a;b;c\n insert into colors(color_num) values(42)~                              | ;      | false
          ~/* a;b;c */\n insert into colors(color_num) values(42)~                           | ;      | false

          # single quotes inside comments
          ~-- What\\'s your favorite color?\n insert into colors(color_num) values(42);~     | ;      | true
          ~-- What's your favorite color?\n insert into colors(color_num) values(42);~       | ;      | true
          ~/* What\\'s your favorite color? */\n insert into colors(color_num) values(42);~  | ;      | true
          ~/* What's your favorite color? */\n insert into colors(color_num) values(42);~    | ;      | true

          # double quotes inside comments
          ~-- double " quotes\n insert into colors(color_num) values(42);~                   | ;      | true
          ~-- double \\" quotes\n insert into colors(color_num) values(42);~                 | ;      | true
          ~/* double " quotes */\n insert into colors(color_num) values(42);~                | ;      | true
          ~/* double \\" quotes */\n insert into colors(color_num) values(42);~              | ;      | true
          """)
  @SuppressWarnings("deprecation")
  public void containsStatementSeparator(String script, String delimiter, boolean expected) {
    // Indirectly tests ScriptUtils.containsStatementSeparator(EncodedResource, String, String, String[], String, String).
    Assertions.assertThat(ScriptUtils.containsSqlScriptDelimiters(script, delimiter)).isEqualTo(expected);
  }

  private String readScript(String path) throws Exception {
    EncodedResource resource = new EncodedResource(new ClassPathResource(path, getClass()));
    return ScriptUtils.readScript(resource, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR, ScriptUtils.DEFAULT_COMMENT_PREFIXES,
            ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
  }

}
