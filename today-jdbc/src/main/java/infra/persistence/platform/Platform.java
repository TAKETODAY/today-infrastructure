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

package infra.persistence.platform;

import java.util.regex.Pattern;

import infra.persistence.sql.ANSIJoinFragment;
import infra.persistence.sql.JoinFragment;
import infra.util.ClassUtils;
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
 * <p><b>Usage Example:</b>
 * Below is an example of how to use the {@code Platform} class to determine the
 * appropriate database platform based on the classpath:
 * <pre>{@code
 * Platform platform = Platform.forClasspath();
 * String truncateStatement = platform.getTruncateTableStatement("example_table");
 * System.out.println(truncateStatement);
 * }</pre>
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
   * Determines the database platform based on the classpath. This method checks for the presence
   * of specific JDBC driver classes in the classpath and returns an appropriate {@link Platform}
   * instance corresponding to the detected database.
   *
   * <p>Usage example:</p>
   * <pre>{@code
   *   Platform platform = Platform.forClasspath();
   *   if (platform instanceof MySQLPlatform) {
   *     System.out.println("MySQL database platform detected.");
   *   } else if (platform instanceof OraclePlatform) {
   *     System.out.println("Oracle database platform detected.");
   *   } else if (platform instanceof PostgreSQLPlatform) {
   *     System.out.println("PostgreSQL database platform detected.");
   *   }
   * }</pre>
   *
   * <p>If no supported database platform is detected, an {@link IllegalStateException} is thrown.</p>
   *
   * @return a {@link Platform} instance representing the detected database platform
   * @throws IllegalStateException if the database platform cannot be determined from the classpath
   */
  public static Platform forClasspath() {
    if (ClassUtils.isPresent("com.mysql.cj.jdbc.Driver")) {
      return new MySQLPlatform();
    }
    else if (ClassUtils.isPresent("oracle.jdbc.driver.OracleDriver")) {
      return new OraclePlatform();
    }
    else if (ClassUtils.isPresent("org.postgresql.Driver")) {
      return new PostgreSQLPlatform();
    }
    throw new IllegalStateException("Cannot determine database platform");
  }

}
