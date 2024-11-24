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

import org.junit.jupiter.api.Test;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thomas Risberg
 */
public class SQLExceptionSubclassTranslatorTests {

  @Test
  public void exceptionClassTranslation() {
    doTest(new SQLDataException("", "", 0), DataIntegrityViolationException.class);
    doTest(new SQLFeatureNotSupportedException("", "", 0), InvalidDataAccessApiUsageException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "", 0), DataIntegrityViolationException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23505", 0), DuplicateKeyException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23000", 1), DuplicateKeyException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23000", 1062), DuplicateKeyException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23000", 2601), DuplicateKeyException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23000", 2627), DuplicateKeyException.class);
    doTest(new SQLInvalidAuthorizationSpecException("", "", 0), PermissionDeniedDataAccessException.class);
    doTest(new SQLNonTransientConnectionException("", "", 0), DataAccessResourceFailureException.class);
    doTest(new SQLRecoverableException("", "", 0), RecoverableDataAccessException.class);
    doTest(new SQLSyntaxErrorException("", "", 0), BadSqlGrammarException.class);
    doTest(new SQLTimeoutException("", "", 0), QueryTimeoutException.class);
    doTest(new SQLTransactionRollbackException("", "", 0), PessimisticLockingFailureException.class);
    doTest(new SQLTransactionRollbackException("", "40001", 0), CannotAcquireLockException.class);
    doTest(new SQLTransientConnectionException("", "", 0), TransientDataAccessResourceException.class);
  }

  @Test
  public void fallbackStateTranslation() {
    // Test fallback. We assume that no database will ever return this error code,
    // but 07xxx will be bad grammar picked up by the fallback SQLState translator
    doTest(new SQLException("", "07xxx", 666666666), BadSqlGrammarException.class);
    // and 08xxx will be data resource failure (non-transient) picked up by the fallback SQLState translator
    doTest(new SQLException("", "08xxx", 666666666), DataAccessResourceFailureException.class);
  }

  private void doTest(SQLException ex, Class<?> dataAccessExceptionType) {
    SQLExceptionTranslator translator = new SQLExceptionSubclassTranslator();
    DataAccessException dax = translator.translate("task", "SQL", ex);

    assertThat(dax).as("Specific translation must not result in null").isNotNull();
    assertThat(dax).as("Wrong DataAccessException type returned").isExactlyInstanceOf(dataAccessExceptionType);
    assertThat(dax.getCause()).as("The exact same original SQLException must be preserved").isSameAs(ex);
  }

}
