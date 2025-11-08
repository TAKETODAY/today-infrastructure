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

package infra.jdbc.core.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:45
 */
class CallParameterMetaDataTests {

  @Test
  void shouldCreateCallParameterMetaDataWithAllParameters() {
    boolean function = true;
    String parameterName = "testParam";
    int parameterType = 1;
    int sqlType = 2;
    String typeName = "VARCHAR";
    boolean nullable = true;

    CallParameterMetaData metaData = new CallParameterMetaData(function, parameterName, parameterType, sqlType, typeName, nullable);

    assertThat(metaData.isFunction()).isEqualTo(function);
    assertThat(metaData.getParameterName()).isEqualTo(parameterName);
    assertThat(metaData.getParameterType()).isEqualTo(parameterType);
    assertThat(metaData.getSqlType()).isEqualTo(sqlType);
    assertThat(metaData.getTypeName()).isEqualTo(typeName);
    assertThat(metaData.isNullable()).isEqualTo(nullable);
  }

  @Test
  void shouldHandleNullParameterNameAndTypeName() {
    boolean function = false;
    int parameterType = 1;
    int sqlType = 2;
    boolean nullable = false;

    CallParameterMetaData metaData = new CallParameterMetaData(function, null, parameterType, sqlType, null, nullable);

    assertThat(metaData.isFunction()).isEqualTo(function);
    assertThat(metaData.getParameterName()).isNull();
    assertThat(metaData.getParameterType()).isEqualTo(parameterType);
    assertThat(metaData.getSqlType()).isEqualTo(sqlType);
    assertThat(metaData.getTypeName()).isNull();
    assertThat(metaData.isNullable()).isEqualTo(nullable);
  }

  @Test
  void shouldIdentifyReturnParameterInFunction() {
    CallParameterMetaData metaData = new CallParameterMetaData(true, "returnParam", java.sql.DatabaseMetaData.functionReturn, 1, "INTEGER", false);

    assertThat(metaData.isReturnParameter()).isTrue();
    assertThat(metaData.isOutParameter()).isFalse();
    assertThat(metaData.isInOutParameter()).isFalse();
  }

  @Test
  void shouldIdentifyReturnParameterInProcedure() {
    CallParameterMetaData metaData = new CallParameterMetaData(false, "returnParam", java.sql.DatabaseMetaData.procedureColumnReturn, 1, "INTEGER", false);

    assertThat(metaData.isReturnParameter()).isTrue();
    assertThat(metaData.isOutParameter()).isFalse();
    assertThat(metaData.isInOutParameter()).isFalse();
  }

  @Test
  void shouldIdentifyResultParameterInProcedure() {
    CallParameterMetaData metaData = new CallParameterMetaData(false, "resultParam", java.sql.DatabaseMetaData.procedureColumnResult, 1, "CURSOR", false);

    assertThat(metaData.isReturnParameter()).isTrue();
    assertThat(metaData.isOutParameter()).isFalse();
    assertThat(metaData.isInOutParameter()).isFalse();
  }

  @Test
  void shouldIdentifyOutParameterInFunction() {
    CallParameterMetaData metaData = new CallParameterMetaData(true, "outParam", java.sql.DatabaseMetaData.functionColumnOut, 1, "VARCHAR", true);

    assertThat(metaData.isReturnParameter()).isFalse();
    assertThat(metaData.isOutParameter()).isTrue();
    assertThat(metaData.isInOutParameter()).isFalse();
  }

  @Test
  void shouldIdentifyOutParameterInProcedure() {
    CallParameterMetaData metaData = new CallParameterMetaData(false, "outParam", java.sql.DatabaseMetaData.procedureColumnOut, 1, "VARCHAR", true);

    assertThat(metaData.isReturnParameter()).isFalse();
    assertThat(metaData.isOutParameter()).isTrue();
    assertThat(metaData.isInOutParameter()).isFalse();
  }

  @Test
  void shouldIdentifyInOutParameterInFunction() {
    CallParameterMetaData metaData = new CallParameterMetaData(true, "inOutParam", java.sql.DatabaseMetaData.functionColumnInOut, 1, "VARCHAR", true);

    assertThat(metaData.isReturnParameter()).isFalse();
    assertThat(metaData.isOutParameter()).isFalse();
    assertThat(metaData.isInOutParameter()).isTrue();
  }

  @Test
  void shouldIdentifyInOutParameterInProcedure() {
    CallParameterMetaData metaData = new CallParameterMetaData(false, "inOutParam", java.sql.DatabaseMetaData.procedureColumnInOut, 1, "VARCHAR", true);

    assertThat(metaData.isReturnParameter()).isFalse();
    assertThat(metaData.isOutParameter()).isFalse();
    assertThat(metaData.isInOutParameter()).isTrue();
  }

  @Test
  void shouldIdentifyInParameter() {
    CallParameterMetaData metaData = new CallParameterMetaData(false, "inParam", java.sql.DatabaseMetaData.procedureColumnIn, 1, "VARCHAR", true);

    assertThat(metaData.isReturnParameter()).isFalse();
    assertThat(metaData.isOutParameter()).isFalse();
    assertThat(metaData.isInOutParameter()).isFalse();
  }

  @Test
  void shouldHandleUnknownParameterType() {
    CallParameterMetaData metaData = new CallParameterMetaData(false, "unknownParam", 999, 1, "UNKNOWN", false);

    assertThat(metaData.isReturnParameter()).isFalse();
    assertThat(metaData.isOutParameter()).isFalse();
    assertThat(metaData.isInOutParameter()).isFalse();
  }

  @Test
  void shouldCreateWithZeroValues() {
    CallParameterMetaData metaData = new CallParameterMetaData(false, "", 0, 0, "", false);

    assertThat(metaData.isFunction()).isFalse();
    assertThat(metaData.getParameterName()).isEqualTo("");
    assertThat(metaData.getParameterType()).isEqualTo(0);
    assertThat(metaData.getSqlType()).isEqualTo(0);
    assertThat(metaData.getTypeName()).isEqualTo("");
    assertThat(metaData.isNullable()).isFalse();
  }

  @Test
  void shouldCreateWithNegativeSqlType() {
    CallParameterMetaData metaData = new CallParameterMetaData(true, "negativeParam", java.sql.DatabaseMetaData.functionColumnOut, -1, "CUSTOM", true);

    assertThat(metaData.isFunction()).isTrue();
    assertThat(metaData.getParameterName()).isEqualTo("negativeParam");
    assertThat(metaData.getSqlType()).isEqualTo(-1);
    assertThat(metaData.getTypeName()).isEqualTo("CUSTOM");
    assertThat(metaData.isNullable()).isTrue();
    assertThat(metaData.isOutParameter()).isTrue();
  }

}