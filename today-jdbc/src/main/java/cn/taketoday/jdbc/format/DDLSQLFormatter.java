/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.format;

import java.util.Locale;
import java.util.StringTokenizer;

import cn.taketoday.util.StringUtils;

/**
 * Performs formatting of DDL SQL statements.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/12 19:20
 */
public class DDLSQLFormatter implements SQLFormatter {

  private static final String INITIAL_LINE = System.lineSeparator() + "    ";
  private static final String OTHER_LINES = System.lineSeparator() + "       ";

  /**
   * Singleton access
   */
  public static final DDLSQLFormatter INSTANCE = new DDLSQLFormatter();

  @Override
  public String format(String sql) {
    if (StringUtils.isEmpty(sql)) {
      return sql;
    }

    String lowerCase = sql.toLowerCase(Locale.ROOT);
    if (lowerCase.startsWith("create table")) {
      return formatCreateTable(sql);
    }
    else if (lowerCase.startsWith("create")) {
      return sql;
    }
    else if (lowerCase.startsWith("alter table")) {
      return formatAlterTable(sql);
    }
    else if (lowerCase.startsWith("comment on")) {
      return formatCommentOn(sql);
    }
    else {
      return INITIAL_LINE + sql;
    }
  }

  public static String formatCommentOn(String sql) {
    final StringBuilder result = new StringBuilder(60).append(INITIAL_LINE);
    final StringTokenizer tokens = new StringTokenizer(sql, " '[]\"", true);

    boolean quoted = false;
    while (tokens.hasMoreTokens()) {
      final String token = tokens.nextToken();
      result.append(token);
      if (isQuote(token)) {
        quoted = !quoted;
      }
      else if (!quoted) {
        if ("is".equals(token)) {
          result.append(OTHER_LINES);
        }
      }
    }

    return result.toString();
  }

  public static String formatAlterTable(String sql) {
    final StringBuilder result = new StringBuilder(60).append(INITIAL_LINE);
    final StringTokenizer tokens = new StringTokenizer(sql, " (,)'[]\"", true);

    boolean quoted = false;
    while (tokens.hasMoreTokens()) {
      final String token = tokens.nextToken();
      if (isQuote(token)) {
        quoted = !quoted;
      }
      else if (!quoted) {
        if (isBreak(token)) {
          result.append(OTHER_LINES);
        }
      }
      result.append(token);
    }

    return result.toString();
  }

  public static String formatCreateTable(String sql) {
    final StringBuilder result = new StringBuilder(60).append(INITIAL_LINE);
    final StringTokenizer tokens = new StringTokenizer(sql, "(,)'[]\"", true);

    int depth = 0;
    boolean quoted = false;
    while (tokens.hasMoreTokens()) {
      final String token = tokens.nextToken();
      if (isQuote(token)) {
        quoted = !quoted;
        result.append(token);
      }
      else if (quoted) {
        result.append(token);
      }
      else {
        if (")".equals(token)) {
          depth--;
          if (depth == 0) {
            result.append(INITIAL_LINE);
          }
        }
        result.append(token);
        if (",".equals(token) && depth == 1) {
          result.append(OTHER_LINES);
        }
        if ("(".equals(token)) {
          depth++;
          if (depth == 1) {
            result.append(OTHER_LINES);
          }
        }
      }
    }

    return result.toString();
  }

  private static boolean isBreak(String token) {
    return "drop".equals(token)
            || "add".equals(token)
            || "references".equals(token)
            || "foreign".equals(token)
            || "on".equals(token);
  }

  private static boolean isQuote(String tok) {
    return "\"".equals(tok)
            || "`".equals(tok)
            || "]".equals(tok)
            || "[".equals(tok)
            || "'".equals(tok);
  }

}
