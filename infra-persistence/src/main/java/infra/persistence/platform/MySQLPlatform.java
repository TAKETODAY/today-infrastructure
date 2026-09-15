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

package infra.persistence.platform;

/**
 * The {@link Platform} implementation for MySQL and MariaDB.
 *
 * <p>MySQL deviates from the SQL standard in the characters used to quote
 * identifiers: names are enclosed in backticks ({@code `}) rather than double
 * quotes. This platform therefore overrides {@link #openQuote()} and
 * {@link #closeQuote()}, which in turn drive {@link #toQuotedIdentifier(String)}
 * so that an {@link infra.persistence.Identifier} renders MySQL-compatible SQL.
 *
 * <p>It also overrides {@link #getNoColumnsInsertString()} to the MySQL form of
 * an insert statement that names no columns.
 *
 * <p>Instances are obtained from {@link Platform#mysql()} or resolved
 * automatically by {@link Platform#forDataSource(javax.sql.DataSource)}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Platform
 * @see GenericPlatform
 * @since 5.0
 */
public class MySQLPlatform extends Platform {

  /**
   * {@inheritDoc}
   *
   * <p>MySQL opens a quoted identifier with a backtick.
   */
  @Override
  public char openQuote() {
    return '`';
  }

  /**
   * {@inheritDoc}
   *
   * <p>MySQL closes a quoted identifier with a backtick.
   */
  @Override
  public char closeQuote() {
    return '`';
  }

  /**
   * {@inheritDoc}
   *
   * <p>MySQL accepts an insert that mentions no columns only as
   * {@code INSERT INTO table () VALUES ()}.
   */
  @Override
  public String getNoColumnsInsertString() {
    return "() VALUES ()";
  }

}
