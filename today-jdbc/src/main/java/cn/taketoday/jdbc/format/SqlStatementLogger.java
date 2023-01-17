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

import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Centralize logging for SQL statements.
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/12 19:19
 */
public class SqlStatementLogger {
  private static final Logger sqlLogger = LoggerFactory.getLogger("today.infra.SQL");
  private static final Logger slowLogger = LoggerFactory.getLogger("today.infra.SQL_SLOW");

  public static final SqlStatementLogger sharedInstance = new SqlStatementLogger(
          TodayStrategies.getFlag("sql.logToStdout", false),
          TodayStrategies.getFlag("sql.format", true),
          TodayStrategies.getFlag("sql.highlight", true),
          TodayStrategies.getLong("sql.logSlowQuery", 0)
  );

  private final boolean format;
  private final boolean logToStdout;
  private final boolean highlight;

  /**
   * Configuration value that indicates slow query. (In milliseconds) 0 - disabled.
   */
  private final long logSlowQuery;

  /**
   * Constructs a new SqlStatementLogger instance.
   */
  public SqlStatementLogger() {
    this(false, false, false);
  }

  /**
   * Constructs a new SqlStatementLogger instance.
   *
   * @param logToStdout Should we log to STDOUT in addition to our internal logger.
   * @param format Should we format the statements in the console and log
   */
  public SqlStatementLogger(boolean logToStdout, boolean format) {
    this(logToStdout, format, false);
  }

  /**
   * Constructs a new SqlStatementLogger instance.
   *
   * @param logToStdout Should we log to STDOUT in addition to our internal logger.
   * @param format Should we format the statements in the console and log
   * @param highlight Should we highlight the statements in the console
   */
  public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight) {
    this(logToStdout, format, highlight, 0);
  }

  /**
   * Constructs a new SqlStatementLogger instance.
   *
   * @param logToStdout Should we log to STDOUT in addition to our internal logger.
   * @param format Should we format the statements in the console and log
   * @param highlight Should we highlight the statements in the console
   * @param logSlowQuery Should we logs query which executed slower than specified milliseconds. 0 - disabled.
   */
  public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, long logSlowQuery) {
    this.logToStdout = logToStdout;
    this.format = format;
    this.highlight = highlight;
    this.logSlowQuery = logSlowQuery;
  }

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level, false otherwise.
   */
  public boolean isDebugEnabled() {
    return sqlLogger.isDebugEnabled();
  }

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level, false otherwise.
   */
  public boolean isSlowDebugEnabled() {
    return slowLogger.isDebugEnabled();
  }

  /**
   * Are we currently logging to stdout?
   *
   * @return True if we are currently logging to stdout; false otherwise.
   */
  public boolean isLogToStdout() {
    return logToStdout;
  }

  public boolean isFormat() {
    return format;
  }

  public long getLogSlowQuery() {
    return logSlowQuery;
  }

  /**
   * Log a SQL statement string.
   *
   * @param statement The SQL statement.
   */
  public void logStatement(String statement) {
    logStatement(null, statement);
  }

  /**
   * Log a SQL statement string.
   *
   * @param desc description of this SQL
   * @param statement The SQL statement.
   */
  public void logStatement(@Nullable Object desc, String statement) {
    // for now just assume a DML log for formatting
    logStatement(desc, statement, BasicSQLFormatter.INSTANCE);
  }

  /**
   * Log a SQL statement string using the specified formatter
   *
   * @param statement The SQL statement.
   * @param formatter The formatter to use.
   */
  public void logStatement(String statement, SQLFormatter formatter) {
    logStatement(null, statement, formatter);
  }

  /**
   * Log a SQL statement string using the specified formatter
   *
   * @param desc description of this SQL
   * @param statement The SQL statement.
   * @param formatter The formatter to use.
   */
  public void logStatement(@Nullable Object desc, String statement, SQLFormatter formatter) {
    if (format) {
      statement = formatter.format(statement);
    }
    if (highlight) {
      statement = HighlightingSQLFormatter.INSTANCE.format(statement);
    }
    if (desc != null) {
      sqlLogger.debug(desc + ", SQL: " + statement);
    }
    else {
      sqlLogger.debug(statement);
    }

    if (logToStdout) {
      String prefix = highlight ? "\u001b[35m[today-infrastructure]\u001b[0m " : "today-infrastructure: ";
      System.out.println(prefix + statement);
    }
  }

  /**
   * Log a slow SQL query
   *
   * @param statement SQL statement.
   * @param startTimeNanos Start time in nanoseconds.
   */
  public void logSlowQuery(Statement statement, long startTimeNanos) {
    if (logSlowQuery < 1) {
      return;
    }
    logSlowQuery(statement.toString(), startTimeNanos);
  }

  /**
   * Log a slow SQL query
   *
   * @param sql The SQL query.
   * @param startTimeNanos Start time in nanoseconds.
   */
  public void logSlowQuery(String sql, long startTimeNanos) {
    if (logSlowQuery < 1) {
      return;
    }
    if (startTimeNanos <= 0) {
      throw new IllegalArgumentException("startTimeNanos [" + startTimeNanos + "] should be greater than 0!");
    }

    long queryExecutionMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);

    if (queryExecutionMillis > logSlowQuery) {
      String logData = "SlowQuery: " + queryExecutionMillis + " milliseconds. SQL: '" + sql + "'";
      slowLogger.info(logData);
      if (logToStdout) {
        System.out.println(logData);
      }
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("format", format)
            .append("logToStdout", logToStdout)
            .append("highlight", highlight)
            .append("logSlowQuery", logSlowQuery)
            .toString();
  }

}
