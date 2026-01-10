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
 * @since 5.0 2025/11/8 21:17
 */
class ResultSetSupportingSqlParameterTests {

  @Test
  void shouldCreateResultSetSupportingSqlParameterWithNameAndSqlType() {
    String name = "testParam";
    int sqlType = java.sql.Types.VARCHAR;

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getScale()).isNull();
    assertThat(parameter.getTypeName()).isNull();
    assertThat(parameter.isResultSetSupported()).isFalse();
    assertThat(parameter.isInputValueProvided()).isFalse();
  }

  @Test
  void shouldCreateResultSetSupportingSqlParameterWithNameSqlTypeAndScale() {
    String name = "testParam";
    int sqlType = java.sql.Types.DECIMAL;
    int scale = 2;

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType, scale);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getScale()).isEqualTo(scale);
    assertThat(parameter.getTypeName()).isNull();
    assertThat(parameter.isResultSetSupported()).isFalse();
    assertThat(parameter.isInputValueProvided()).isFalse();
  }

  @Test
  void shouldCreateResultSetSupportingSqlParameterWithNameSqlTypeAndTypeName() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    String typeName = "CUSTOM_TYPE";

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType, typeName);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isEqualTo(typeName);
    assertThat(parameter.isResultSetSupported()).isFalse();
    assertThat(parameter.isInputValueProvided()).isFalse();
  }

  @Test
  void shouldCreateResultSetSupportingSqlParameterWithResultSetExtractor() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    ResultSetExtractor<?> rse = mock(ResultSetExtractor.class);

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType, rse);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getResultSetExtractor()).isEqualTo(rse);
    assertThat(parameter.isResultSetSupported()).isTrue();
    assertThat(parameter.isInputValueProvided()).isFalse();
  }

  @Test
  void shouldCreateResultSetSupportingSqlParameterWithRowCallbackHandler() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    RowCallbackHandler rch = mock(RowCallbackHandler.class);

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType, rch);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getRowCallbackHandler()).isEqualTo(rch);
    assertThat(parameter.isResultSetSupported()).isTrue();
    assertThat(parameter.isInputValueProvided()).isFalse();
  }

  @Test
  void shouldCreateResultSetSupportingSqlParameterWithRowMapper() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    RowMapper<?> rm = mock(RowMapper.class);

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType, rm);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getRowMapper()).isEqualTo(rm);
    assertThat(parameter.isResultSetSupported()).isTrue();
    assertThat(parameter.isInputValueProvided()).isFalse();
  }

  @Test
  void shouldReturnFalseWhenNoResultSetSupport() {
    String name = "testParam";
    int sqlType = java.sql.Types.VARCHAR;

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType);

    assertThat(parameter.isResultSetSupported()).isFalse();
    assertThat(parameter.getResultSetExtractor()).isNull();
    assertThat(parameter.getRowCallbackHandler()).isNull();
    assertThat(parameter.getRowMapper()).isNull();
  }

  @Test
  void shouldReturnTrueWhenResultSetExtractorIsSet() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    ResultSetExtractor<?> rse = mock(ResultSetExtractor.class);

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType, rse);

    assertThat(parameter.isResultSetSupported()).isTrue();
    assertThat(parameter.getResultSetExtractor()).isEqualTo(rse);
  }

  @Test
  void shouldReturnTrueWhenRowCallbackHandlerIsSet() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    RowCallbackHandler rch = mock(RowCallbackHandler.class);

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType, rch);

    assertThat(parameter.isResultSetSupported()).isTrue();
    assertThat(parameter.getRowCallbackHandler()).isEqualTo(rch);
  }

  @Test
  void shouldReturnTrueWhenRowMapperIsSet() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    RowMapper<?> rm = mock(RowMapper.class);

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType, rm);

    assertThat(parameter.isResultSetSupported()).isTrue();
    assertThat(parameter.getRowMapper()).isEqualTo(rm);
  }

  @Test
  void shouldAlwaysReturnFalseForIsInputValueProvided() {
    String name = "testParam";
    int sqlType = java.sql.Types.VARCHAR;

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType);

    assertThat(parameter.isInputValueProvided()).isFalse();
  }

  @Test
  void shouldInheritFromSqlParameter() {
    String name = "testParam";
    int sqlType = java.sql.Types.VARCHAR;

    ResultSetSupportingSqlParameter parameter = new ResultSetSupportingSqlParameter(name, sqlType);

    assertThat(parameter).isInstanceOf(SqlParameter.class);
  }

}