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