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

import org.jspecify.annotations.Nullable;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import infra.jdbc.config.DatabaseDriver;
import infra.jdbc.support.JdbcUtils;
import infra.jdbc.support.MetaDataAccessException;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.persistence.sql.ANSIJoinFragment;
import infra.persistence.sql.JoinFragment;
import infra.persistence.sql.LogicalOperator;
import infra.util.StringUtils;

/**
 * A database platform — the collection of SQL syntax variations specific to one
 * database family.
 *
 * <p>A {@code Platform} is consulted while an SQL statement is rendered to text,
 * most importantly to {@linkplain #toQuotedIdentifier(String) quote identifiers}
 * with the characters the target database expects. Concrete platforms live in
 * this package, for example {@link GenericPlatform} for ANSI SQL and
 * {@link MySQLPlatform} for MySQL and MariaDB.
 *
 * <p>Platforms carry no mutable state, so a single instance may be shared. The
 * static factory methods return ready-to-use instances: use
 * {@link #forDataSource(DataSource)} to detect the platform from a JDBC data
 * source, or {@link #generic()} and {@link #mysql()} to obtain a known one. A
 * database without a dedicated implementation falls back to
 * {@link GenericPlatform}.
 *
 * <p>Custom platforms are created by subclassing this class and overriding the
 * methods whose defaults differ, for example:
 * <pre>{@code
 * public class CustomPlatform extends Platform {
 *   @Override
 *   public char openQuote() {
 *     return '[';
 *   }
 *
 *   @Override
 *   public char closeQuote() {
 *     return ']';
 *   }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see GenericPlatform
 * @see MySQLPlatform
 * @since 4.0 2021/10/10 13:11
 */
public abstract class Platform {

  private static final Logger log = LoggerFactory.getLogger(Platform.class);

  /**
   * Characters that may open a quoted SQL identifier: a backtick, a double quote
   * or an opening square bracket.
   */
  public static final String QUOTE = "`\"[";

  /**
   * Characters that may close a quoted SQL identifier: a backtick, a double quote
   * or a closing square bracket. Mirrors {@link #QUOTE}.
   */
  public static final String CLOSED_QUOTE = "`\"]";

  private static final Pattern ESCAPE_CLOSING_COMMENT_PATTERN = Pattern.compile("\\*/");
  private static final Pattern ESCAPE_OPENING_COMMENT_PATTERN = Pattern.compile("/\\*");

  /**
   * Return the character that opens a quoted identifier for this platform.
   *
   * <p>The default is the SQL-standard double quote ({@code "}). Databases that
   * deviate override this method together with {@link #closeQuote()} — for
   * example MySQL, which uses the backtick, or SQL Server, which uses brackets.
   *
   * @return the opening quote character
   */
  public char openQuote() {
    return '"';
  }

  /**
   * Return the character that closes a quoted identifier for this platform.
   *
   * <p>The default is the SQL-standard double quote ({@code "}) and should be
   * overridden together with {@link #openQuote()} whenever a database deviates.
   *
   * @return the closing quote character
   * @see #openQuote()
   */
  public char closeQuote() {
    return '"';
  }

  /**
   * Wrap the given identifier name in this platform's quote characters.
   *
   * <p>This method only adds the opening and closing delimiters. It does not
   * escape quote characters already present in the name; callers should provide
   * the bare identifier name.
   *
   * @param name the non-null identifier name to quote
   * @return the quoted identifier
   * @see #openQuote()
   * @see #closeQuote()
   */
  public String toQuotedIdentifier(String name) {
    return openQuote() + name + closeQuote();
  }

  /**
   * Resolve an identifier written in the internal backtick convention.
   *
   * <p>A name enclosed in a matched pair of backticks is taken to require
   * quoting: the markers are replaced by this platform's quote characters. Any
   * other value is returned unchanged. This lets statements be assembled in a
   * dialect-neutral way and defer the actual quoting to the platform.
   *
   * @param name the identifier to resolve, possibly {@code null}
   * @return the platform-quoted text, or the name unchanged when it is not
   * enclosed in a matched pair of backticks
   * @see #toQuotedIdentifier(String)
   */
  public @Nullable String quote(@Nullable String name) {
    if (name == null) {
      return null;
    }
    int length = name.length();
    if (length < 2 || name.charAt(0) != '`' || name.charAt(length - 1) != '`') {
      return name;
    }
    return toQuotedIdentifier(name.substring(1, length - 1));
  }

  /**
   * Escape the SQL comment delimiters embedded in the given text, so that the
   * text cannot prematurely close or reopen the surrounding block comment.
   *
   * @param comment the comment text to escape, possibly {@code null}
   * @return the escaped text, or the original value when it is empty or {@code null}
   */
  public static CharSequence escapeComment(CharSequence comment) {
    if (StringUtils.isNotEmpty(comment)) {
      final String escaped = ESCAPE_CLOSING_COMMENT_PATTERN.matcher(comment).replaceAll("*\\\\/");
      return ESCAPE_OPENING_COMMENT_PATTERN.matcher(escaped).replaceAll("/\\\\*");
    }
    return comment;
  }

  /**
   * Return the fragment appended to a query to lock the selected rows for update.
   *
   * <p>The default is {@code " for update"}, understood by most databases.
   *
   * @return the {@code FOR UPDATE} fragment
   */
  public String getForUpdateString() {
    return " for update";
  }

  /**
   * Return the fragment used by an insert statement that specifies no columns.
   *
   * <p>The default is {@code "VALUES ( )"}. Databases requiring a different form
   * — for example MySQL — override it.
   *
   * @return the empty values clause
   */
  public String getNoColumnsInsertString() {
    return "VALUES ( )";
  }

  /**
   * Create the {@link JoinFragment} strategy that renders joins for this platform.
   *
   * <p>The default returns an ANSI-style fragment.
   *
   * @return this platform's join fragment strategy, never {@code null}
   */
  public JoinFragment createOuterJoinFragment() {
    return new ANSIJoinFragment();
  }

  /**
   * Return the SQL token that renders the given logical operator on this
   * platform.
   *
   * <p>ANSI SQL supports {@link LogicalOperator#AND AND} and
   * {@link LogicalOperator#OR OR}. {@link LogicalOperator#XOR XOR} is not part
   * of the standard, so the default implementation rejects it by throwing;
   * platforms that support it natively override this method. The returned token
   * carries no surrounding whitespace.
   *
   * @param operator the logical operator to render, never {@code null}
   * @return the operator token
   * @throws UnsupportedOperationException if this platform cannot render the operator
   * @since 5.0
   */
  public String getLogicalOperator(LogicalOperator operator) {
    if (operator == LogicalOperator.XOR) {
      throw new UnsupportedOperationException(
              "Logical operator XOR is not supported by " + getClass().getName());
    }
    return operator.name();
  }

  /**
   * Build the statement that truncates the given table.
   *
   * @param tableName the name of the table to truncate
   * @return the {@code TRUNCATE TABLE} statement
   * @since 5.0
   */
  public String getTruncateTableStatement(String tableName) {
    return "TRUNCATE TABLE " + tableName;
  }

  /**
   * Append a {@code SELECT COUNT(*)} query for the given table to the supplied
   * buffer.
   *
   * @param countSql the buffer to append to
   * @param tableName the name of the table to count
   * @since 5.0
   */
  public void selectCountFrom(StringBuilder countSql, String tableName) {
    countSql.append("SELECT COUNT(*) FROM `")
            .append(tableName)
            .append('`');
  }

  /**
   * Append pagination to a SELECT statement, after its ORDER BY clause.
   * The default implementation uses SQL-standard OFFSET/FETCH syntax.
   * Dialects may override this method to modify the supplied statement as needed.
   *
   * <p>This method does not add ordering. Callers should supply a deterministic
   * ORDER BY for stable pages; some databases require ordering for pagination.
   *
   * @param sql the SELECT statement buffer
   * @param limit the maximum row count, or {@code null} for no limit
   * @param offset the number of rows to skip, or {@code null} for no offset
   * @throws IllegalArgumentException if either value is negative
   * @since 5.0
   */
  public void appendPagination(StringBuilder sql, @Nullable Integer limit, @Nullable Integer offset) {
    if ((limit != null && limit < 0) || (offset != null && offset < 0)) {
      throw new IllegalArgumentException("Limit and offset must not be negative");
    }
    if (offset != null && offset > 0) {
      sql.append(" OFFSET ").append(offset).append(" ROWS");
    }
    if (limit != null) {
      sql.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
    }
  }

  /**
   * Return the ANSI SQL platform, the fallback for databases without a dedicated
   * implementation.
   *
   * @return a new {@link GenericPlatform}
   */
  public static Platform generic() {
    return new GenericPlatform();
  }

  /**
   * Return the MySQL platform, also used for MariaDB.
   *
   * @return a new {@link MySQLPlatform}
   * @see MySQLPlatform
   * @since 5.0
   */
  public static Platform mysql() {
    return new MySQLPlatform();
  }

  /**
   * Select the platform for the database behind the given data source.
   *
   * <p>Reads the {@linkplain DatabaseMetaData#getDatabaseProductName() database
   * product name} from the data source's JDBC metadata and resolves it through
   * {@link #forDatabaseProductName(String)}. A {@code null} data source, or one
   * whose metadata cannot be obtained, yields the {@linkplain #generic() generic
   * platform} instead of failing.
   *
   * @param dataSource the data source to inspect, possibly {@code null}
   * @return the matching platform, never {@code null}
   * @see #forDatabaseMetaData(DatabaseMetaData)
   * @see #forDatabaseProductName(String)
   * @since 5.0
   */
  public static Platform forDataSource(@Nullable DataSource dataSource) {
    if (dataSource == null) {
      return generic();
    }
    try {
      return JdbcUtils.extractDatabaseMetaData(dataSource, Platform::forDatabaseMetaData);
    }
    catch (MetaDataAccessException ex) {
      log.debug("Cannot resolve a Platform from the DataSource metadata, falling back to GenericPlatform", ex);
      return generic();
    }
  }

  /**
   * Select the platform matching the database described by the given JDBC
   * metadata.
   *
   * @param metaData the database metadata to inspect
   * @return the matching platform; {@link GenericPlatform} when the product name
   * has no dedicated implementation
   * @throws SQLException if the database product name cannot be read from the
   * metadata
   * @see #forDatabaseProductName(String)
   * @since 5.0
   */
  public static Platform forDatabaseMetaData(DatabaseMetaData metaData) throws SQLException {
    return forDatabaseProductName(metaData.getDatabaseProductName());
  }

  /**
   * Select the platform matching the given database product name, as returned by
   * {@link DatabaseMetaData#getDatabaseProductName()}.
   *
   * <p>Names that map to no dedicated platform fall back to
   * {@link GenericPlatform}; a {@code null} name is treated the same way.
   *
   * @param productName the database product name, possibly {@code null}
   * @return the matching platform, never {@code null}
   * @see #forDriver(DatabaseDriver)
   * @since 5.0
   */
  public static Platform forDatabaseProductName(@Nullable String productName) {
    return forDriver(DatabaseDriver.fromProductName(productName));
  }

  /**
   * Select the platform matching the given database driver.
   *
   * @param driver the database driver, never {@code null}
   * @return the matching platform, never {@code null}
   * @see #generic()
   * @see #mysql()
   * @since 5.0
   */
  public static Platform forDriver(DatabaseDriver driver) {
    return switch (driver) {
      case MYSQL, MARIADB -> mysql();
      default -> generic();
    };
  }

}
