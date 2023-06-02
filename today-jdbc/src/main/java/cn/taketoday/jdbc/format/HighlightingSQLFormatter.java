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

import java.util.Set;
import java.util.StringTokenizer;

/**
 * Performs basic syntax highlighting for SQL using ANSI escape codes.
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/12 19:20
 */
public final class HighlightingSQLFormatter implements SQLFormatter {

  private static final Set<String> KEYWORDS = Set.of(
          "ADD", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "ARRAY", "AS", "ASENSITIVE",
          "ASYMMETRIC", "AT", "ATOMIC", "AUTHORIZATION", "BEGIN", "BETWEEN", "BIGINT",
          "BLOB", "BINARY", "BOTH", "BY", "CALL", "CALLED", "CASCADED", "CASE", "CAST",
          "CHAR", "CHARACTER", "CHECK", "CLOB", "CLOSE", "COLLATE", "COLUMN", "COMMIT",
          "CONDITION", "CONNECT", "CONSTRAINT", "CONTINUE", "CORRESPONDING", "CREATE",
          "CROSS", "CUBE", "CURRENT", "CURRENT_DATE", "CURRENT_PATH", "CURRENT_ROLE",
          "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "CYCLE", "DATE",
          "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELETE", "DEREF",
          "DESCRIBE", "DETERMINISTIC", "DISCONNECT", "DISTINCT", "DO", "DOUBLE", "DROP",
          "DYNAMIC", "EACH", "ELEMENT", "ELSE", "ELSIF", "END", "ESCAPE", "EXCEPT", "EXEC",
          "EXECUTE", "EXISTS", "EXIT", "EXTERNAL", "FALSE", "FETCH", "FILTER", "FLOAT", "FOR",
          "FOREIGN", "FREE", "FROM", "FULL", "FUNCTION", "GET", "GLOBAL", "GRANT", "GROUP",
          "GROUPING", "HANDLER", "HAVING", "HOLD", "HOUR", "IDENTITY", "IF", "IMMEDIATE", "IN",
          "INDICATOR", "INNER", "INOUT", "INPUT", "INSENSITIVE", "INSERT", "INT", "INTEGER",
          "INTERSECT", "INTERVAL", "INTO", "IS", "ITERATE", "JOIN", "LANGUAGE", "LARGE", "LATERAL",
          "LEADING", "LEAVE", "LEFT", "LIKE", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP", "LOOP",
          "MATCH", "MEMBER", "MERGE", "METHOD", "MINUTE", "MODIFIES", "MODULE", "MONTH", "MULTISET",
          "NATIONAL", "NATURAL", "NCHAR", "NCLOB", "NEW", "NO", "NONE", "NOT", "NULL", "NUMERIC",
          "OF", "OLD", "ON", "ONLY", "OPEN", "OR", "ORDER", "OUT", "OUTER", "OUTPUT", "OVER", "OVERLAPS",
          "PARAMETER", "PARTITION", "PRECISION", "PREPARE", "PRIMARY", "PROCEDURE", "RANGE", "READS",
          "REAL", "RECURSIVE", "REF", "REFERENCES", "REFERENCING", "RELEASE", "REPEAT", "RESIGNAL",
          "RESULT", "RETURN", "RETURNS", "REVOKE", "RIGHT", "ROLLBACK", "ROLLUP", "ROW", "ROWS",
          "SAVEPOINT", "SCROLL", "SEARCH", "SECOND", "SELECT", "SENSITIVE", "SESSION_USE", "SET", "SIGNAL",
          "SIMILAR", "SMALLINT", "SOME", "SPECIFIC", "SPECIFICTYPE", "SQL", "SQLEXCEPTION", "SQLSTATE",
          "SQLWARNING", "START", "STATIC", "SUBMULTISET", "SYMMETRIC", "SYSTEM", "SYSTEM_USER", "TABLE",
          "TABLESAMPLE", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO",
          "TRAILING", "TRANSLATION", "TREAT", "TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE", "UNKNOWN", "UNNEST",
          "UNTIL", "UPDATE", "USER", "USING", "VALUE", "VALUES", "VARCHAR", "VARYING", "WHEN", "WHENEVER",
          "WHERE", "WHILE", "WINDOW", "WITH", "WITHIN", "WITHOUT", "YEAR", "KEY", "SEQUENCE", "CASCADE", "INCREMENT"
  );

  public static final SQLFormatter INSTANCE = new HighlightingSQLFormatter(
          "34", // blue
          "36", // cyan
          "32"
  );
  private static final String SYMBOLS_AND_WS = "=><!+-*/()',.|&`\"?" + WHITESPACE;

  private final String keywordEscape;
  private final String stringEscape;
  private final String quotedEscape;
  private final String normalEscape;

  /**
   * @param keywordCode the ANSI escape code to use for highlighting SQL keywords
   * @param stringCode the ANSI escape code to use for highlighting SQL strings
   */
  public HighlightingSQLFormatter(String keywordCode, String stringCode, String quotedCode) {
    this.normalEscape = escape("0");
    this.stringEscape = escape(stringCode);
    this.quotedEscape = escape(quotedCode);
    this.keywordEscape = escape(keywordCode);
  }

  private static String escape(String code) {
    return "\u001b[" + code + "m";
  }

  @Override
  public String format(String sql) {
    StringBuilder result = new StringBuilder();
    boolean inString = false;
    boolean inQuoted = false;

    StringTokenizer tokenizer = new StringTokenizer(sql, SYMBOLS_AND_WS, true);
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      switch (token) {
        case "\"":
        case "`": // for MySQL
          if (inString) {
            result.append(token);
          }
          else if (inQuoted) {
            inQuoted = false;
            result.append(token).append(normalEscape);
          }
          else {
            inQuoted = true;
            result.append(quotedEscape).append(token);
          }
          break;
        case "'":
          if (inQuoted) {
            result.append('\'');
          }
          else if (inString) {
            inString = false;
            result.append('\'').append(normalEscape);
          }
          else {
            inString = true;
            result.append(stringEscape).append('\'');
          }
          break;
        default:
          if (KEYWORDS.contains(token.toUpperCase())) {
            result.append(keywordEscape).append(token).append(normalEscape);
          }
          else {
            result.append(token);
          }
      }
    }
    return result.toString();
  }

}
