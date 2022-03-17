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

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import cn.taketoday.dao.ConcurrencyFailureException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.dao.PermissionDeniedDataAccessException;
import cn.taketoday.dao.QueryTimeoutException;
import cn.taketoday.dao.RecoverableDataAccessException;
import cn.taketoday.dao.TransientDataAccessResourceException;
import cn.taketoday.jdbc.BadSqlGrammarException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thomas Risberg
 */
public class SQLExceptionSubclassTranslatorTests {

  private static SQLErrorCodes ERROR_CODES = new SQLErrorCodes();

  static {
    ERROR_CODES.setBadSqlGrammarCodes("1");
  }

  @Test
  public void errorCodeTranslation() {
    SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

    SQLException dataIntegrityViolationEx = SQLExceptionSubclassFactory.newSQLDataException("", "", 0);
    DataIntegrityViolationException divex = (DataIntegrityViolationException) sext.translate("task", "SQL", dataIntegrityViolationEx);
    assertThat(divex.getCause()).isEqualTo(dataIntegrityViolationEx);

    SQLException featureNotSupEx = SQLExceptionSubclassFactory.newSQLFeatureNotSupportedException("", "", 0);
    InvalidDataAccessApiUsageException idaex = (InvalidDataAccessApiUsageException) sext.translate("task", "SQL", featureNotSupEx);
    assertThat(idaex.getCause()).isEqualTo(featureNotSupEx);

    SQLException dataIntegrityViolationEx2 = SQLExceptionSubclassFactory.newSQLIntegrityConstraintViolationException("", "", 0);
    DataIntegrityViolationException divex2 = (DataIntegrityViolationException) sext.translate("task", "SQL", dataIntegrityViolationEx2);
    assertThat(divex2.getCause()).isEqualTo(dataIntegrityViolationEx2);

    SQLException permissionDeniedEx = SQLExceptionSubclassFactory.newSQLInvalidAuthorizationSpecException("", "", 0);
    PermissionDeniedDataAccessException pdaex = (PermissionDeniedDataAccessException) sext.translate("task", "SQL", permissionDeniedEx);
    assertThat(pdaex.getCause()).isEqualTo(permissionDeniedEx);

    SQLException dataAccessResourceEx = SQLExceptionSubclassFactory.newSQLNonTransientConnectionException("", "", 0);
    DataAccessResourceFailureException darex = (DataAccessResourceFailureException) sext.translate("task", "SQL", dataAccessResourceEx);
    assertThat(darex.getCause()).isEqualTo(dataAccessResourceEx);

    SQLException badSqlEx2 = SQLExceptionSubclassFactory.newSQLSyntaxErrorException("", "", 0);
    BadSqlGrammarException bsgex2 = (BadSqlGrammarException) sext.translate("task", "SQL2", badSqlEx2);
    assertThat(bsgex2.getSql()).isEqualTo("SQL2");
    assertThat((Object) bsgex2.getSQLException()).isEqualTo(badSqlEx2);

    SQLException tranRollbackEx = SQLExceptionSubclassFactory.newSQLTransactionRollbackException("", "", 0);
    ConcurrencyFailureException cfex = (ConcurrencyFailureException) sext.translate("task", "SQL", tranRollbackEx);
    assertThat(cfex.getCause()).isEqualTo(tranRollbackEx);

    SQLException transientConnEx = SQLExceptionSubclassFactory.newSQLTransientConnectionException("", "", 0);
    TransientDataAccessResourceException tdarex = (TransientDataAccessResourceException) sext.translate("task", "SQL", transientConnEx);
    assertThat(tdarex.getCause()).isEqualTo(transientConnEx);

    SQLException transientConnEx2 = SQLExceptionSubclassFactory.newSQLTimeoutException("", "", 0);
    QueryTimeoutException tdarex2 = (QueryTimeoutException) sext.translate("task", "SQL", transientConnEx2);
    assertThat(tdarex2.getCause()).isEqualTo(transientConnEx2);

    SQLException recoverableEx = SQLExceptionSubclassFactory.newSQLRecoverableException("", "", 0);
    RecoverableDataAccessException rdaex2 = (RecoverableDataAccessException) sext.translate("task", "SQL", recoverableEx);
    assertThat(rdaex2.getCause()).isEqualTo(recoverableEx);

    // Test classic error code translation. We should move there next if the exception we pass in is not one
    // of the new sub-classes.
    SQLException sexEct = new SQLException("", "", 1);
    BadSqlGrammarException bsgEct = (BadSqlGrammarException) sext.translate("task", "SQL-ECT", sexEct);
    assertThat(bsgEct.getSql()).isEqualTo("SQL-ECT");
    assertThat((Object) bsgEct.getSQLException()).isEqualTo(sexEct);

    // Test fallback. We assume that no database will ever return this error code,
    // but 07xxx will be bad grammar picked up by the fallback SQLState translator
    SQLException sexFbt = new SQLException("", "07xxx", 666666666);
    BadSqlGrammarException bsgFbt = (BadSqlGrammarException) sext.translate("task", "SQL-FBT", sexFbt);
    assertThat(bsgFbt.getSql()).isEqualTo("SQL-FBT");
    assertThat((Object) bsgFbt.getSQLException()).isEqualTo(sexFbt);
    // and 08xxx will be data resource failure (non-transient) picked up by the fallback SQLState translator
    SQLException sexFbt2 = new SQLException("", "08xxx", 666666666);
    DataAccessResourceFailureException darfFbt = (DataAccessResourceFailureException) sext.translate("task", "SQL-FBT2", sexFbt2);
    assertThat(darfFbt.getCause()).isEqualTo(sexFbt2);
  }

}
