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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
import infra.dao.PermissionDeniedDataAccessException;
import infra.dao.TransientDataAccessResourceException;
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
    ERROR_CODES.setPermissionDeniedCodes("11");
    ERROR_CODES.setTransientDataAccessResourceCodes("12");
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
    checkTranslation(11, PermissionDeniedDataAccessException.class);
    checkTranslation(12, TransientDataAccessResourceException.class);

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
  void duplicateKeyCodesDeclaredInUnsortedOrder() {
    SQLErrorCodes errorCodes = new SQLErrorCodes();
    errorCodes.setDuplicateKeyCodes("90002", "1586", "1062");
    SQLErrorCodeSQLExceptionTranslator unsortedCodesTranslator = new SQLErrorCodeSQLExceptionTranslator(errorCodes);

    for (int errorCode : new int[] { 90002, 1586, 1062 }) {
      SQLException sqlException = new SQLException("", "", errorCode);
      assertThat(unsortedCodesTranslator.translate("task", "SQL", sqlException))
              .isInstanceOf(DuplicateKeyException.class)
              .hasCause(sqlException);
    }
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
  void usesPostgresSqlStateFromBatchCauseEvenWithoutVendorCode() {
    var postgres = new SQLErrorCodeSQLExceptionTranslator("PostgreSQL");
    var duplicate = new SQLException("duplicate", "23505");
    var batch = new BatchUpdateException();
    batch.setNextException(duplicate);

    assertThat(postgres.translate("insert", null, batch)).isInstanceOf(DuplicateKeyException.class).hasCause(duplicate);
    assertThat(postgres.translate("insert", null, new SQLException("no state", null, 23505))).isNull();
    assertThat(postgres.translate("insert", null, new SQLException("unmapped", "ZZ999"))).isNull();
  }

  @Test
  void retainsBatchExceptionWhenNextExceptionHasNoTranslationIdentifier() {
    var batch = new BatchUpdateException("invalid result", null, 4, new int[0]);
    batch.setNextException(new SQLException("no state or code"));

    var translated = (InvalidResultSetAccessException) translator.translate("read", null, batch);

    assertThat(translated.getSql()).isEmpty();
    assertThat(translated).hasCause(batch);
  }

  @Test
  void fallsBackToSqlStateWhenVendorCodesAreNotConfigured() {
    var fallback = new SQLErrorCodeSQLExceptionTranslator();
    var syntaxError = new SQLException("syntax error", "42000");

    assertThat(fallback.getSqlErrorCodes()).isNull();
    assertThat(fallback.translate("query", null, syntaxError)).isInstanceOf(BadSqlGrammarException.class)
            .hasCause(syntaxError);
  }

  @Test
  void customTranslatorTakesPrecedenceButCanDeferToVendorCodes() {
    var codes = new SQLErrorCodes();
    codes.setBadSqlGrammarCodes("1");
    var custom = mock(SQLExceptionTranslator.class);
    codes.setCustomSqlExceptionTranslator(custom);
    var sqlError = new SQLException("bad SQL", null, 1);
    var customResult = new DataAccessResourceFailureException("custom", sqlError);
    given(custom.translate("query", "SQL", sqlError)).willReturn(customResult).willReturn(null);
    var candidate = new SQLErrorCodeSQLExceptionTranslator(codes);

    assertThat(candidate.translate("query", "SQL", sqlError)).isSameAs(customResult);
    assertThat(candidate.translate("query", "SQL", sqlError)).isInstanceOf(BadSqlGrammarException.class).hasCause(sqlError);
  }

  @ParameterizedTest
  @ValueSource(classes = { InvalidResultSetAccessException.class, CustomTaskSqlException.class })
  void passesTaskSqlAndCauseToCustomException(Class<? extends DataAccessException> exceptionClass) {
    var candidate = translatorWithCustomException(exceptionClass);
    var cause = new SQLException("bad SQL", null, 1);

    assertThat(candidate.translate("query", "SELECT id", cause)).isExactlyInstanceOf(exceptionClass)
            .hasMessageContaining("query").hasMessageContaining("SELECT id").hasCause(cause);
  }

  @Test
  void fallsBackToVendorCodesWhenCustomExceptionCannotBeConstructed() {
    var candidate = translatorWithCustomException(UnconstructableException.class);
    var cause = new SQLException("bad SQL", null, 1);

    assertThat(candidate.translate("query", null, cause)).isInstanceOf(BadSqlGrammarException.class).hasCause(cause);
  }

  private static SQLErrorCodeSQLExceptionTranslator translatorWithCustomException(
          Class<? extends DataAccessException> exceptionClass) {
    var codes = new SQLErrorCodes();
    codes.setBadSqlGrammarCodes("1");
    var mapping = new CustomSQLErrorCodesTranslation();
    mapping.setErrorCodes("1");
    mapping.setExceptionClass(exceptionClass);
    codes.setCustomTranslations(mapping);
    return new SQLErrorCodeSQLExceptionTranslator(codes);
  }

  @SuppressWarnings("serial")
  public static class CustomTaskSqlException extends DataAccessException {

    public CustomTaskSqlException(String task, String sql, Throwable cause) {
      super(task + ": " + sql, cause);
    }
  }

  @SuppressWarnings("serial")
  public static class UnconstructableException extends DataAccessException {

    public UnconstructableException(String message) {
      super(message);
      throw new IllegalStateException("constructor failed");
    }
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
