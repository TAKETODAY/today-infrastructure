/*
 * Copyright 2017 - 2026 the TODAY authors.
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