/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import cn.taketoday.dao.CannotAcquireLockException;
import cn.taketoday.dao.CannotSerializeTransactionException;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.DeadlockLoserDataAccessException;
import cn.taketoday.dao.DuplicateKeyException;
import cn.taketoday.jdbc.BadSqlGrammarException;
import cn.taketoday.jdbc.InvalidResultSetAccessException;
import cn.taketoday.lang.Nullable;

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
public class SQLErrorCodeSQLExceptionTranslatorTests {

  private static SQLErrorCodes ERROR_CODES = new SQLErrorCodes();

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

  @Test
  public void errorCodeTranslation() {
    SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

    SQLException badSqlEx = new SQLException("", "", 1);
    BadSqlGrammarException bsgex = (BadSqlGrammarException) sext.translate("task", "SQL", badSqlEx);
    assertThat(bsgex.getSql()).isEqualTo("SQL");
    assertThat((Object) bsgex.getSQLException()).isEqualTo(badSqlEx);

    SQLException invResEx = new SQLException("", "", 4);
    InvalidResultSetAccessException irsex = (InvalidResultSetAccessException) sext.translate("task", "SQL", invResEx);
    assertThat(irsex.getSql()).isEqualTo("SQL");
    assertThat((Object) irsex.getSQLException()).isEqualTo(invResEx);

    checkTranslation(sext, 5, DataAccessResourceFailureException.class);
    checkTranslation(sext, 6, DataIntegrityViolationException.class);
    checkTranslation(sext, 7, CannotAcquireLockException.class);
    checkTranslation(sext, 8, DeadlockLoserDataAccessException.class);
    checkTranslation(sext, 9, CannotSerializeTransactionException.class);
    checkTranslation(sext, 10, DuplicateKeyException.class);

    SQLException dupKeyEx = new SQLException("", "", 10);
    DataAccessException dksex = sext.translate("task", "SQL", dupKeyEx);
    assertThat(DataIntegrityViolationException.class.isInstance(dksex)).as("Not instance of DataIntegrityViolationException").isTrue();

    // Test fallback. We assume that no database will ever return this error code,
    // but 07xxx will be bad grammar picked up by the fallback SQLState translator
    SQLException sex = new SQLException("", "07xxx", 666666666);
    BadSqlGrammarException bsgex2 = (BadSqlGrammarException) sext.translate("task", "SQL2", sex);
    assertThat(bsgex2.getSql()).isEqualTo("SQL2");
    assertThat((Object) bsgex2.getSQLException()).isEqualTo(sex);
  }

  private void checkTranslation(SQLExceptionTranslator sext, int errorCode, Class<?> exClass) {
    SQLException sex = new SQLException("", "", errorCode);
    DataAccessException ex = sext.translate("", "", sex);
    assertThat(exClass.isInstance(ex)).isTrue();
    assertThat(ex.getCause() == sex).isTrue();
  }

  @Test
  public void batchExceptionTranslation() {
    SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

    SQLException badSqlEx = new SQLException("", "", 1);
    BatchUpdateException batchUpdateEx = new BatchUpdateException();
    batchUpdateEx.setNextException(badSqlEx);
    BadSqlGrammarException bsgex = (BadSqlGrammarException) sext.translate("task", "SQL", batchUpdateEx);
    assertThat(bsgex.getSql()).isEqualTo("SQL");
    assertThat((Object) bsgex.getSQLException()).isEqualTo(badSqlEx);
  }

  @Test
  public void dataTruncationTranslation() {
    SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

    SQLException dataAccessEx = new SQLException("", "", 5);
    DataTruncation dataTruncation = new DataTruncation(1, true, true, 1, 1, dataAccessEx);
    DataAccessResourceFailureException daex = (DataAccessResourceFailureException) sext.translate("task", "SQL", dataTruncation);
    assertThat(daex.getCause()).isEqualTo(dataTruncation);
  }

  @SuppressWarnings("serial")
  @Test
  public void customTranslateMethodTranslation() {
    final String TASK = "TASK";
    final String SQL = "SQL SELECT *";
    final DataAccessException customDex = new DataAccessException("") { };

    final SQLException badSqlEx = new SQLException("", "", 1);
    SQLException intVioEx = new SQLException("", "", 6);

    SQLErrorCodeSQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator() {
      @Override
      @Nullable
      protected DataAccessException customTranslate(String task, @Nullable String sql, SQLException sqlex) {
        assertThat(task).isEqualTo(TASK);
        assertThat(sql).isEqualTo(SQL);
        return (sqlex == badSqlEx) ? customDex : null;
      }
    };
    sext.setSqlErrorCodes(ERROR_CODES);

    // Shouldn't custom translate this
    assertThat(sext.translate(TASK, SQL, badSqlEx)).isEqualTo(customDex);
    DataIntegrityViolationException diex = (DataIntegrityViolationException) sext.translate(TASK, SQL, intVioEx);
    assertThat(diex.getCause()).isEqualTo(intVioEx);
  }

  @Test
  public void customExceptionTranslation() {
    final String TASK = "TASK";
    final String SQL = "SQL SELECT *";
    final SQLErrorCodes customErrorCodes = new SQLErrorCodes();
    final CustomSQLErrorCodesTranslation customTranslation = new CustomSQLErrorCodesTranslation();

    customErrorCodes.setBadSqlGrammarCodes("1", "2");
    customErrorCodes.setDataIntegrityViolationCodes("3", "4");
    customTranslation.setErrorCodes("1");
    customTranslation.setExceptionClass(CustomErrorCodeException.class);
    customErrorCodes.setCustomTranslations(customTranslation);

    SQLErrorCodeSQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(customErrorCodes);

    // Should custom translate this
    SQLException badSqlEx = new SQLException("", "", 1);
    assertThat(sext.translate(TASK, SQL, badSqlEx).getClass()).isEqualTo(CustomErrorCodeException.class);
    assertThat(sext.translate(TASK, SQL, badSqlEx).getCause()).isEqualTo(badSqlEx);

    // Shouldn't custom translate this
    SQLException invResEx = new SQLException("", "", 3);
    DataIntegrityViolationException diex = (DataIntegrityViolationException) sext.translate(TASK, SQL, invResEx);
    assertThat(diex.getCause()).isEqualTo(invResEx);

    // Shouldn't custom translate this - invalid class
    assertThatIllegalArgumentException().isThrownBy(() ->
            customTranslation.setExceptionClass(String.class));
  }

  @Test
  public void dataSourceInitialization() throws Exception {
    SQLException connectionException = new SQLException();
    SQLException duplicateKeyException = new SQLException("test", "", 1);

    DataSource dataSource = mock(DataSource.class);
    given(dataSource.getConnection()).willThrow(connectionException);

    SQLErrorCodeSQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    assertThat(sext.translate("test", null, duplicateKeyException)).isNull();

    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");

    Connection connection = mock(Connection.class);
    given(connection.getMetaData()).willReturn(databaseMetaData);

    Mockito.reset(dataSource);
    given(dataSource.getConnection()).willReturn(connection);
    assertThat(sext.translate("test", null, duplicateKeyException)).isInstanceOf(DuplicateKeyException.class);

    verify(connection).close();
  }

}
