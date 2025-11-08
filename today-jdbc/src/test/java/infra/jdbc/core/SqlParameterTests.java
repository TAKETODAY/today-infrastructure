/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jdbc.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:22
 */
class SqlParameterTests {

  @Test
  void shouldCreateAnonymousSqlParameterWithSqlType() {
    int sqlType = java.sql.Types.VARCHAR;

    SqlParameter parameter = new SqlParameter(sqlType);

    assertThat(parameter.getName()).isNull();
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isNull();
    assertThat(parameter.getScale()).isNull();
    assertThat(parameter.isInputValueProvided()).isTrue();
    assertThat(parameter.isResultsParameter()).isFalse();
  }

  @Test
  void shouldCreateAnonymousSqlParameterWithSqlTypeAndTypeName() {
    int sqlType = java.sql.Types.OTHER;
    String typeName = "CUSTOM_TYPE";

    SqlParameter parameter = new SqlParameter(sqlType, typeName);

    assertThat(parameter.getName()).isNull();
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isEqualTo(typeName);
    assertThat(parameter.getScale()).isNull();
  }

  @Test
  void shouldCreateAnonymousSqlParameterWithSqlTypeAndScale() {
    int sqlType = java.sql.Types.DECIMAL;
    int scale = 2;

    SqlParameter parameter = new SqlParameter(sqlType, scale);

    assertThat(parameter.getName()).isNull();
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isNull();
    assertThat(parameter.getScale()).isEqualTo(scale);
  }

  @Test
  void shouldCreateNamedSqlParameterWithNameAndSqlType() {
    String name = "testParam";
    int sqlType = java.sql.Types.VARCHAR;

    SqlParameter parameter = new SqlParameter(name, sqlType);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isNull();
    assertThat(parameter.getScale()).isNull();
  }

  @Test
  void shouldCreateNamedSqlParameterWithNameSqlTypeAndTypeName() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    String typeName = "CUSTOM_TYPE";

    SqlParameter parameter = new SqlParameter(name, sqlType, typeName);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isEqualTo(typeName);
    assertThat(parameter.getScale()).isNull();
  }

  @Test
  void shouldCreateNamedSqlParameterWithNameSqlTypeAndScale() {
    String name = "testParam";
    int sqlType = java.sql.Types.DECIMAL;
    int scale = 2;

    SqlParameter parameter = new SqlParameter(name, sqlType, scale);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isNull();
    assertThat(parameter.getScale()).isEqualTo(scale);
  }

  @Test
  void shouldThrowExceptionWhenCopyingNullSqlParameter() {
    assertThatThrownBy(() -> new SqlParameter(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SqlParameter object is required");
  }

  @Test
  void shouldReturnTrueForIsInputValueProvided() {
    SqlParameter parameter = new SqlParameter("test", java.sql.Types.VARCHAR);

    assertThat(parameter.isInputValueProvided()).isTrue();
  }

  @Test
  void shouldReturnFalseForIsResultsParameter() {
    SqlParameter parameter = new SqlParameter("test", java.sql.Types.VARCHAR);

    assertThat(parameter.isResultsParameter()).isFalse();
  }

  @Test
  void shouldConvertSqlTypesToAnonymousParameterList() {
    int[] types = { java.sql.Types.VARCHAR, java.sql.Types.INTEGER, java.sql.Types.DATE };

    List<SqlParameter> parameters = SqlParameter.sqlTypesToAnonymousParameterList(types);

    assertThat(parameters).hasSize(3);
    assertThat(parameters.get(0).getSqlType()).isEqualTo(java.sql.Types.VARCHAR);
    assertThat(parameters.get(1).getSqlType()).isEqualTo(java.sql.Types.INTEGER);
    assertThat(parameters.get(2).getSqlType()).isEqualTo(java.sql.Types.DATE);

    for (SqlParameter param : parameters) {
      assertThat(param.getName()).isNull();
      assertThat(param.isInputValueProvided()).isTrue();
      assertThat(param.isResultsParameter()).isFalse();
    }
  }

  @Test
  void shouldConvertNullSqlTypesToEmptyList() {
    List<SqlParameter> parameters = SqlParameter.sqlTypesToAnonymousParameterList((int[]) null);

    assertThat(parameters).isEmpty();
  }

  @Test
  void shouldConvertEmptySqlTypesToArray() {
    int[] types = {};

    List<SqlParameter> parameters = SqlParameter.sqlTypesToAnonymousParameterList(types);

    assertThat(parameters).isEmpty();
  }

}