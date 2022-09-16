/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.dao.ConcurrencyFailureException;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
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
 * @see java.sql.SQLException#getSQLState()
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class SQLStateSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

  private static final Set<String> BAD_SQL_GRAMMAR_CODES = new HashSet<>(8);
  private static final Set<String> CONCURRENCY_FAILURE_CODES = Set.of(
          "40", // Transaction rollback
          "61" // Oracle: deadlock
  );

  private static final Set<String> DATA_INTEGRITY_VIOLATION_CODES = new HashSet<>(8);
  private static final Set<String> DATA_ACCESS_RESOURCE_FAILURE_CODES = new HashSet<>(8);
  private static final Set<String> TRANSIENT_DATA_ACCESS_RESOURCE_CODES = Set.of(
          "JW",  // Sybase: internal I/O error
          "JZ",  // Sybase: unexpected I/O error
          "S1"  // DB2: communication failure
  );

  static {
    BAD_SQL_GRAMMAR_CODES.add("07");  // Dynamic SQL error
    BAD_SQL_GRAMMAR_CODES.add("21");  // Cardinality violation
    BAD_SQL_GRAMMAR_CODES.add("2A");  // Syntax error direct SQL
    BAD_SQL_GRAMMAR_CODES.add("37");  // Syntax error dynamic SQL
    BAD_SQL_GRAMMAR_CODES.add("42");  // General SQL syntax error
    BAD_SQL_GRAMMAR_CODES.add("65");  // Oracle: unknown identifier

    DATA_INTEGRITY_VIOLATION_CODES.add("01");  // Data truncation
    DATA_INTEGRITY_VIOLATION_CODES.add("02");  // No data found
    DATA_INTEGRITY_VIOLATION_CODES.add("22");  // Value out of range
    DATA_INTEGRITY_VIOLATION_CODES.add("23");  // Integrity constraint violation
    DATA_INTEGRITY_VIOLATION_CODES.add("27");  // Triggered data change violation
    DATA_INTEGRITY_VIOLATION_CODES.add("44");  // With check violation

    DATA_ACCESS_RESOURCE_FAILURE_CODES.add("08");  // Connection exception
    DATA_ACCESS_RESOURCE_FAILURE_CODES.add("53");  // PostgreSQL: insufficient resources (e.g. disk full)
    DATA_ACCESS_RESOURCE_FAILURE_CODES.add("54");  // PostgreSQL: program limit exceeded (e.g. statement too complex)
    DATA_ACCESS_RESOURCE_FAILURE_CODES.add("57");  // DB2: out-of-memory exception / database not started
    DATA_ACCESS_RESOURCE_FAILURE_CODES.add("58");  // DB2: unexpected system error
  }

  @Override
  @Nullable
  protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
    // First, the getSQLState check...
    String sqlState = getSqlState(ex);
    if (sqlState != null && sqlState.length() >= 2) {
      String classCode = sqlState.substring(0, 2);
      if (logger.isDebugEnabled()) {
        logger.debug("Extracted SQL state class '{}' from value '{}'", classCode, sqlState);
      }
      if (BAD_SQL_GRAMMAR_CODES.contains(classCode)) {
        return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
      }
      else if (DATA_INTEGRITY_VIOLATION_CODES.contains(classCode)) {
        return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
      }
      else if (DATA_ACCESS_RESOURCE_FAILURE_CODES.contains(classCode)) {
        return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
      }
      else if (TRANSIENT_DATA_ACCESS_RESOURCE_CODES.contains(classCode)) {
        return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
      }
      else if (CONCURRENCY_FAILURE_CODES.contains(classCode)) {
        return new ConcurrencyFailureException(buildMessage(task, sql, ex), ex);
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

}
