/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.type;

import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.beans.BeanProperty;

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
    TypeHandler<Integer> delegate = mock(TypeHandler.class);
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
    TypeHandler<Integer> delegate = mock(TypeHandler.class);
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