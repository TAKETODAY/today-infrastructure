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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:16
 */
class SqlInOutParameterTests {

  @Test
  void shouldCreateSqlInOutParameterWithNameAndSqlType() {
    String name = "testParam";
    int sqlType = java.sql.Types.VARCHAR;

    SqlInOutParameter parameter = new SqlInOutParameter(name, sqlType);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getScale()).isNull();
    assertThat(parameter.getTypeName()).isNull();
    assertThat(parameter.isInputValueProvided()).isTrue();
  }

  @Test
  void shouldCreateSqlInOutParameterWithNameSqlTypeAndScale() {
    String name = "testParam";
    int sqlType = java.sql.Types.DECIMAL;
    int scale = 2;

    SqlInOutParameter parameter = new SqlInOutParameter(name, sqlType, scale);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getScale()).isEqualTo(scale);
    assertThat(parameter.getTypeName()).isNull();
    assertThat(parameter.isInputValueProvided()).isTrue();
  }

  @Test
  void shouldCreateSqlInOutParameterWithNameSqlTypeAndTypeName() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    String typeName = "CUSTOM_TYPE";

    SqlInOutParameter parameter = new SqlInOutParameter(name, sqlType, typeName);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isEqualTo(typeName);
    assertThat(parameter.isInputValueProvided()).isTrue();
  }

  @Test
  void shouldCreateSqlInOutParameterWithCustomReturnType() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    String typeName = "CUSTOM_TYPE";
    SqlReturnType sqlReturnType = mock(SqlReturnType.class);

    SqlInOutParameter parameter = new SqlInOutParameter(name, sqlType, typeName, sqlReturnType);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isEqualTo(typeName);
    assertThat(parameter.getSqlReturnType()).isEqualTo(sqlReturnType);
    assertThat(parameter.isReturnTypeSupported()).isTrue();
    assertThat(parameter.isInputValueProvided()).isTrue();
  }

  @Test
  void shouldCreateSqlInOutParameterWithResultSetExtractor() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    ResultSetExtractor<?> rse = mock(ResultSetExtractor.class);

    SqlInOutParameter parameter = new SqlInOutParameter(name, sqlType, rse);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getResultSetExtractor()).isEqualTo(rse);
    assertThat(parameter.isInputValueProvided()).isTrue();
  }

  @Test
  void shouldCreateSqlInOutParameterWithRowCallbackHandler() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    RowCallbackHandler rch = mock(RowCallbackHandler.class);

    SqlInOutParameter parameter = new SqlInOutParameter(name, sqlType, rch);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getRowCallbackHandler()).isEqualTo(rch);
    assertThat(parameter.isInputValueProvided()).isTrue();
  }

  @Test
  void shouldCreateSqlInOutParameterWithRowMapper() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    RowMapper<?> rm = mock(RowMapper.class);

    SqlInOutParameter parameter = new SqlInOutParameter(name, sqlType, rm);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getRowMapper()).isEqualTo(rm);
    assertThat(parameter.isInputValueProvided()).isTrue();
  }

  @Test
  void shouldAlwaysReturnTrueForIsInputValueProvided() {
    String name = "testParam";
    int sqlType = java.sql.Types.VARCHAR;

    SqlInOutParameter parameter = new SqlInOutParameter(name, sqlType);

    assertThat(parameter.isInputValueProvided()).isTrue();
  }

  @Test
  void shouldInheritFromSqlOutParameter() {
    String name = "testParam";
    int sqlType = java.sql.Types.VARCHAR;

    SqlInOutParameter parameter = new SqlInOutParameter(name, sqlType);

    assertThat(parameter).isInstanceOf(SqlOutParameter.class);
  }

}