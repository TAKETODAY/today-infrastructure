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

  @Test
  public void translateNullException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new SQLStateSQLExceptionTranslator().translate("", "", null));
  }

  @Test
  public void translateBadSqlGrammar() {
    doTest("07", BadSqlGrammarException.class);
  }

  @Test
  public void translateDataIntegrityViolation() {
    doTest("23", DataIntegrityViolationException.class);
  }

  @Test
  public void translateDuplicateKey() {
    doTest("23505", DuplicateKeyException.class);
  }

  @Test
  public void translateDataAccessResourceFailure() {
    doTest("53", DataAccessResourceFailureException.class);
  }

  @Test
  public void translateTransientDataAccessResourceFailure() {
    doTest("S1", TransientDataAccessResourceException.class);
  }

  @Test
  public void translatePessimisticLockingFailure() {
    doTest("40", PessimisticLockingFailureException.class);
  }

  @Test
  public void translateCannotAcquireLock() {
    doTest("40001", CannotAcquireLockException.class);
  }

  @Test
  public void translateUncategorized() {
    doTest("00000000", null);
  }

  @Test
  public void invalidSqlStateCode() {
    doTest("NO SUCH CODE", null);
  }

  /**
   * PostgreSQL can return null.
   * SAP DB can apparently return empty SQL code.
   * Bug 729170
   */
  @Test
  public void malformedSqlStateCodes() {
    doTest(null, null);
    doTest("", null);
    doTest("I", null);
  }

  private void doTest(@Nullable String sqlState, @Nullable Class<?> dataAccessExceptionType) {
    SQLExceptionTranslator translator = new SQLStateSQLExceptionTranslator();
    SQLException ex = new SQLException("reason", sqlState);
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
