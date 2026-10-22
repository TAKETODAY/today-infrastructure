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
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.InfraStrategies;
import infra.util.LogFormatUtils;
import infra.util.StringUtils;

/**
 * Logs SQL statements, bound parameters, execution results, and slow queries.
 * Supports SQL formatting, ANSI syntax highlighting, and optional standard output.
 *
 * <p>SQL statements and execution details use the {@code infra.SQL} logger at
 * DEBUG level. {@link #isTraceEnabled()} controls whether callers should collect
 * detailed parameter and result information; enabling standard output alone does
 * not enable this collection. Slow queries use {@code infra.SQL_SLOW} at INFO level.
 * Detail collection can also be explicitly enabled through the {@code trace}
 * option when DEBUG logging or standard output is enabled.
 * The {@code stdoutOnly} option bypasses {@code infra.SQL}, but does not suppress
 * the slow-query logger. Slow queries are additionally written to standard output
 * only when {@code logToStdout} is enabled.
 *
 * <p>For example:
 *
 * <pre>{@code
 * SqlStatementLogger logger = SqlStatementLogger.sharedInstance;
 * logger.logStatement("SELECT * FROM users");
 * logger.logStatement("Fetch active users", "SELECT * FROM users WHERE active = 1");
 *
 * // Record the start time immediately before executing the query.
 * // Slow-query logging requires a positive configured threshold.
 * long startTimeNanos = System.nanoTime();
 * // Execute the query...
 * logger.logSlowQuery("SELECT * FROM large_table", startTimeNanos);
 * }</pre>
 *
 * <p>The {@link #sharedInstance shared instance} reads the following
 * {@link InfraStrategies} properties once, when this class is initialized:
 * <ul>
 * <li>{@code infra.sql.logging.stdout}: also write to standard output (default: {@code false}).
 * <li>{@code infra.sql.logging.trace}: collect parameter and result details when
 * DEBUG logging or standard output is enabled, even without TRACE logging
 * (default: {@code false}). Does not change the underlying logger's level.
 * <li>{@code infra.sql.logging.format}: format SQL statements (default: {@code true}).
 * <li>{@code infra.sql.logging.highlight}: apply ANSI highlighting (default: {@code true}).
 * <li>{@code infra.sql.logging.stdout-only}: write SQL statements and execution
 * details only to standard output (default: {@code false}).
 * <li>{@code infra.sql.logging.slow-query-threshold-millis}: slow-query threshold
 * in milliseconds; non-positive values disable slow-query logging (default: {@code 0}).
 * <li>{@code infra.sql.logging.stdout-prefix}: prefix for SQL statements and
 * execution details on standard output (default: {@value #DEFAULT_LOG_PREFIX}).
 * <li>{@code infra.sql.logging.string-preview-length}: maximum character count
 * for parameter previews; non-positive values disable truncation
 * (default: {@value #DEFAULT_STRING_PREVIEW_LENGTH}).
 * </ul>
 *
 * <p>Instances are immutable and may be shared between threads. Custom
 * {@link SQLFormatter} implementations supplied by callers must support concurrent
 * use if shared between threads.
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/12 19:19
 */
public class SqlStatementLogger {

  private static final Logger sqlLogger = LoggerFactory.getLogger("infra.SQL");

  private static final Logger slowLogger = LoggerFactory.getLogger("infra.SQL_SLOW");

  /**
   * Default prefix for SQL statements and execution details on standard output.
   */
  public static final String DEFAULT_LOG_PREFIX = "today-infrastructure";

  /**
   * Default maximum number of characters logged for a {@code CharSequence} parameter
   * value.
   *
   * @since 5.0
   */
  public static final int DEFAULT_STRING_PREVIEW_LENGTH = 10;

  /**
   * Shared logger configured through {@link InfraStrategies} at class initialization.
   *
   * @see SqlStatementLogger
   */
  public static final SqlStatementLogger sharedInstance = new SqlStatementLogger(
          InfraStrategies.getFlag("infra.sql.logging.stdout", false),
          InfraStrategies.getFlag("infra.sql.logging.format", true),
          InfraStrategies.getFlag("infra.sql.logging.highlight", true),
          InfraStrategies.getFlag("infra.sql.logging.stdout-only", false),
          InfraStrategies.getLong("infra.sql.logging.slow-query-threshold-millis", 0),
          InfraStrategies.getProperty("infra.sql.logging.stdout-prefix", DEFAULT_LOG_PREFIX),
          InfraStrategies.getInt("infra.sql.logging.string-preview-length", DEFAULT_STRING_PREVIEW_LENGTH),
          InfraStrategies.getFlag("infra.sql.logging.trace", false)
  );

  private final boolean format;

  private final boolean logToStdout;

  private final boolean stdoutOnly;

  private final boolean highlight;

  private final boolean trace;

  /**
   * Slow-query threshold in milliseconds; non-positive values disable logging.
   */
  private final long logSlowQuery;

  /**
   * @since 5.0
   */
  private final String stdoutOnlyPrefix;

  /**
   * Maximum number of characters logged for a {@code CharSequence} parameter value; a
   * non-positive value disables truncation.
   *
   * @since 5.0
   */
  private final int stringPreviewLength;

  /**
   * Create a logger with the default standard-output prefix and parameter preview
   * length, without bypassing the SQL logger.
   *
   * @param logToStdout whether to also log to standard output
   * @param format whether to format SQL statements
   * @param highlight whether to apply ANSI highlighting to SQL statements
   * @param logSlowQuery slow-query threshold in milliseconds; non-positive values disable logging
   */
  public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, long logSlowQuery) {
    this(logToStdout, format, highlight, false, logSlowQuery, DEFAULT_LOG_PREFIX, DEFAULT_STRING_PREVIEW_LENGTH);
  }

  /**
   * Create a logger with the default parameter preview length.
   *
   * @param logToStdout whether to also log to standard output
   * @param format whether to format SQL statements
   * @param highlight whether to apply ANSI highlighting to SQL statements
   * @param stdoutOnly whether to bypass the SQL logger and write statements and
   * execution details only to standard output; does not affect the slow-query logger
   * @param logSlowQuery slow-query threshold in milliseconds; non-positive values disable logging
   * @param stdoutOnlyPrefix prefix for statements and execution details on standard
   * output, or {@code null} to use {@link #DEFAULT_LOG_PREFIX}
   */
  public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, boolean stdoutOnly,
          long logSlowQuery, @Nullable String stdoutOnlyPrefix) {
    this(logToStdout, format, highlight, stdoutOnly, logSlowQuery, stdoutOnlyPrefix, DEFAULT_STRING_PREVIEW_LENGTH);
  }

  /**
   * Create a logger with explicit output, formatting, and parameter preview options.
   *
   * @param logToStdout whether to also log to standard output
   * @param format whether to format SQL statements
   * @param highlight whether to apply ANSI highlighting to SQL statements
   * @param stdoutOnly whether to bypass the SQL logger and write statements and
   * execution details only to standard output; does not affect the slow-query logger
   * @param logSlowQuery slow-query threshold in milliseconds; non-positive values disable logging
   * @param stdoutOnlyPrefix prefix for statements and execution details on standard
   * output, or {@code null} to use {@link #DEFAULT_LOG_PREFIX}
   * @param stringPreviewLength maximum number of characters logged for a
   * {@code CharSequence} parameter value; a non-positive value disables truncation
   * @since 5.0
   */
  public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, boolean stdoutOnly,
          long logSlowQuery, @Nullable String stdoutOnlyPrefix, int stringPreviewLength) {
    this(logToStdout, format, highlight, stdoutOnly, logSlowQuery, stdoutOnlyPrefix, stringPreviewLength, false);
  }

  /**
   * Create a logger with explicit output, formatting, parameter preview, and detail
   * collection options.
   *
   * @param logToStdout whether to also log to standard output
   * @param format whether to format SQL statements
   * @param highlight whether to apply ANSI highlighting to SQL statements
   * @param stdoutOnly whether to bypass the SQL logger and write statements and
   * execution details only to standard output; does not affect the slow-query logger
   * @param logSlowQuery slow-query threshold in milliseconds; non-positive values disable logging
   * @param stdoutOnlyPrefix prefix for statements and execution details on standard
   * output, or {@code null} to use {@link #DEFAULT_LOG_PREFIX}
   * @param stringPreviewLength maximum number of characters logged for a
   * {@code CharSequence} parameter value; a non-positive value disables truncation
   * @param trace whether to collect parameter and result details when DEBUG logging
   * or standard output is enabled, even if the SQL logger is not enabled at TRACE
   * level; does not change the underlying logger's level
   * @since 5.0
   */
  public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, boolean stdoutOnly,
          long logSlowQuery, @Nullable String stdoutOnlyPrefix, int stringPreviewLength, boolean trace) {
    this.logToStdout = logToStdout;
    this.stdoutOnly = stdoutOnly;
    this.stdoutOnlyPrefix = Objects.requireNonNullElse(stdoutOnlyPrefix, DEFAULT_LOG_PREFIX);
    this.format = format;
    this.highlight = highlight;
    this.logSlowQuery = logSlowQuery;
    this.stringPreviewLength = stringPreviewLength;
    this.trace = trace;
  }

  /**
   * Return whether SQL statement output is enabled through the DEBUG logger or
   * either standard-output option.
   *
   * @return whether SQL statements can be written to at least one output
   */
  public boolean isDebugEnabled() {
    return sqlLogger.isDebugEnabled() || logToStdout || stdoutOnly;
  }

  /**
   * Return whether callers should collect detailed parameter and result information.
   * Enabled when {@code infra.SQL} is enabled at TRACE level, or when the explicit
   * {@code trace} option is enabled and {@link #isDebugEnabled()} reports an available
   * output. Standard-output options alone do not enable detail collection.
   *
   * @return whether statement parameters and results should be logged
   * @since 5.0
   */
  public boolean isTraceEnabled() {
    return sqlLogger.isTraceEnabled() || (trace && isDebugEnabled());
  }

  /**
   * Return whether the {@code infra.SQL_SLOW} logger is enabled at DEBUG level.
   * This is not the level used by {@link #logSlowQuery(String, long)}, which logs
   * at INFO level.
   *
   * @return whether the slow-query logger is enabled at DEBUG level
   */
  public boolean isSlowDebugEnabled() {
    return slowLogger.isDebugEnabled();
  }

  /**
   * Log a SQL statement using {@link BasicSQLFormatter} when formatting is enabled.
   *
   * @param statement the SQL statement
   */
  public void logStatement(String statement) {
    logStatement(null, statement);
  }

  /**
   * Log a SQL statement with an optional description, using
   * {@link BasicSQLFormatter} when formatting is enabled.
   *
   * @param desc description to include in logger output, or {@code null} for none;
   * omitted from standard output
   * @param statement the SQL statement
   */
  public void logStatement(@Nullable Object desc, CharSequence statement) {
    // for now just assume a DML log for formatting
    logStatement(desc, statement, BasicSQLFormatter.INSTANCE);
  }

  /**
   * Log a SQL statement using the specified formatter when formatting is enabled.
   *
   * @param statement the SQL statement
   * @param formatter the formatter to use when SQL formatting is enabled
   */
  public void logStatement(String statement, SQLFormatter formatter) {
    logStatement(null, statement, formatter);
  }

  /**
   * Log a SQL statement with an optional description and formatter.
   * Formatting and ANSI highlighting are applied independently according to this
   * logger's configuration, before writing to the configured outputs.
   *
   * @param desc description to include in logger output, or {@code null} for none;
   * omitted from standard output
   * @param statement the SQL statement
   * @param formatter the formatter to use when SQL formatting is enabled
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
   * Log the bound parameters of a prepared statement, MyBatis style.
   *
   * @param parameters the parameter values in bind order; may be {@code null} or
   * empty to log nothing
   */
  protected void logParameters(@Nullable Object @Nullable [] parameters) {
    if (parameters == null || parameters.length == 0) {
      return;
    }
    String text = "==> Parameters: " + formatParameters(parameters);
    logOutcome(text);
  }

  /**
   * Log the outcome of an executed statement, MyBatis style, e.g.
   * {@code <== Updates: 3} or {@code <== Total: 5}.
   *
   * @param action the outcome label, e.g. {@code Updates} or {@code Total}
   * @param count the affected or returned row count
   */
  protected void logResult(String action, int count) {
    logOutcome("<== " + action + ": " + count);
  }

  /**
   * Write a preformatted parameter or result line to {@code infra.SQL} at DEBUG
   * level and/or standard output, according to the output options.
   * This method does not check {@link #isTraceEnabled()}; callers are responsible
   * for deciding whether to collect execution details.
   *
   * @param text the fully formatted line to log
   */
  protected void logOutcome(String text) {
    if (!stdoutOnly) {
      sqlLogger.debug(text);
    }
    if (stdoutOnly || logToStdout) {
      String prefix = highlight ? "\u001b[35m[" + this.stdoutOnlyPrefix + "]\u001b[0m " : this.stdoutOnlyPrefix + ": ";
      System.out.println(prefix + text);
    }
  }

  /**
   * Format parameter values as a comma-separated list, e.g.
   * {@code [10(Integer), TODAY(String)]}.
   * Character sequences are truncated according to the configured preview length;
   * byte arrays are represented by their length, and {@code null} values by
   * {@code "null"}.
   *
   * @param parameters the parameter values in bind order
   * @return the formatted representation, never {@code null}
   */
  String formatParameters(@Nullable Object[] parameters) {
    StringBuilder sb = new StringBuilder(parameters.length * 12 + 2);
    sb.append('[');
    for (int i = 0; i < parameters.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(formatParameter(parameters[i]));
    }
    return sb.append(']').toString();
  }

  private String formatParameter(@Nullable Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof byte[] bytes) {
      return "byte[" + bytes.length + "]";
    }
    Object text = value;
    if (value instanceof CharSequence cs && stringPreviewLength > 0) {
      text = StringUtils.truncate(cs, stringPreviewLength);
    }
    return text + "(" + value.getClass().getSimpleName() + ")";
  }

  /**
   * Log a JDBC statement if its elapsed execution time exceeds the configured
   * slow-query threshold. Uses {@link Statement#toString()} as the SQL text.
   *
   * @param statement the executed JDBC statement
   * @param startTimeNanos execution start time obtained from {@link System#nanoTime()}
   * @throws IllegalArgumentException if slow-query logging is enabled and
   * {@code startTimeNanos} is not positive
   * @see #logSlowQuery(String, long)
   */
  public void logSlowQuery(Statement statement, long startTimeNanos) {
    if (logSlowQuery < 1) {
      return;
    }
    logSlowQuery(statement.toString(), startTimeNanos);
  }

  /**
   * Log a query at INFO level through {@code infra.SQL_SLOW} if its elapsed
   * execution time, converted to whole milliseconds, strictly exceeds the configured
   * threshold. Also writes to standard output when {@code logToStdout} is enabled.
   * Does nothing, including skipping start-time validation, if the threshold is
   * non-positive. SQL formatting, highlighting, and the standard-output prefix
   * are not applied to slow-query messages.
   *
   * @param sql the executed SQL query
   * @param startTimeNanos execution start time obtained from {@link System#nanoTime()}
   * @throws IllegalArgumentException if slow-query logging is enabled and
   * {@code startTimeNanos} is not positive
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
            .append("trace", trace)
            .append("logSlowQuery", logSlowQuery)
            .toString();
  }

}
