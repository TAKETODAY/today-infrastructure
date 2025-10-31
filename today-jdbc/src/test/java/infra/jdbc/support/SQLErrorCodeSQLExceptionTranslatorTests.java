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
import org.junit.jupiter.api.Test;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import infra.dao.CannotAcquireLockException;
import infra.dao.CannotSerializeTransactionException;
import infra.dao.DataAccessException;
import infra.dao.DataAccessResourceFailureException;
import infra.dao.DataIntegrityViolationException;
import infra.dao.DeadlockLoserDataAccessException;
import infra.dao.DuplicateKeyException;
import infra.jdbc.BadSqlGrammarException;
import infra.jdbc.InvalidResultSetAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class SQLErrorCodeSQLExceptionTranslatorTests {

  private static final SQLErrorCodes ERROR_CODES = new SQLErrorCodes();

  static {
    ERROR_CODES.setBadSqlGrammarCodes("1", "2");
    ERROR_CODES.setInvalidResultSetAccessCodes("3", "4");
    ERROR_CODES.setDuplicateKeyCodes("10");
    ERROR_CODES.setDataAccessResourceFailureCodes("5");
    ERROR_CODES.setDataIntegrityViolationCodes("6");
    ERROR_CODES.setCannotAcquireLockCodes("7");
    ERROR_CODES.setDeadlockLoserCodes("8");
    ERROR_CODES.setCannotSerializeTransactionCodes("9");
  }

  private SQLErrorCodeSQLExceptionTranslator translator = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

  @Test
  void errorCodeTranslation() {
    SQLException badSqlEx = new SQLException("", "", 1);
    BadSqlGrammarException bsgEx = (BadSqlGrammarException) translator.translate("task", "SQL", badSqlEx);
    assertThat(bsgEx.getSql()).isEqualTo("SQL");
    assertThat((Object) bsgEx.getSQLException()).isEqualTo(badSqlEx);

    SQLException cause = new SQLException("", "", 4);
    InvalidResultSetAccessException invResEx = (InvalidResultSetAccessException) translator.translate("task", "SQL", cause);
    assertThat(invResEx.getSql()).isEqualTo("SQL");
    assertThat((Object) invResEx.getSQLException()).isEqualTo(cause);

    checkTranslation(5, DataAccessResourceFailureException.class);
    checkTranslation(6, DataIntegrityViolationException.class);
    checkTranslation(7, CannotAcquireLockException.class);
    checkTranslation(8, DeadlockLoserDataAccessException.class);
    checkTranslation(9, CannotSerializeTransactionException.class);
    checkTranslation(10, DuplicateKeyException.class);

    SQLException dupKeyEx = new SQLException("", "", 10);
    DataAccessException dataAccessException = translator.translate("task", "SQL", dupKeyEx);
    assertThat(dataAccessException)
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasCause(dupKeyEx);

    // Test fallback. We assume that no database will ever return this error code,
    // but 07xxx will be bad grammar picked up by the fallback SQLState translator
    cause = new SQLException("", "07xxx", 666666666);
    bsgEx = (BadSqlGrammarException) translator.translate("task", "SQL2", cause);
    assertThat(bsgEx.getSql()).isEqualTo("SQL2");
    assertThat((Object) bsgEx.getSQLException()).isEqualTo(cause);
  }

  private void checkTranslation(int errorCode, Class<? extends Exception> expectedType) {
    SQLException sqlException = new SQLException("", "", errorCode);
    DataAccessException dataAccessException = this.translator.translate("", "", sqlException);
    assertThat(dataAccessException)
            .isInstanceOf(expectedType)
            .hasCause(sqlException);
  }

  @Test
  void batchExceptionTranslation() {
    SQLException badSqlEx = new SQLException("", "", 1);
    BatchUpdateException batchUpdateEx = new BatchUpdateException();
    batchUpdateEx.setNextException(badSqlEx);
    BadSqlGrammarException bsgEx = (BadSqlGrammarException) translator.translate("task", "SQL", batchUpdateEx);
    assertThat(bsgEx.getSql()).isEqualTo("SQL");
    assertThat((Object) bsgEx.getSQLException()).isEqualTo(badSqlEx);
  }

  @Test
  void dataTruncationTranslation() {
    SQLException dataAccessEx = new SQLException("", "", 5);
    DataTruncation dataTruncation = new DataTruncation(1, true, true, 1, 1, dataAccessEx);
    DataAccessException dataAccessException = translator.translate("task", "SQL", dataTruncation);
    assertThat(dataAccessException)
            .isInstanceOf(DataAccessResourceFailureException.class)
            .hasCause(dataTruncation);
  }

  @Test
  @SuppressWarnings("serial")
  void customTranslateMethodTranslation() {
    String TASK = "TASK";
    String SQL = "SQL SELECT *";
    DataAccessException customDex = new DataAccessException("") { };

    SQLException badSqlEx = new SQLException("", "", 1);
    SQLException integrityViolationEx = new SQLException("", "", 6);

    translator = new SQLErrorCodeSQLExceptionTranslator() {
      @SuppressWarnings("deprecation")
      @Override
      protected @Nullable DataAccessException customTranslate(String task, @Nullable String sql, SQLException sqlException) {
        assertThat(task).isEqualTo(TASK);
        assertThat(sql).isEqualTo(SQL);
        return (sqlException == badSqlEx) ? customDex : null;
      }
    };
    translator.setSqlErrorCodes(ERROR_CODES);

    // Should custom translate this
    assertThat(translator.translate(TASK, SQL, badSqlEx)).isEqualTo(customDex);

    // Shouldn't custom translate this
    assertThat(translator.translate(TASK, SQL, integrityViolationEx))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasCause(integrityViolationEx);
  }

  @Test
  void customExceptionTranslation() {
    String TASK = "TASK";
    String SQL = "SQL SELECT *";
    SQLErrorCodes customErrorCodes = new SQLErrorCodes();
    CustomSQLErrorCodesTranslation customTranslation = new CustomSQLErrorCodesTranslation();

    customErrorCodes.setBadSqlGrammarCodes("1", "2");
    customErrorCodes.setDataIntegrityViolationCodes("3", "4");
    customTranslation.setErrorCodes("1");
    customTranslation.setExceptionClass(CustomErrorCodeException.class);
    customErrorCodes.setCustomTranslations(customTranslation);

    translator = new SQLErrorCodeSQLExceptionTranslator(customErrorCodes);

    // Should custom translate this
    SQLException badSqlEx = new SQLException("", "", 1);
    assertThat(translator.translate(TASK, SQL, badSqlEx))
            .isInstanceOf(CustomErrorCodeException.class)
            .hasCause(badSqlEx);

    // Shouldn't custom translate this
    SQLException invResEx = new SQLException("", "", 3);
    assertThat(translator.translate(TASK, SQL, invResEx))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasCause(invResEx);

    // Shouldn't custom translate this - invalid class
    assertThatIllegalArgumentException().isThrownBy(() -> customTranslation.setExceptionClass(String.class));
  }

  @Test
  void dataSourceInitializationWhenConnectionCannotBeObtained() throws Exception {
    SQLException connectionException = new SQLException();
    SQLException duplicateKeyException = new SQLException("test", "", 1);

    DataSource dataSource = mock();
    given(dataSource.getConnection()).willThrow(connectionException);

    translator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    assertThat(translator.translate("test", null, duplicateKeyException)).isNull();
  }

  @Test
  void dataSourceInitialization() throws Exception {
    SQLException duplicateKeyException = new SQLException("test", "", 1);

    DatabaseMetaData databaseMetaData = mock();
    given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");

    Connection connection = mock();
    given(connection.getMetaData()).willReturn(databaseMetaData);

    DataSource dataSource = mock();
    given(dataSource.getConnection()).willReturn(connection);

    translator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    assertThat(translator.translate("test", null, duplicateKeyException))
            .isInstanceOf(DuplicateKeyException.class);

    verify(connection).close();
  }

}
