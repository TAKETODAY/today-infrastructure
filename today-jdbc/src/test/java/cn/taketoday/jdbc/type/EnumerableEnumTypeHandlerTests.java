/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import cn.taketoday.lang.Enumerable;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/1 23:02
 */
class EnumerableEnumTypeHandlerTests {

  enum Gender implements Enumerable<Integer> {

    MALE(1, "男"),
    FEMALE(0, "女");

    private final int value;
    private final String desc;

    Gender(int value, String desc) {
      this.value = value;
      this.desc = desc;
    }

    @Override
    public Integer getValue() {
      return value;
    }

    @Override
    public String getDescription() {
      return desc;
    }

  }

  enum StringValue implements Enumerable {

    TEST1,
    TEST2;
  }

  @Data
  static class UserModel {

    Gender gender;

    StringValue stringValue;
  }

  @Test
  void test() {
    Class<?> valueType = EnumerableEnumTypeHandler.getValueType(Gender.class);
    assertThat(valueType).isNotNull().isEqualTo(Integer.class);

    assertThat(EnumerableEnumTypeHandler.getValueType(StringValue.class))
            .isEqualTo(String.class);

  }

  @Test
  void setParameter() throws SQLException {
    TypeHandler<Integer> delegate = mock(TypeHandler.class);
    PreparedStatement statement = mock(PreparedStatement.class);

    TypeHandlerRegistry registry = new TypeHandlerRegistry();
    registry.register(Integer.class, delegate);

    var handler = new EnumerableEnumTypeHandler<>(Gender.class, registry);

    handler.setParameter(statement, 1, null);
    handler.setParameter(statement, 1, Gender.MALE);

    verify(delegate).setParameter(statement, 1, null);
    verify(delegate).setParameter(statement, 1, Gender.MALE.getValue());
  }

  @Test
  void getResult() throws SQLException {
    TypeHandler<Integer> delegate = mock(TypeHandler.class);
    ResultSet resultSet = mock(ResultSet.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(delegate.getResult(resultSet, 1)).willReturn(Gender.MALE.getValue());
    given(delegate.getResult(resultSet, "gender")).willReturn(Gender.FEMALE.getValue());
    given(delegate.getResult(callableStatement, 1)).willReturn(Gender.FEMALE.getValue());

    TypeHandlerRegistry registry = new TypeHandlerRegistry();
    registry.register(Integer.class, delegate);

    var handler = new EnumerableEnumTypeHandler<>(Gender.class, registry);

    assertThat(handler.getResult(resultSet, 1)).isEqualTo(Gender.MALE);
    assertThat(handler.getResult(resultSet, "gender")).isEqualTo(Gender.FEMALE);
    assertThat(handler.getResult(callableStatement, 1)).isEqualTo(Gender.FEMALE);

    verify(delegate).getResult(resultSet, 1);
    verify(delegate).getResult(resultSet, "gender");
    verify(delegate).getResult(callableStatement, 1);

  }
}