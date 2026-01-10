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
