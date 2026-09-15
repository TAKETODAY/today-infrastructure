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
import infra.util.StringUtils;

/**
 * An abstract class representing a database platform. This class provides
 * utility methods and configurations for handling SQL syntax and database-specific
 * behaviors. It serves as a base for concrete platform implementations such as
 * {@code MySQLPlatform}, {@code OraclePlatform}, and {@code PostgreSQLPlatform}.
 *
 * <p>This class includes constants, static utility methods, and abstract methods
 * that must be implemented by subclasses to handle platform-specific SQL generation.
 *
 * <p><b>Subclassing Example:</b>
 * To create a custom platform implementation, extend this class and override
 * necessary methods. For example:
 * <pre>{@code
 * public class CustomPlatform extends Platform {
 *   @Override
 *   public String getForUpdateString() {
 *     return " FOR UPDATE NOWAIT";
 *   }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0 2021/10/10 13:11
 */
public abstract class Platform {

  private static final Logger log = LoggerFactory.getLogger(Platform.class);

  /**
   * Characters used as opening for quoting SQL identifiers
   */
  public static final String QUOTE = "`\"[";

  /**
   * Characters used as closing for quoting SQL identifiers
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
   * @return the closing quote character
   * @see #openQuote()
   */
  public char closeQuote() {
    return '"';
  }

  /**
   * Wrap the given name in this platform's quote characters.
   *
   * @param name the identifier to quote, possibly {@code null}
   * @return the quoted identifier, or {@code null} when the given name is {@code null}
   */
  public @Nullable String toQuotedIdentifier(@Nullable String name) {
    if (name == null) {
      return null;
    }
    return new StringBuilder(name.length() + 2)
            .append(openQuote())
            .append(name)
            .append(closeQuote())
            .toString();
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

  public static CharSequence escapeComment(CharSequence comment) {
    if (StringUtils.isNotEmpty(comment)) {
      final String escaped = ESCAPE_CLOSING_COMMENT_PATTERN.matcher(comment).replaceAll("*\\\\/");
      return ESCAPE_OPENING_COMMENT_PATTERN.matcher(escaped).replaceAll("/\\\\*");
    }
    return comment;
  }

  /**
   * determine the appropriate for update fragment to use.
   *
   * @return The appropriate for update fragment.
   */
  public String getForUpdateString() {
    return " for update";
  }

  /**
   * The fragment used to insert a row without specifying any column values.
   * This is not possible on some databases.
   *
   * @return The appropriate empty values clause.
   */
  public String getNoColumnsInsertString() {
    return "VALUES ( )";
  }

  /**
   * Create a {@link JoinFragment} strategy responsible
   * for handling this dialect's variations in how joins are handled.
   *
   * @return This dialect's {@link JoinFragment} strategy.
   */
  public JoinFragment createOuterJoinFragment() {
    return new ANSIJoinFragment();
  }

  /**
   * A SQL statement that truncates the given table.
   *
   * @param tableName the name of the table
   * @since 5.0
   */
  public String getTruncateTableStatement(String tableName) {
    return "TRUNCATE TABLE " + tableName;
  }

  /**
   * SELECT COUNT
   *
   * @param tableName the name of the table
   * @since 5.0
   */
  public void selectCountFrom(StringBuilder countSql, String tableName) {
    countSql.append("SELECT COUNT(*) FROM `")
            .append(tableName)
            .append('`');
  }

  //

  /**
   * ANSI SQL Platform
   */
  public static Platform generic() {
    return new GenericPlatform();
  }

  /**
   * MySQL Platform
   *
   * @see MySQLPlatform
   * @since 5.0
   */
  public static Platform mysql() {
    return new MySQLPlatform();
  }

  /**
   * Select the platform for the database behind the given data source.
   *
   * <p>The database product name is read from JDBC metadata. If metadata cannot
   * be obtained, the ANSI-compatible {@link GenericPlatform} is returned.
   *
   * @param dataSource the data source to inspect, possibly {@code null}
   * @return the matching platform, never {@code null}
   * @see #forDatabaseMetaData(DatabaseMetaData)
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
   * Select the platform matching the given JDBC metadata.
   *
   * @param metaData the database metadata to inspect
   * @return the matching platform, never {@code null}
   * @throws SQLException if the database product name cannot be read
   * @see #forDatabaseProductName(String)
   */
  public static Platform forDatabaseMetaData(DatabaseMetaData metaData) throws SQLException {
    return forDatabaseProductName(metaData.getDatabaseProductName());
  }

  /**
   * Select the platform matching the given database product name.
   *
   * @param productName the database product name, possibly {@code null}
   * @return the matching platform, never {@code null}
   * @see #forDriver(DatabaseDriver)
   */
  public static Platform forDatabaseProductName(@Nullable String productName) {
    return forDriver(DatabaseDriver.fromProductName(productName));
  }

  /**
   * Select the platform matching the given database driver.
   *
   * @param driver the database driver, never {@code null}
   * @return the matching platform, never {@code null}
   */
  public static Platform forDriver(DatabaseDriver driver) {
    return switch (driver) {
      case MYSQL, MARIADB -> mysql();
      default -> generic();
    };
  }

}
