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

import java.sql.SQLDataException;
import java.sql.SQLException;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.TransientDataAccessResourceException;
import cn.taketoday.jdbc.BadSqlGrammarException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for custom SQLException translation.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 */
public class SQLExceptionCustomTranslatorTests {

  private static SQLErrorCodes ERROR_CODES = new SQLErrorCodes();

  static {
    ERROR_CODES.setBadSqlGrammarCodes("1");
    ERROR_CODES.setDataAccessResourceFailureCodes("2");
    ERROR_CODES.setCustomSqlExceptionTranslatorClass(CustomSqlExceptionTranslator.class);
  }

  private final SQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator(ERROR_CODES);

  @Test
  public void badSqlGrammarException() {
    SQLException badSqlGrammarExceptionEx = new SQLDataException("", "", 1);
    DataAccessException dae = sext.translate("task", "SQL", badSqlGrammarExceptionEx);
    assertThat(dae.getCause()).isEqualTo(badSqlGrammarExceptionEx);
    assertThat(dae).isInstanceOf(BadSqlGrammarException.class);
  }

  @Test
  public void dataAccessResourceException() {
    SQLException dataAccessResourceEx = new SQLDataException("", "", 2);
    DataAccessException dae = sext.translate("task", "SQL", dataAccessResourceEx);
    assertThat(dae.getCause()).isEqualTo(dataAccessResourceEx);
    assertThat(dae).isInstanceOf(TransientDataAccessResourceException.class);
  }

}
