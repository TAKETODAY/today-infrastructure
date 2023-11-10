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

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import cn.taketoday.dao.CannotAcquireLockException;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.DuplicateKeyException;
import cn.taketoday.dao.PessimisticLockingFailureException;
import cn.taketoday.dao.TransientDataAccessResourceException;
import cn.taketoday.jdbc.BadSqlGrammarException;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SQLStateSQLExceptionTranslatorTests {

  private final SQLExceptionTranslator translator = new SQLStateSQLExceptionTranslator();

  @Test
  void translateNullException() {
    assertThatIllegalArgumentException().isThrownBy(() -> translator.translate("", "", null));
  }

  @Test
  void translateBadSqlGrammar() {
    assertTranslation("07", BadSqlGrammarException.class);
  }

  @Test
  void translateDataIntegrityViolation() {
    assertTranslation("23", DataIntegrityViolationException.class);
  }

  @Test
  void translateDuplicateKey() {
    assertTranslation("23505", DuplicateKeyException.class);
  }

  @Test
  void translateDuplicateKeyOracle() {
    assertTranslation("23000", 1, DuplicateKeyException.class);
  }

  @Test
  void translateDuplicateKeyMySQL() {
    assertTranslation("23000", 1062, DuplicateKeyException.class);
  }

  @Test
  void translateDuplicateKeyMSSQL1() {
    assertTranslation("23000", 2601, DuplicateKeyException.class);
  }

  @Test
  void translateDuplicateKeyMSSQL2() {
    assertTranslation("23000", 2627, DuplicateKeyException.class);
  }

  @Test
    // gh-31554
  void translateDuplicateKeySapHana() {
    assertTranslation("23000", 301, DuplicateKeyException.class);
  }

  @Test
  void translateDataAccessResourceFailure() {
    assertTranslation("53", DataAccessResourceFailureException.class);
  }

  @Test
  void translateTransientDataAccessResourceFailure() {
    assertTranslation("S1", TransientDataAccessResourceException.class);
  }

  @Test
  void translatePessimisticLockingFailure() {
    assertTranslation("40", PessimisticLockingFailureException.class);
  }

  @Test
  void translateCannotAcquireLock() {
    assertTranslation("40001", CannotAcquireLockException.class);
  }

  @Test
  void translateUncategorized() {
    assertTranslation("00000000", null);
  }

  @Test
  void invalidSqlStateCode() {
    assertTranslation("NO SUCH CODE", null);
  }

  /**
   * PostgreSQL can return null.
   * SAP DB can apparently return empty SQL code.
   * Bug 729170
   */
  @Test
  void malformedSqlStateCodes() {
    assertTranslation(null, null);
    assertTranslation("", null);
    assertTranslation("I", null);
  }

  private void assertTranslation(@Nullable String sqlState, @Nullable Class<?> dataAccessExceptionType) {
    assertTranslation(sqlState, 0, dataAccessExceptionType);
  }

  private void assertTranslation(@Nullable String sqlState, int errorCode, @Nullable Class<?> dataAccessExceptionType) {
    SQLException ex = new SQLException("reason", sqlState, errorCode);
    DataAccessException dax = translator.translate("task", "SQL", ex);

    if (dataAccessExceptionType == null) {
      assertThat(dax).as("Expected translation to null").isNull();
      return;
    }

    assertThat(dax).as("Specific translation must not result in null").isNotNull();
    assertThat(dax).as("Wrong DataAccessException type returned").isExactlyInstanceOf(dataAccessExceptionType);
    assertThat(dax.getCause()).as("The exact same original SQLException must be preserved").isSameAs(ex);
  }

}
