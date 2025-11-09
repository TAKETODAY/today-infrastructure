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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:15
 */
class SqlOutParameterTests {

  @Test
  void shouldCreateSqlOutParameterWithNameSqlTypeAndScale() {
    String name = "testParam";
    int sqlType = java.sql.Types.DECIMAL;
    int scale = 2;

    SqlOutParameter parameter = new SqlOutParameter(name, sqlType, scale);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getScale()).isEqualTo(scale);
    assertThat(parameter.getTypeName()).isNull();
  }

  @Test
  void shouldCreateSqlOutParameterWithNameSqlTypeAndTypeName() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    String typeName = "CUSTOM_TYPE";

    SqlOutParameter parameter = new SqlOutParameter(name, sqlType, typeName);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isEqualTo(typeName);
    assertThat(parameter.getSqlReturnType()).isNull();
    assertThat(parameter.isReturnTypeSupported()).isFalse();
  }

  @Test
  void shouldCreateSqlOutParameterWithCustomReturnType() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    String typeName = "CUSTOM_TYPE";
    SqlReturnType sqlReturnType = mock(SqlReturnType.class);

    SqlOutParameter parameter = new SqlOutParameter(name, sqlType, typeName, sqlReturnType);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isEqualTo(typeName);
    assertThat(parameter.getSqlReturnType()).isEqualTo(sqlReturnType);
    assertThat(parameter.isReturnTypeSupported()).isTrue();
  }

  @Test
  void shouldCreateSqlOutParameterWithResultSetExtractor() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    ResultSetExtractor<?> rse = mock(ResultSetExtractor.class);

    SqlOutParameter parameter = new SqlOutParameter(name, sqlType, rse);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getResultSetExtractor()).isEqualTo(rse);
  }

  @Test
  void shouldCreateSqlOutParameterWithRowCallbackHandler() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    RowCallbackHandler rch = mock(RowCallbackHandler.class);

    SqlOutParameter parameter = new SqlOutParameter(name, sqlType, rch);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getRowCallbackHandler()).isEqualTo(rch);
  }

  @Test
  void shouldCreateSqlOutParameterWithRowMapper() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    RowMapper<?> rm = mock(RowMapper.class);

    SqlOutParameter parameter = new SqlOutParameter(name, sqlType, rm);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getRowMapper()).isEqualTo(rm);
  }

  @Test
  void shouldReturnNullWhenNoCustomReturnType() {
    String name = "testParam";
    int sqlType = java.sql.Types.VARCHAR;

    SqlOutParameter parameter = new SqlOutParameter(name, sqlType);

    assertThat(parameter.getSqlReturnType()).isNull();
    assertThat(parameter.isReturnTypeSupported()).isFalse();
  }

  @Test
  void shouldReturnCustomReturnTypeWhenProvided() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    String typeName = "CUSTOM_TYPE";
    SqlReturnType sqlReturnType = mock(SqlReturnType.class);

    SqlOutParameter parameter = new SqlOutParameter(name, sqlType, typeName, sqlReturnType);

    assertThat(parameter.getSqlReturnType()).isEqualTo(sqlReturnType);
    assertThat(parameter.isReturnTypeSupported()).isTrue();
  }

  @Test
  void shouldHandleNullTypeNameWithCustomReturnType() {
    String name = "testParam";
    int sqlType = java.sql.Types.OTHER;
    SqlReturnType sqlReturnType = mock(SqlReturnType.class);

    SqlOutParameter parameter = new SqlOutParameter(name, sqlType, (String) null, sqlReturnType);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(sqlType);
    assertThat(parameter.getTypeName()).isNull();
    assertThat(parameter.getSqlReturnType()).isEqualTo(sqlReturnType);
    assertThat(parameter.isReturnTypeSupported()).isTrue();
  }

}