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

import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdbcUtils}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class JdbcUtilsTests {

  @Test
  public void commonDatabaseName() {
    assertThat(JdbcUtils.commonDatabaseName("Oracle")).isEqualTo("Oracle");
    assertThat(JdbcUtils.commonDatabaseName("DB2-for-Spring")).isEqualTo("DB2");
    assertThat(JdbcUtils.commonDatabaseName("Sybase SQL Server")).isEqualTo("Sybase");
    assertThat(JdbcUtils.commonDatabaseName("Adaptive Server Enterprise")).isEqualTo("Sybase");
    assertThat(JdbcUtils.commonDatabaseName("MySQL")).isEqualTo("MySQL");
  }

  @Test
  public void resolveTypeName() {
    assertThat(JdbcUtils.resolveTypeName(Types.VARCHAR)).isEqualTo("VARCHAR");
    assertThat(JdbcUtils.resolveTypeName(Types.NUMERIC)).isEqualTo("NUMERIC");
    assertThat(JdbcUtils.resolveTypeName(Types.INTEGER)).isEqualTo("INTEGER");
    assertThat(JdbcUtils.resolveTypeName(JdbcUtils.TYPE_UNKNOWN)).isNull();
  }

  @Test
  public void convertUnderscoreNameToPropertyName() {
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("MY_NAME")).isEqualTo("myName");
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("yOUR_nAME")).isEqualTo("yourName");
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("a_name")).isEqualTo("AName");
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("someone_elses_name")).isEqualTo("someoneElsesName");
  }

}
