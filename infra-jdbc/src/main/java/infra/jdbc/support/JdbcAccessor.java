/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.support;

import org.jspecify.annotations.Nullable;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import javax.sql.DataSource;

import infra.dao.DataAccessException;
import infra.jdbc.SQLWarningException;
import infra.jdbc.UncategorizedSQLException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.SqlProvider;
import infra.jdbc.format.SqlStatementLogger;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.Assert;

/**
 * Base class for {@link JdbcTemplate} and other JDBC-accessing helpers,
 * providing a DataSource, exception translation, statement logging, and
 * SQL warning handling.
 *
 * <p>A non-null {@link DataSource} is required at construction time, and its
 * reference remains fixed for the lifetime of the accessor. Other configuration
 * properties can be customized independently.
 *
 * <p>The exception translator is initialized lazily unless explicitly supplied.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JdbcTemplate
 * @since 4.0
 */
public abstract class JdbcAccessor {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  protected final DataSource dataSource;

  private volatile @Nullable SQLExceptionTranslator exceptionTranslator;

  /** If this variable is false, we will throw exceptions on SQL warnings. */
  private boolean ignoreWarnings = true;

  /**
   * Create an accessor with a fixed JDBC DataSource.
   *
   * @param dataSource the DataSource to obtain connections from (never {@code null})
   * @throws IllegalArgumentException if {@code dataSource} is {@code null}
   */
  protected JdbcAccessor(DataSource dataSource) {
    Assert.notNull(dataSource, "dataSource is required");
    this.dataSource = dataSource;
  }

  /**
   * Create an accessor with the given DataSource and a snapshot of the original
   * accessor's configuration.
   * <p>Copies the statement logger, warning policy, and current exception
   * translator reference without triggering translator initialization.
   *
   * @param original the accessor to copy from
   * @param dataSource the DataSource to use (never {@code null})
   * @throws IllegalArgumentException if {@code dataSource} is {@code null}
   */
  protected JdbcAccessor(JdbcAccessor original, DataSource dataSource) {
    this(dataSource);
    this.stmtLogger = original.stmtLogger;
    this.ignoreWarnings = original.ignoreWarnings;
    this.exceptionTranslator = original.exceptionTranslator;
  }

  /**
   * Return the DataSource supplied at construction time.
   *
   * @return the DataSource used to obtain connections (never {@code null})
   */
  public DataSource getDataSource() {
    return this.dataSource;
  }

  /**
   * Configure exception translation using the given database product name.
   * <p>When user-provided error codes are available, creates an
   * {@link SQLErrorCodeSQLExceptionTranslator} without obtaining a connection
   * for database metadata. Otherwise, uses an {@link SQLExceptionSubclassTranslator}.
   * Any previously configured translator is replaced.
   *
   * @param dbName the database product name that identifies the error codes entry
   * @see SQLErrorCodeSQLExceptionTranslator#setDatabaseProductName
   * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   */
  public void setDatabaseProductName(String dbName) {
    if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
      this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
    }
    else {
      this.exceptionTranslator = new SQLExceptionSubclassTranslator();
    }
  }

  /**
   * Set the exception translator to use instead of the lazily initialized default.
   *
   * @param exceptionTranslator the translator to use
   * @see #getExceptionTranslator()
   * @see SQLErrorCodeSQLExceptionTranslator
   * @see SQLStateSQLExceptionTranslator
   */
  public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
    this.exceptionTranslator = exceptionTranslator;
  }

  /**
   * Return the exception translator for this instance.
   * <p>If none is set, creates an {@link SQLErrorCodeSQLExceptionTranslator}
   * for the DataSource when user-provided error codes are available, or an
   * {@link SQLExceptionSubclassTranslator} otherwise.
   *
   * @return the configured or lazily initialized translator (never {@code null})
   * @see #getDataSource()
   */
  public SQLExceptionTranslator getExceptionTranslator() {
    SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
    if (exceptionTranslator != null) {
      return exceptionTranslator;
    }
    synchronized(this) {
      exceptionTranslator = this.exceptionTranslator;
      if (exceptionTranslator == null) {
        if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
          exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(getDataSource());
        }
        else {
          exceptionTranslator = new SQLExceptionSubclassTranslator();
        }
        this.exceptionTranslator = exceptionTranslator;
      }
      return exceptionTranslator;
    }
  }

  /**
   * Set whether JDBC statement warnings should be ignored.
   * <p>Defaults to {@code true}: warnings are logged when debug logging is enabled.
   * Set to {@code false} to raise a {@link SQLWarningException} instead
   * (or chain the {@link SQLWarning} into the primary {@link SQLException}, if any).
   *
   * @param ignoreWarnings whether to ignore statement warnings
   * @see Statement#getWarnings()
   * @see java.sql.SQLWarning
   * @see SQLWarningException
   * @see #handleWarnings(Statement)
   */
  public void setIgnoreWarnings(boolean ignoreWarnings) {
    this.ignoreWarnings = ignoreWarnings;
  }

  /**
   * Return whether JDBC statement warnings are ignored.
   *
   * @return {@code true} if warnings are ignored; {@code false} if they are raised
   */
  public boolean isIgnoreWarnings() {
    return this.ignoreWarnings;
  }

  /**
   * Set the logger used for SQL statements.
   *
   * @param stmtLogger the statement logger (never {@code null})
   * @throws IllegalArgumentException if {@code stmtLogger} is {@code null}
   */
  public void setStatementLogger(SqlStatementLogger stmtLogger) {
    Assert.notNull(stmtLogger, "SqlStatementLogger is required");
    this.stmtLogger = stmtLogger;
  }

  /**
   * Handle warnings before propagating a primary {@code SQLException}
   * from executing the given statement.
   * <p>Calls regular {@link #handleWarnings(Statement)} but catches
   * {@link SQLWarningException} in order to chain the {@link SQLWarning}
   * into the primary exception instead.
   * Other failures during warning retrieval or processing are logged at debug
   * level so that the primary exception can still be propagated.
   *
   * @param stmt the current JDBC statement
   * @param ex the primary exception after failed statement execution
   * @see #handleWarnings(Statement)
   * @see SQLException#setNextException
   */
  @SuppressWarnings("NullAway")
  protected void handleWarnings(Statement stmt, SQLException ex) {
    try {
      handleWarnings(stmt);
    }
    catch (SQLWarningException nonIgnoredWarning) {
      ex.setNextException(nonIgnoredWarning.getSQLWarning());
    }
    catch (SQLException warningsEx) {
      logger.debug("Failed to retrieve warnings", warningsEx);
    }
    catch (Throwable warningsEx) {
      logger.debug("Failed to process warnings", warningsEx);
    }
  }

  /**
   * Handle the warnings for the given JDBC statement, if any.
   * <p>Throws a {@link SQLWarningException} if we're not ignoring warnings,
   * otherwise logs the warnings at debug level.
   *
   * @param stmt the current JDBC statement
   * @throws SQLException in case of warnings retrieval failure
   * @throws SQLWarningException for a concrete warning to raise
   * (when not ignoring warnings)
   * @see #setIgnoreWarnings
   * @see #handleWarnings(SQLWarning)
   */
  public void handleWarnings(Statement stmt) throws SQLException {
    if (isIgnoreWarnings()) {
      if (logger.isDebugEnabled()) {
        SQLWarning warningToLog = stmt.getWarnings();
        while (warningToLog != null) {
          logger.debug("SQLWarning ignored: SQL state '{}', error code '{}', message [{}]",
                  warningToLog.getSQLState(), warningToLog.getErrorCode(), warningToLog.getMessage());
          warningToLog = warningToLog.getNextWarning();
        }
      }
    }
    else {
      handleWarnings(stmt.getWarnings());
    }
  }

  /**
   * Throw an SQLWarningException if encountering an actual warning.
   *
   * @param warning the warnings object from the current statement.
   * May be {@code null}, in which case this method does nothing.
   * @throws SQLWarningException in case of an actual warning to be raised
   */
  public void handleWarnings(@Nullable SQLWarning warning) throws SQLWarningException {
    if (warning != null) {
      throw new SQLWarningException("Warning not ignored", warning);
    }
  }

  /**
   * Translate the given {@link SQLException} into a {@link DataAccessException}.
   * <p>Uses the configured exception translator, falling back to an
   * {@link UncategorizedSQLException} if the translator cannot classify the failure.
   *
   * @param task readable text describing the task being attempted
   * @param sql the SQL query or update that caused the problem (may be {@code null})
   * @param ex the offending {@code SQLException}
   * @return a DataAccessException wrapping the {@code SQLException} (never {@code null})
   * @see #getExceptionTranslator()
   */
  public DataAccessException translateException(String task, @Nullable String sql, SQLException ex) {
    DataAccessException dae = getExceptionTranslator().translate(task, sql, ex);
    return dae != null ? dae : new UncategorizedSQLException(task, sql, ex);
  }

  /**
   * Determine SQL from potential provider object.
   *
   * @param sqlProvider object which is potentially an SqlProvider
   * @return the SQL string, or {@code null} if not known
   * @see SqlProvider
   */
  protected static @Nullable String getSql(Object sqlProvider) {
    if (sqlProvider instanceof SqlProvider) {
      return ((SqlProvider) sqlProvider).getSql();
    }
    else {
      return null;
    }
  }

}
