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

package infra.persistence;

import infra.persistence.platform.Platform;

/**
 * Represents a sequence of SQL statements that can be converted into a single SQL string.
 * This interface is typically used to build complex SQL statements such as INSERT, SELECT, DELETE, etc.,
 * by combining multiple clauses and conditions into a cohesive structure.
 *
 * <p>Implementations of this interface provide specific logic for constructing SQL statements
 * based on the requirements of the underlying database platform. The {@link #toStatementString(Platform)}
 * method is responsible for generating the final SQL string.
 *
 * <p><b>Usage Example:</b>
 * <p>The following example demonstrates how to use the {@code StatementSequence} interface to construct
 * an SQL INSERT statement using the {@code InsertSelect} class:
 *
 * <pre>{@code
 * Platform platform = new MySQLPlatform();
 * Select select = new Select()
 *     .setSelectClause("id, name")
 *     .setFromClause("users")
 *     .setWhereClause("active = 1");
 *
 * StatementSequence insertSelect = new InsertSelect()
 *     .setTableName("active_users")
 *     .addColumns("id", "name")
 *     .setSelect(select);
 *
 * String sql = insertSelect.toStatementString(platform);
 * System.out.println(sql);
 * }</pre>
 *
 * <p>The output might look like this:
 * <pre>{@code
 * INSERT INTO active_users (id, name) SELECT id, name FROM users WHERE active = 1
 * }</pre>
 *
 * <p><b>Another Example:</b>
 * <p>Here is an example of constructing a DELETE statement using the {@code Delete} class:
 *
 * <pre>{@code
 * Platform platform = new PostgreSQLPlatform();
 * StatementSequence delete = new Delete("inactive_users")
 *     .addColumnRestriction("status")
 *     .setComment("Remove inactive users");
 *
 * String sql = delete.toStatementString(platform);
 * System.out.println(sql);
 * }</pre>
 *
 * <p>The output might look like this:
 * <pre>{@code
 * /* Remove inactive users * / DELETE FROM inactive_users WHERE status = ?
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Platform
 * @since 4.0 2024/2/21 22:45
 */
public interface StatementSequence {

  /**
   * Converts this sequence of SQL statements into a single, cohesive SQL string
   * that is compatible with the given database platform.
   *
   * <p>This method takes into account the specific SQL syntax and conventions
   * of the provided {@link Platform}, ensuring that the generated SQL string
   * adheres to the requirements of the target database. For example, it may
   * handle differences in identifier quoting, function usage, or clause ordering.
   *
   * <p><b>Usage Example:</b>
   * <p>The following example demonstrates how to use this method to generate
   * an SQL INSERT statement for a MySQL database:
   *
   * <pre>{@code
   * Platform platform = new MySQLPlatform();
   * StatementSequence insert = new Insert("users")
   *     .addColumns("id", "name")
   *     .setValues(1, "Alice");
   *
   * String sql = insert.toStatementString(platform);
   * System.out.println(sql);
   * }</pre>
   *
   * <p>The output might look like this:
   * <pre>{@code
   * INSERT INTO `users` (`id`, `name`) VALUES (1, 'Alice')
   * }</pre>
   *
   * <p><b>Another Example:</b>
   * <p>Here is an example of generating a SELECT statement for a PostgreSQL database:
   *
   * <pre>{@code
   * Platform platform = new PostgreSQLPlatform();
   * StatementSequence select = new Select()
   *     .setSelectClause("id, name")
   *     .setFromClause("users")
   *     .setWhereClause("active = true");
   *
   * String sql = select.toStatementString(platform);
   * System.out.println(sql);
   * }</pre>
   *
   * <p>The output might look like this:
   * <pre>{@code
   * SELECT id, name FROM users WHERE active = true
   * }</pre>
   *
   * @param platform the database platform for which the SQL string is being generated.
   * This parameter determines the SQL dialect and formatting rules
   * applied during the conversion process.
   * @return a single SQL string that represents the complete sequence of statements,
   * formatted according to the specified database platform's conventions.
   */
  String toStatementString(Platform platform);

}
