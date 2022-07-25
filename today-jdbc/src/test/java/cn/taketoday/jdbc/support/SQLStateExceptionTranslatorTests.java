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

import cn.taketoday.jdbc.BadSqlGrammarException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @since 13-Jan-03
 */
public class SQLStateExceptionTranslatorTests {

  private static final String sql = "SELECT FOO FROM BAR";

  private final SQLStateSQLExceptionTranslator trans = new SQLStateSQLExceptionTranslator();

  // ALSO CHECK CHAIN of SQLExceptions!?
  // also allow chain of translators? default if can't do specific?

  @Test
  public void badSqlGrammar() {
    SQLException sex = new SQLException("Message", "42001", 1);
    try {
      throw this.trans.translate("task", sql, sex);
    }
    catch (BadSqlGrammarException ex) {
      // OK
      assertThat(sql.equals(ex.getSql())).as("SQL is correct").isTrue();
      assertThat(sex.equals(ex.getSQLException())).as("Exception matches").isTrue();
    }
  }

  @Test
  public void invalidSqlStateCode() {
    SQLException sex = new SQLException("Message", "NO SUCH CODE", 1);
    assertThat(this.trans.translate("task", sql, sex)).isNull();
  }

  /**
   * PostgreSQL can return null.
   * SAP DB can apparently return empty SQL code.
   * Bug 729170
   */
  @Test
  public void malformedSqlStateCodes() {
    SQLException sex = new SQLException("Message", null, 1);
    assertThat(this.trans.translate("task", sql, sex)).isNull();

    sex = new SQLException("Message", "", 1);
    assertThat(this.trans.translate("task", sql, sex)).isNull();

    // One char's not allowed
    sex = new SQLException("Message", "I", 1);
    assertThat(this.trans.translate("task", sql, sex)).isNull();
  }

}
