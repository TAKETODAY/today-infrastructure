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

import java.sql.SQLException;

import infra.context.support.ClassPathXmlApplicationContext;
import infra.dao.DataAccessException;
import infra.dao.TransientDataAccessResourceException;
import infra.jdbc.BadSqlGrammarException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for custom {@link SQLExceptionTranslator}.
 *
 * @author Thomas Risberg
 */
public class CustomSQLExceptionTranslatorRegistrarTests {

  @Test
  @SuppressWarnings("resource")
  public void customErrorCodeTranslation() {
    new ClassPathXmlApplicationContext("test-custom-translators-context.xml",
            CustomSQLExceptionTranslatorRegistrarTests.class);

    SQLErrorCodes codes = SQLErrorCodesFactory.of().getErrorCodes("H2");
    SQLErrorCodeSQLExceptionTranslator sext = new SQLErrorCodeSQLExceptionTranslator();
    sext.setSqlErrorCodes(codes);

    DataAccessException exFor4200 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 42000));
    assertThat(exFor4200).as("Should have been translated").isNotNull();
    assertThat(BadSqlGrammarException.class.isAssignableFrom(exFor4200.getClass())).as("Should have been instance of BadSqlGrammarException").isTrue();

    DataAccessException exFor2 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 2));
    assertThat(exFor2).as("Should have been translated").isNotNull();
    assertThat(TransientDataAccessResourceException.class.isAssignableFrom(exFor2.getClass())).as("Should have been instance of TransientDataAccessResourceException").isTrue();

    DataAccessException exFor3 = sext.doTranslate("", "", new SQLException("Ouch", "42000", 3));
    assertThat(exFor3).as("Should not have been translated").isNull();
  }

}
