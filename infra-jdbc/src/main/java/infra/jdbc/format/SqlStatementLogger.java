/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.format;

import org.jspecify.annotations.Nullable;

import java.sql.Statement;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import infra.core.style.ToStringBuilder;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.LogFormatUtils;

/**
 * A utility class for logging SQL statements and slow queries with various
 * formatting and highlighting options. It supports logging to both internal
 * loggers and standard output (stdout) based on configuration.
 *
 * <p>This class provides methods to log SQL statements, format them, highlight
 * syntax, and track slow queries. It is designed to be configurable through
 * constructor parameters and environment strategies.
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 * // Log a simple SQL statement
 * SqlStatementLogger.sharedInstance.logStatement("SELECT * FROM users");
 *
 * // Log a SQL statement with a description
 * SqlStatementLogger.sharedInstance.logStatement("Fetch Users", "SELECT * FROM users WHERE active = 1");
 *
 * // Log a SQL statement with custom formatting
 * SqlStatementLogger.sharedInstance.logStatement(
 *   "Custom Formatter Example",
 *   "SELECT * FROM orders WHERE status = 'shipped'",
 *   sql -> "Formatted: " + sql
 * );
 *
 * // Log a slow query
 * long startTimeNanos = System.nanoTime();
 * // Simulate query execution
 * Thread.sleep(200);
 * SqlStatementLogger.sharedInstance.logSlowQuery("SELECT * FROM large_table", startTimeNanos);
 * }</pre>
 *
 * <p><b>Configuration Options:</b>
 * <ul>
 *   <li>{@code logToStdout}: Whether to log SQL statements to stdout in addition to the internal logger.</li>
 *   <li>{@code format}: Whether to format SQL statements for better readability.</li>
 *   <li>{@code highlight}: Whether to apply syntax highlighting to SQL statements.</li>
 *   <li>{@code logSlowQuery}: Threshold (in milliseconds) for logging slow queries. Set to 0 to disable.</li>
 *   <li>{@code stdoutOnly}: If true, logs are written only to stdout and not to the internal logger.</li>
 *   <li>{@code stdoutOnlyPrefix}: Prefix for stdout-only logs.</li>
 * </ul>
 *
 * <p>This class is thread-safe and can be used as a singleton via {@link #sharedInstance}.
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/12 19:19
 */
public class SqlStatementLogger {

  private static final Logger sqlLogger = LoggerFactory.getLogger("today.SQL");

  private static final Logger slowLogger = LoggerFactory.getLogger("today.SQL_SLOW");

  public static final String DEFAULT_LOG_PREFIX = "today-infrastructure";

  public static final SqlStatementLogger sharedInstance = new SqlStatementLogger(
          TodayStrategies.getFlag("sql.logToStdout", false),
          TodayStrategies.getFlag("sql.format", true),
          TodayStrategies.getFlag("sql.highlight", true),
          TodayStrategies.getFlag("sql.stdoutOnly", false),
          TodayStrategies.getLong("sql.logSlowQuery", 0),
          TodayStrategies.getProperty("sql.stdoutOnlyPrefix", DEFAULT_LOG_PREFIX)
  );

  private final boolean format;

  private final boolean logToStdout;

  private final boolean stdoutOnly;

  private final boolean highlight;

  /**
   * Configuration value that indicates slow query. (In milliseconds) 0 - disabled.
   */
  private final long logSlowQuery;

  /**
   * @since 5.0
   */
  private final String stdoutOnlyPrefix;

  /**
   * Constructs a new SqlStatementLogger instance.
   *
   * @param logToStdout Should we log to STDOUT in addition to our internal logger.
   * @param format Should we format the statements in the console and log
   * @param highlight Should we highlight the statements in the console
   * @param logSlowQuery Should we logs query which executed slower than specified milliseconds. 0 - disabled.
   */
  public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, long logSlowQuery) {
    this(logToStdout, format, highlight, false, logSlowQuery, DEFAULT_LOG_PREFIX);
  }

  /**
   * Constructs a new SqlStatementLogger instance.
   *
   * @param logToStdout Should we log to STDOUT in addition to our internal logger
   * @param format Should we format the statements in the console and log
   * @param highlight Should we highlight the statements in the console
   * @param stdoutOnly just log to std out
   * @param logSlowQuery Should we logs query which executed slower than specified milliseconds, 0 - disabled
   * @param stdoutOnlyPrefix stdout-only log prefix
   */
  public SqlStatementLogger(boolean logToStdout, boolean format,
          boolean highlight, boolean stdoutOnly, long logSlowQuery, @Nullable String stdoutOnlyPrefix) {
    this.logToStdout = logToStdout;
    this.format = format;
    this.highlight = highlight;
    this.stdoutOnly = stdoutOnly;
    this.logSlowQuery = logSlowQuery;
    this.stdoutOnlyPrefix = Objects.requireNonNullElse(stdoutOnlyPrefix, DEFAULT_LOG_PREFIX);
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
  public void logStatement(@Nullable Object desc, CharSequence statement) {
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
  public void logStatement(@Nullable Object desc, CharSequence statement, SQLFormatter formatter) {
    if (format) {
      statement = formatter.format(statement.toString());
    }
    if (highlight) {
      statement = HighlightingSQLFormatter.INSTANCE.format(statement.toString());
    }

    if (!stdoutOnly) {
      if (desc != null) {
        String sql = statement.toString();
        LogFormatUtils.traceDebug(sqlLogger,
                traceOn -> LogFormatUtils.formatValue(desc, !traceOn) + ", SQL: " + sql);
      }
      else {
        sqlLogger.debug(statement);
      }
    }

    if (stdoutOnly || logToStdout) {
      String prefix = highlight ? "\u001b[35m[" + this.stdoutOnlyPrefix + "]\u001b[0m " : this.stdoutOnlyPrefix + ": ";
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
      throw new IllegalArgumentException("startTimeNanos [%d] should be greater than 0!".formatted(startTimeNanos));
    }

    long queryExecutionMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);

    if (queryExecutionMillis > logSlowQuery) {
      String logData = "SlowQuery: %d milliseconds. SQL: '%s'".formatted(queryExecutionMillis, sql);
      slowLogger.info(logData);
      if (logToStdout) {
        System.out.println(logData);
      }
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("format", format)
            .append("logToStdout", logToStdout)
            .append("highlight", highlight)
            .append("logSlowQuery", logSlowQuery)
            .toString();
  }

}
