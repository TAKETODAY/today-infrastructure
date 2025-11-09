/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc.support;

import org.jspecify.annotations.Nullable;

import java.sql.BatchUpdateException;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLTransientException;

import infra.dao.CannotAcquireLockException;
import infra.dao.DataAccessException;
import infra.dao.DataAccessResourceFailureException;
import infra.dao.DataIntegrityViolationException;
import infra.dao.DuplicateKeyException;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.dao.PermissionDeniedDataAccessException;
import infra.dao.PessimisticLockingFailureException;
import infra.dao.QueryTimeoutException;
import infra.dao.RecoverableDataAccessException;
import infra.dao.TransientDataAccessResourceException;
import infra.jdbc.BadSqlGrammarException;

/**
 * {@link SQLExceptionTranslator} implementation which analyzes the specific
 * {@link SQLException} subclass thrown by the JDBC driver.
 *
 * <p>Falls back to a standard {@link SQLStateSQLExceptionTranslator} if the JDBC
 * driver does not actually expose JDBC 4 compliant {@code SQLException} subclasses.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see SQLTransientException
 * @see SQLTransientException
 * @see SQLRecoverableException
 * @since 4.0
 */
public class SQLExceptionSubclassTranslator extends AbstractFallbackSQLExceptionTranslator {

  public SQLExceptionSubclassTranslator() {
    setFallbackTranslator(new SQLStateSQLExceptionTranslator());
  }

  @Override
  protected @Nullable DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
    SQLException sqlEx = ex;
    if (sqlEx instanceof BatchUpdateException && sqlEx.getNextException() != null) {
      sqlEx = sqlEx.getNextException();
    }

    if (sqlEx instanceof SQLTransientException) {
      if (sqlEx instanceof SQLTransientConnectionException) {
        return new TransientDataAccessResourceException(buildMessage(task, sql, sqlEx), ex);
      }
      if (sqlEx instanceof SQLTransactionRollbackException) {
        if (SQLStateSQLExceptionTranslator.indicatesCannotAcquireLock(sqlEx.getSQLState())) {
          return new CannotAcquireLockException(buildMessage(task, sql, sqlEx), ex);
        }
        return new PessimisticLockingFailureException(buildMessage(task, sql, sqlEx), ex);
      }
      if (sqlEx instanceof SQLTimeoutException) {
        return new QueryTimeoutException(buildMessage(task, sql, sqlEx), ex);
      }
    }
    else if (sqlEx instanceof SQLNonTransientException) {
      if (sqlEx instanceof SQLNonTransientConnectionException) {
        return new DataAccessResourceFailureException(buildMessage(task, sql, sqlEx), ex);
      }
      if (sqlEx instanceof SQLDataException) {
        return new DataIntegrityViolationException(buildMessage(task, sql, sqlEx), ex);
      }
      if (sqlEx instanceof SQLIntegrityConstraintViolationException) {
        if (SQLStateSQLExceptionTranslator.indicatesDuplicateKey(sqlEx.getSQLState(), sqlEx.getErrorCode())) {
          return new DuplicateKeyException(buildMessage(task, sql, sqlEx), ex);
        }
        return new DataIntegrityViolationException(buildMessage(task, sql, sqlEx), ex);
      }
      if (sqlEx instanceof SQLInvalidAuthorizationSpecException) {
        return new PermissionDeniedDataAccessException(buildMessage(task, sql, sqlEx), ex);
      }
      if (sqlEx instanceof SQLSyntaxErrorException) {
        return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
      }
      if (sqlEx instanceof SQLFeatureNotSupportedException) {
        return new InvalidDataAccessApiUsageException(buildMessage(task, sql, sqlEx), ex);
      }
    }
    else if (sqlEx instanceof SQLRecoverableException) {
      return new RecoverableDataAccessException(buildMessage(task, sql, sqlEx), ex);
    }

    // Fallback to Infra own SQL state translation...
    return null;
  }

}
