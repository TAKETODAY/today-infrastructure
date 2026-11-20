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

package infra.jdbc.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UnknownTypeHandlerTests {

  private final UnknownTypeHandler handler = new UnknownTypeHandler(new TypeHandlerManager());

  private final ResultSet resultSet = mock(ResultSet.class);

  private final ResultSetMetaData metadata = mock(ResultSetMetaData.class);

  @Test
  void bindsKnownTypesNativelyAndFallsBackToSetObject() throws SQLException {
    var statement = mock(PreparedStatement.class);
    var customValue = new Object();

    handler.setNonNullParameter(statement, 1, "text");
    handler.setNonNullParameter(statement, 2, customValue);

    verify(statement).setString(1, "text");
    verify(statement).setObject(2, customValue);
  }

  @Test
  void readsKnownTypeByIndex() throws SQLException {
    given(resultSet.getMetaData()).willReturn(metadata);
    given(metadata.getColumnClassName(2)).willReturn(String.class.getName());
    given(resultSet.getString(2)).willReturn("value");

    assertThat(handler.getResult(resultSet, 2)).isEqualTo("value");
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void matchesDriverColumnLabelOrName(boolean useLabel) throws SQLException {
    given(resultSet.getMetaData()).willReturn(metadata);
    given(metadata.getColumnCount()).willReturn(2);
    if (useLabel) {
      given(metadata.getColumnLabel(1)).willReturn("unrelated");
      given(metadata.getColumnLabel(2)).willReturn("selected");
    }
    else {
      given(metadata.getColumnName(1)).willReturn("unrelated");
      given(metadata.getColumnName(2)).willReturn("selected");
    }
    given(metadata.getColumnClassName(2)).willReturn(String.class.getName());
    given(resultSet.getString("selected")).willReturn("value");
    handler.setUseColumnLabel(useLabel);

    assertThat(handler.getResult(resultSet, "selected")).isEqualTo("value");
  }

  @ParameterizedTest
  @ValueSource(strings = { "java.lang.Object", "example.MissingDriverClass" })
  void fallsBackToUntypedReadForUnknownColumnType(String className) throws SQLException {
    given(resultSet.getMetaData()).willReturn(metadata);
    given(metadata.getColumnCount()).willReturn(1);
    given(metadata.getColumnLabel(1)).willReturn("data");
    given(metadata.getColumnClassName(1)).willReturn(className);
    var value = new Object();
    given(resultSet.getObject(1)).willReturn(value);
    given(resultSet.getObject("data")).willReturn(value);

    assertThat(handler.getResult(resultSet, 1)).isSameAs(value);
    assertThat(handler.getResult(resultSet, "data")).isSameAs(value);
  }

  @Test
  void translatesFailureToIdentifyNamedColumn() throws SQLException {
    var failure = new SQLException("metadata unavailable");
    given(resultSet.getMetaData()).willThrow(failure);

    assertThatThrownBy(() -> handler.getResult(resultSet, "data"))
            .isInstanceOf(TypeException.class).hasMessageContaining("column data").hasCause(failure);
  }

  @Test
  void readsUntypedOutParameterDirectly() throws SQLException {
    var statement = mock(CallableStatement.class);
    var value = new Object();
    given(statement.getObject(3)).willReturn(value);

    assertThat(handler.getResult(statement, 3)).isSameAs(value);
  }

}
