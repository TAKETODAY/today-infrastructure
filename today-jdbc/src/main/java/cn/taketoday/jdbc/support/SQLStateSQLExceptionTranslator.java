/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.util.Set;

import cn.taketoday.dao.CannotAcquireLockException;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.DuplicateKeyException;
import cn.taketoday.dao.PessimisticLockingFailureException;
import cn.taketoday.dao.QueryTimeoutException;
import cn.taketoday.dao.TransientDataAccessResourceException;
import cn.taketoday.jdbc.BadSqlGrammarException;
import cn.taketoday.lang.Nullable;

/**
 * {@link SQLExceptionTranslator} implementation that analyzes the SQL state in
 * the {@link SQLException} based on the first two digits (the SQL state "class").
 * Detects standard SQL state values and well-known vendor-specific SQL states.
 *
 * <p>Not able to diagnose all problems, but is portable between databases and
 * does not require special initialization (no database vendor detection, etc.).
 * For more precise translation, consider {@link SQLErrorCodeSQLExceptionTranslator}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.sql.SQLException#getSQLState()
 * @see SQLErrorCodeSQLExceptionTranslator
 * @since 4.0
 */
public class SQLStateSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

  private static final Set<String> BAD_SQL_GRAMMAR_CODES = Set.of(
          "07",  // Dynamic SQL error
          "21",  // Cardinality violation
          "2A",  // Syntax error direct SQL
          "37",  // Syntax error dynamic SQL
          "42",  // General SQL syntax error
          "65"   // Oracle: unknown identifier
  );

  private static final Set<String> DATA_INTEGRITY_VIOLATION_CODES = Set.of(
          "01",  // Data truncation
          "02",  // No data found
          "22",  // Value out of range
          "23",  // Integrity constraint violation
          "27",  // Triggered data change violation
          "44"   // With check violation
  );

  private static final Set<String> DATA_ACCESS_RESOURCE_FAILURE_CODES = Set.of(
          "08",  // Connection exception
          "53",  // PostgreSQL: insufficient resources (e.g. disk full)
          "54",  // PostgreSQL: program limit exceeded (e.g. statement too complex)
          "57",  // DB2: out-of-memory exception / database not started
          "58"   // DB2: unexpected system error
  );

  private static final Set<String> TRANSIENT_DATA_ACCESS_RESOURCE_CODES = Set.of(
          "JW",  // Sybase: internal I/O error
          "JZ",  // Sybase: unexpected I/O error
          "S1"   // DB2: communication failure
  );

  private static final Set<String> PESSIMISTIC_LOCKING_FAILURE_CODES = Set.of(
          "40",  // Transaction rollback
          "61"   // Oracle: deadlock
  );

  private static final Set<Integer> DUPLICATE_KEY_ERROR_CODES = Set.of(
          1,     // Oracle
          301,   // SAP HANA
          1062,  // MySQL/MariaDB
          2601,  // MS SQL Server
          2627   // MS SQL Server
  );

  @Override
  @Nullable
  protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
    // First, the getSQLState check...
    String sqlState = getSqlState(ex);
    if (sqlState != null && sqlState.length() >= 2) {
      String classCode = sqlState.substring(0, 2);
      if (logger.isDebugEnabled()) {
        logger.debug("Extracted SQL state class '" + classCode + "' from value '" + sqlState + "'");
      }
      if (BAD_SQL_GRAMMAR_CODES.contains(classCode)) {
        return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
      }
      else if (DATA_INTEGRITY_VIOLATION_CODES.contains(classCode)) {
        if (indicatesDuplicateKey(sqlState, ex.getErrorCode())) {
          return new DuplicateKeyException(buildMessage(task, sql, ex), ex);
        }
        return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
      }
      else if (DATA_ACCESS_RESOURCE_FAILURE_CODES.contains(classCode)) {
        return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
      }
      else if (TRANSIENT_DATA_ACCESS_RESOURCE_CODES.contains(classCode)) {
        return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
      }
      else if (PESSIMISTIC_LOCKING_FAILURE_CODES.contains(classCode)) {
        if (indicatesCannotAcquireLock(sqlState)) {
          return new CannotAcquireLockException(buildMessage(task, sql, ex), ex);
        }
        return new PessimisticLockingFailureException(buildMessage(task, sql, ex), ex);
      }
    }

    // For MySQL: exception class name indicating a timeout?
    // (since MySQL doesn't throw the JDBC 4 SQLTimeoutException)
    if (ex.getClass().getName().contains("Timeout")) {
      return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
    }

    // Couldn't resolve anything proper - resort to UncategorizedSQLException.
    return null;
  }

  /**
   * Gets the SQL state code from the supplied {@link SQLException exception}.
   * <p>Some JDBC drivers nest the actual exception from a batched update, so we
   * might need to dig down into the nested exception.
   *
   * @param ex the exception from which the {@link SQLException#getSQLState() SQL state}
   * is to be extracted
   * @return the SQL state code
   */
  @Nullable
  private String getSqlState(SQLException ex) {
    String sqlState = ex.getSQLState();
    if (sqlState == null) {
      SQLException nestedEx = ex.getNextException();
      if (nestedEx != null) {
        sqlState = nestedEx.getSQLState();
      }
    }
    return sqlState;
  }

  /**
   * Check whether the given SQL state and the associated error code (in case
   * of a generic SQL state value) indicate a {@link DuplicateKeyException}:
   * either SQL state 23505 as a specific indication, or the generic SQL state
   * 23000 with a well-known vendor code.
   *
   * @param sqlState the SQL state value
   * @param errorCode the error code
   */
  static boolean indicatesDuplicateKey(@Nullable String sqlState, int errorCode) {
    return ("23505".equals(sqlState) ||
            ("23000".equals(sqlState) && DUPLICATE_KEY_ERROR_CODES.contains(errorCode)));
  }

  /**
   * Check whether the given SQL state indicates a {@link CannotAcquireLockException},
   * with SQL state 40001 as a specific indication.
   *
   * @param sqlState the SQL state value
   */
  static boolean indicatesCannotAcquireLock(@Nullable String sqlState) {
    return "40001".equals(sqlState);
  }

}
