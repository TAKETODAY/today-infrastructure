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
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.TransientDataAccessResourceException;
import cn.taketoday.jdbc.BadSqlGrammarException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SQLStateSQLExceptionTranslatorTests {

  private static final String REASON = "The game is afoot!";

  private static final String TASK = "Counting sheep... yawn.";

  private static final String SQL = "select count(0) from t_sheep where over_fence = ... yawn... 1";

  @Test
  public void testTranslateNullException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new SQLStateSQLExceptionTranslator().translate("", "", null));
  }

  @Test
  public void testTranslateBadSqlGrammar() {
    doTest("07", BadSqlGrammarException.class);
  }

  @Test
  public void testTranslateDataIntegrityViolation() {
    doTest("23", DataIntegrityViolationException.class);
  }

  @Test
  public void testTranslateDataAccessResourceFailure() {
    doTest("53", DataAccessResourceFailureException.class);
  }

  @Test
  public void testTranslateTransientDataAccessResourceFailure() {
    doTest("S1", TransientDataAccessResourceException.class);
  }

  @Test
  public void testTranslateConcurrencyFailure() {
    doTest("40", ConcurrencyFailureException.class);
  }

  @Test
  public void testTranslateUncategorized() {
    assertThat(new SQLStateSQLExceptionTranslator().translate("", "", new SQLException(REASON, "00000000"))).isNull();
  }

  private void doTest(String sqlState, Class<?> dataAccessExceptionType) {
    SQLException ex = new SQLException(REASON, sqlState);
    SQLExceptionTranslator translator = new SQLStateSQLExceptionTranslator();
    DataAccessException dax = translator.translate(TASK, SQL, ex);
    assertThat(dax).as("Specific translation must not result in a null DataAccessException being returned.").isNotNull();
    assertThat(dax.getClass()).as("Wrong DataAccessException type returned as the result of the translation").isEqualTo(dataAccessExceptionType);
    assertThat(dax.getCause()).as("The original SQLException must be preserved in the translated DataAccessException").isNotNull();
    assertThat(dax.getCause()).as("The exact same original SQLException must be preserved in the translated DataAccessException").isSameAs(ex);
  }

}
