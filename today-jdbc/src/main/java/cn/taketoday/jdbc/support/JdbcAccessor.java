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

package cn.taketoday.jdbc.support;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.SQLWarningException;
import cn.taketoday.jdbc.UncategorizedSQLException;
import cn.taketoday.jdbc.core.SqlProvider;
import cn.taketoday.jdbc.format.SqlStatementLogger;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Base class for {@link cn.taketoday.jdbc.core.JdbcTemplate} and
 * other JDBC-accessing DAO helpers, defining common properties such as
 * DataSource and exception translator.
 *
 * <p>Not intended to be used directly.
 * See {@link cn.taketoday.jdbc.core.JdbcTemplate}.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.jdbc.core.JdbcTemplate
 * @since 4.0
 */
public abstract class JdbcAccessor {
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  protected SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  @Nullable
  private DataSource dataSource;

  @Nullable
  private volatile SQLExceptionTranslator exceptionTranslator;

  /** If this variable is false, we will throw exceptions on SQL warnings. */
  private boolean ignoreWarnings = true;

  /**
   * Set the JDBC DataSource to obtain connections from.
   */
  public void setDataSource(@Nullable DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Return the DataSource used by this template.
   */
  @Nullable
  public DataSource getDataSource() {
    return this.dataSource;
  }

  /**
   * Obtain the DataSource for actual use.
   *
   * @return the DataSource (never {@code null})
   * @throws IllegalStateException in case of no DataSource set
   */
  protected DataSource obtainDataSource() {
    DataSource dataSource = getDataSource();
    Assert.state(dataSource != null, "No DataSource set");
    return dataSource;
  }

  /**
   * Specify the database product name for the DataSource that this accessor uses.
   * This allows to initialize an SQLErrorCodeSQLExceptionTranslator without
   * obtaining a Connection from the DataSource to get the meta-data.
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
   * Set the exception translator for this instance.
   * <p>If no custom translator is provided, a default
   * {@link SQLErrorCodeSQLExceptionTranslator} is used
   * which examines the SQLException's vendor-specific error code.
   *
   * @see cn.taketoday.jdbc.support.SQLErrorCodeSQLExceptionTranslator
   * @see cn.taketoday.jdbc.support.SQLStateSQLExceptionTranslator
   */
  public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
    this.exceptionTranslator = exceptionTranslator;
  }

  /**
   * Return the exception translator for this instance.
   * <p>Creates a default {@link SQLErrorCodeSQLExceptionTranslator}
   * for the specified DataSource if none set, or a
   * {@link SQLStateSQLExceptionTranslator} in case of no DataSource.
   *
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
          exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(obtainDataSource());
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
   * Set whether or not we want to ignore SQLWarnings.
   * <p>Default is "true", swallowing and logging all warnings. Switch this flag
   * to "false" to make the JdbcTemplate throw an SQLWarningException instead.
   *
   * @see SQLWarning
   * @see cn.taketoday.jdbc.SQLWarningException
   * @see #handleWarnings
   */
  public void setIgnoreWarnings(boolean ignoreWarnings) {
    this.ignoreWarnings = ignoreWarnings;
  }

  /**
   * Return whether or not we ignore SQLWarnings.
   */
  public boolean isIgnoreWarnings() {
    return this.ignoreWarnings;
  }

  public void setStatementLogger(SqlStatementLogger statementLogger) {
    Assert.notNull(statementLogger, "statementLogger is required");
    this.stmtLogger = statementLogger;
  }

  public SqlStatementLogger getStatementLogger() {
    return stmtLogger;
  }

  /**
   * Throw an SQLWarningException if we're not ignoring warnings,
   * otherwise log the warnings at debug level.
   *
   * @param stmt the current JDBC statement
   * @throws SQLWarningException if not ignoring warnings
   * @see cn.taketoday.jdbc.SQLWarningException
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
   * Translate the given {@link SQLException} into a generic {@link DataAccessException}.
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
  @Nullable
  protected static String getSql(Object sqlProvider) {
    if (sqlProvider instanceof SqlProvider) {
      return ((SqlProvider) sqlProvider).getSql();
    }
    else {
      return null;
    }
  }

}
