/*
 * Copyright 2017 - 2024 the original author or authors.
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
import infra.lang.Nullable;

/**
 * {@link SQLExceptionTranslator} implementation which analyzes the specific
 * {@link SQLException} subclass thrown by the JDBC driver.
 *
 * <p>Falls back to a standard {@link SQLStateSQLExceptionTranslator} if the JDBC
 * driver does not actually expose JDBC 4 compliant {@code SQLException} subclasses.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
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
  @Nullable
  protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
    if (ex instanceof SQLTransientException) {
      if (ex instanceof SQLTransientConnectionException) {
        return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLTransactionRollbackException) {
        if ("40001".equals(ex.getSQLState())) {
          return new CannotAcquireLockException(buildMessage(task, sql, ex), ex);
        }
        return new PessimisticLockingFailureException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLTimeoutException) {
        return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
      }
    }
    else if (ex instanceof SQLNonTransientException) {
      if (ex instanceof SQLNonTransientConnectionException) {
        return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLDataException) {
        return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLIntegrityConstraintViolationException) {
        if (SQLStateSQLExceptionTranslator.indicatesDuplicateKey(ex.getSQLState(), ex.getErrorCode())) {
          return new DuplicateKeyException(buildMessage(task, sql, ex), ex);
        }
        return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLInvalidAuthorizationSpecException) {
        return new PermissionDeniedDataAccessException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLSyntaxErrorException) {
        return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
      }
      if (ex instanceof SQLFeatureNotSupportedException) {
        return new InvalidDataAccessApiUsageException(buildMessage(task, sql, ex), ex);
      }
    }
    else if (ex instanceof SQLRecoverableException) {
      return new RecoverableDataAccessException(buildMessage(task, sql, ex), ex);
    }

    // Fallback to Infra own SQL state translation...
    return null;
  }

}
