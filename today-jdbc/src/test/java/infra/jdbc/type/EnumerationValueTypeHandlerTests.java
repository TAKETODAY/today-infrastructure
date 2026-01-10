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
import org.mockito.Mockito;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.beans.BeanProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/2 21:19
 */
class EnumerationValueTypeHandlerTests {
  enum StringCode {

    TEST1(1, "desc1"),
    TEST2(2, "desc2");

    @EnumerationValue
    final int code;

    final String desc;

    StringCode(int code, String desc) {
      this.code = code;
      this.desc = desc;
    }
  }

  enum StringValue {

    TEST1(1, "desc1"),
    TEST2(2, "desc2");

    final int value;

    final String desc;

    StringValue(int value, String desc) {
      this.value = value;
      this.desc = desc;
    }
  }

  enum StringValueNotFound {

    TEST1(1, "desc1"),
    TEST2(2, "desc2");

    final int value1;

    final String desc;

    StringValueNotFound(int value, String desc) {
      this.value1 = value;
      this.desc = desc;
    }
  }

  @Test
  void getAnnotatedProperty() {
    BeanProperty annotatedProperty = EnumerationValueTypeHandler.getAnnotatedProperty(StringCode.class);
    assertThat(annotatedProperty).isNotNull();
    assertThat(annotatedProperty.getType()).isEqualTo(int.class);

    //
    annotatedProperty = EnumerationValueTypeHandler.getAnnotatedProperty(StringValue.class);
    assertThat(annotatedProperty).isNotNull();
    assertThat(annotatedProperty.getType()).isEqualTo(int.class);

    assertThat(EnumerationValueTypeHandler.getAnnotatedProperty(StringValueNotFound.class)).isNull();

  }

  @Test
  void setParameter() throws SQLException {
    TypeHandler<Integer> delegate = Mockito.mock(TypeHandler.class);
    PreparedStatement statement = mock(PreparedStatement.class);

    TypeHandlerManager registry = new TypeHandlerManager();
    registry.register(int.class, delegate);

    var handler = new EnumerationValueTypeHandler<>(StringCode.class, registry);

    handler.setParameter(statement, 1, null);
    handler.setParameter(statement, 1, StringCode.TEST1);

    verify(delegate).setParameter(statement, 1, null);
    verify(delegate).setParameter(statement, 1, StringCode.TEST1.code);
  }

  @Test
  void getResult() throws SQLException {
    TypeHandler<Integer> delegate = Mockito.mock(TypeHandler.class);
    ResultSet resultSet = mock(ResultSet.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(delegate.getResult(resultSet, 1)).willReturn(StringCode.TEST1.code);
    given(delegate.getResult(resultSet, "test")).willReturn(StringCode.TEST2.code);
    given(delegate.getResult(callableStatement, 1)).willReturn(StringCode.TEST2.code);

    TypeHandlerManager registry = new TypeHandlerManager();
    registry.register(int.class, delegate);

    var handler = new EnumerationValueTypeHandler<>(StringCode.class, registry);

    assertThat(handler.getResult(resultSet, 1)).isEqualTo(StringCode.TEST1);
    assertThat(handler.getResult(resultSet, "test")).isEqualTo(StringCode.TEST2);
    assertThat(handler.getResult(callableStatement, 1)).isEqualTo(StringCode.TEST2);

    verify(delegate).getResult(resultSet, 1);
    verify(delegate).getResult(resultSet, "test");
    verify(delegate).getResult(callableStatement, 1);

  }

}