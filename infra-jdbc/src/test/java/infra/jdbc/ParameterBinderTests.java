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

package infra.jdbc;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import infra.jdbc.type.TypeHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:12
 */
class ParameterBinderTests {

  @Test
  void shouldBindNullValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);

    ParameterBinder.null_binder.bind(statement, 1);

    verify(statement).setObject(1, null);
  }

  @Test
  void shouldBindIntegerValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    int value = 42;

    ParameterBinder binder = ParameterBinder.forInt(value);
    binder.bind(statement, 1);

    verify(statement).setInt(1, value);
  }

  @Test
  void shouldBindLongValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    long value = 123456789L;

    ParameterBinder binder = ParameterBinder.forLong(value);
    binder.bind(statement, 1);

    verify(statement).setLong(1, value);
  }

  @Test
  void shouldBindStringValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    String value = "testValue";

    ParameterBinder binder = ParameterBinder.forString(value);
    binder.bind(statement, 1);

    verify(statement).setString(1, value);
  }

  @Test
  void shouldBindTimestampValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    Timestamp value = new Timestamp(System.currentTimeMillis());

    ParameterBinder binder = ParameterBinder.forTimestamp(value);
    binder.bind(statement, 1);

    verify(statement).setTimestamp(1, value);
  }

  @Test
  void shouldBindTimeValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    Time value = new Time(System.currentTimeMillis());

    ParameterBinder binder = ParameterBinder.forTime(value);
    binder.bind(statement, 1);

    verify(statement).setTime(1, value);
  }

  @Test
  void shouldBindDateValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    Date value = new Date(System.currentTimeMillis());

    ParameterBinder binder = ParameterBinder.forDate(value);
    binder.bind(statement, 1);

    verify(statement).setDate(1, value);
  }

  @Test
  void shouldBindBooleanValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    boolean value = true;

    ParameterBinder binder = ParameterBinder.forBoolean(value);
    binder.bind(statement, 1);

    verify(statement).setBoolean(1, value);
  }

  @Test
  void shouldBindBinaryStreamValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    InputStream value = mock(InputStream.class);

    ParameterBinder binder = ParameterBinder.forBinaryStream(value);
    binder.bind(statement, 1);

    verify(statement).setBinaryStream(1, value);
  }

  @Test
  void shouldBindObjectValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    Object value = new Object();

    ParameterBinder binder = ParameterBinder.forObject(value);
    binder.bind(statement, 1);

    verify(statement).setObject(1, value);
  }

  @Test
  void shouldBindNullObjectValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    Object value = null;

    ParameterBinder binder = ParameterBinder.forObject(value);
    binder.bind(statement, 1);

    verify(statement).setObject(1, null);
  }

  @Test
  void shouldBindWithTypeHandler() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    TypeHandler<String> typeHandler = mock(TypeHandler.class);
    String value = "testValue";

    ParameterBinder binder = ParameterBinder.forTypeHandler(typeHandler, value);
    binder.bind(statement, 1);

    verify(typeHandler).setParameter(statement, 1, value);
  }

  @Test
  void shouldCreateDifferentBindersForSameValues() {
    int value1 = 42;
    int value2 = 42;

    ParameterBinder binder1 = ParameterBinder.forInt(value1);
    ParameterBinder binder2 = ParameterBinder.forInt(value2);

    assertThat(binder1).isNotSameAs(binder2);
  }

  @Test
  void shouldHandleNegativeIntegerValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    int value = -42;

    ParameterBinder binder = ParameterBinder.forInt(value);
    binder.bind(statement, 1);

    verify(statement).setInt(1, value);
  }

  @Test
  void shouldHandleZeroLongValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    long value = 0L;

    ParameterBinder binder = ParameterBinder.forLong(value);
    binder.bind(statement, 1);

    verify(statement).setLong(1, value);
  }

  @Test
  void shouldHandleEmptyStringValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    String value = "";

    ParameterBinder binder = ParameterBinder.forString(value);
    binder.bind(statement, 1);

    verify(statement).setString(1, value);
  }

  @Test
  void shouldHandleNullStringValue() throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    String value = null;

    ParameterBinder binder = ParameterBinder.forString(value);
    binder.bind(statement, 1);

    verify(statement).setString(1, null);
  }

}