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

import org.junit.jupiter.api.Test;

import java.sql.SQLDataException;
import java.sql.SQLException;

import infra.dao.DataAccessException;
import infra.dao.TransientDataAccessResourceException;
import infra.jdbc.BadSqlGrammarException;

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
