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

package cn.taketoday.persistence.dialect;

import java.util.regex.Pattern;

import cn.taketoday.persistence.sql.ANSICaseFragment;
import cn.taketoday.persistence.sql.ANSIJoinFragment;
import cn.taketoday.persistence.sql.CaseFragment;
import cn.taketoday.persistence.sql.JoinFragment;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * SQL generate strategy
 *
 * @author TODAY 2021/10/10 13:11
 * @since 4.0
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
//  private static final Pattern SINGLE_QUOTE_PATTERN = Pattern.compile(
//          "'",
//          Pattern.LITERAL
//  );

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
   * Create a {@link CaseFragment} strategy responsible
   * for handling this dialect's variations in how CASE statements are
   * handled.
   *
   * @return This dialect's {@link CaseFragment} strategy.
   */
  public CaseFragment createCaseFragment() {
    return new ANSICaseFragment();
  }

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
