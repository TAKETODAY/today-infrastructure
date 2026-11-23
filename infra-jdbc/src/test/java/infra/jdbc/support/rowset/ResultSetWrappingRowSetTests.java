/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.support.rowset;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Stream;

import infra.jdbc.InvalidResultSetAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Thomas Risberg
 */
public class ResultSetWrappingRowSetTests {

  private ResultSet resultSet;

  private ResultSetWrappingSqlRowSet rowSet;

  @BeforeEach
  public void setup() throws Exception {
    resultSet = mock(ResultSet.class);
    rowSet = new ResultSetWrappingSqlRowSet(resultSet);
  }

  @Test
  public void testGetBigDecimalInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getBigDecimal", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBigDecimal", int.class);
    doTest(rset, rowset, 1, BigDecimal.ONE);
  }

  @Test
  public void testGetBigDecimalString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getBigDecimal", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBigDecimal", String.class);
    doTest(rset, rowset, "test", BigDecimal.ONE);
  }

  @Test
  public void testGetStringInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getString", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getString", int.class);
    doTest(rset, rowset, 1, "test");
  }

  @Test
  public void testGetStringString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getString", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getString", String.class);
    doTest(rset, rowset, "test", "test");
  }

  @Test
  public void testGetTimestampInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getTimestamp", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTimestamp", int.class);
    doTest(rset, rowset, 1, new Timestamp(1234L));
  }

  @Test
  public void testGetTimestampString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getTimestamp", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTimestamp", String.class);
    doTest(rset, rowset, "test", new Timestamp(1234L));
  }

  @Test
  public void testGetDateInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getDate", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDate", int.class);
    doTest(rset, rowset, 1, new Date(1234L));
  }

  @Test
  public void testGetDateString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getDate", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDate", String.class);
    doTest(rset, rowset, "test", new Date(1234L));
  }

  @Test
  public void testGetTimeInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getTime", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTime", int.class);
    doTest(rset, rowset, 1, new Time(1234L));
  }

  @Test
  public void testGetTimeString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getTime", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTime", String.class);
    doTest(rset, rowset, "test", new Time(1234L));
  }

  @Test
  public void testGetObjectInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getObject", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getObject", int.class);
    doTest(rset, rowset, 1, new Object());
  }

  @Test
  public void testGetObjectString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getObject", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getObject", String.class);
    doTest(rset, rowset, "test", new Object());
  }

  @Test
  public void testGetIntInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getInt", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getInt", int.class);
    doTest(rset, rowset, 1, 1);
  }

  @Test
  public void testGetIntString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getInt", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getInt", String.class);
    doTest(rset, rowset, "test", 1);
  }

  @Test
  public void testGetFloatInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getFloat", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getFloat", int.class);
    doTest(rset, rowset, 1, 1.0f);
  }

  @Test
  public void testGetFloatString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getFloat", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getFloat", String.class);
    doTest(rset, rowset, "test", 1.0f);
  }

  @Test
  public void testGetDoubleInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getDouble", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDouble", int.class);
    doTest(rset, rowset, 1, 1.0d);
  }

  @Test
  public void testGetDoubleString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getDouble", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDouble", String.class);
    doTest(rset, rowset, "test", 1.0d);
  }

  @Test
  public void testGetLongInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getLong", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getLong", int.class);
    doTest(rset, rowset, 1, 1L);
  }

  @Test
  public void testGetLongString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getLong", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getLong", String.class);
    doTest(rset, rowset, "test", 1L);
  }

  @Test
  public void testGetBooleanInt() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getBoolean", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBoolean", int.class);
    doTest(rset, rowset, 1, true);
  }

  @Test
  public void testGetBooleanString() throws Exception {
    Method rset = ResultSet.class.getDeclaredMethod("getBoolean", int.class);
    Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBoolean", String.class);
    doTest(rset, rowset, "test", true);
  }

  @Test
  void resolvesFirstMatchingLabelAndQualifiedNameFromMetadata() throws SQLException {
    var metadata = mock(ResultSetMetaData.class);
    given(resultSet.getMetaData()).willReturn(metadata);
    given(metadata.getColumnCount()).willReturn(3);
    given(metadata.getColumnLabel(1)).willReturn("alias");
    given(metadata.getColumnLabel(2)).willReturn("alias");
    given(metadata.getColumnLabel(3)).willReturn("other");
    given(metadata.getTableName(1)).willReturn("items");
    given(metadata.getTableName(2)).willReturn("items");
    given(metadata.getTableName(3)).willReturn("");
    given(metadata.getColumnName(1)).willReturn("id");
    given(metadata.getColumnName(2)).willReturn("id");
    var missing = new SQLException("unknown column");
    given(resultSet.findColumn("missing")).willThrow(missing);
    var wrapped = new ResultSetWrappingSqlRowSet(resultSet);

    assertThat(wrapped.getResultSet()).isSameAs(resultSet);
    assertThat(wrapped.getMetaData().getColumnCount()).isEqualTo(3);
    assertThat(wrapped.findColumn("alias")).isEqualTo(1);
    assertThat(wrapped.findColumn("items.id")).isEqualTo(1);
    assertThat(wrapped.findColumn("other")).isEqualTo(3);
    verify(resultSet, never()).findColumn("alias");
    assertThatExceptionOfType(InvalidResultSetAccessException.class)
            .isThrownBy(() -> wrapped.findColumn("missing")).withCause(missing);
  }

  @Test
  void translatesMetadataFailuresAtEitherStage() throws SQLException {
    var failure = new SQLException("metadata unavailable");
    var metadata = mock(ResultSetMetaData.class);
    given(resultSet.getMetaData()).willThrow(failure).willReturn(metadata);
    given(metadata.getColumnCount()).willThrow(failure);

    for (int i = 0; i < 2; i++) {
      assertThatExceptionOfType(InvalidResultSetAccessException.class)
              .isThrownBy(() -> new ResultSetWrappingSqlRowSet(resultSet)).withCause(failure);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cursorReads")
  void forwardsCursorOperationAndTranslatesFailure(String name, DriverRead driver,
          Function<SqlRowSet, ?> read, Object value) throws SQLException {
    var failure = new SQLException("cursor unavailable");
    given(driver.read(resultSet)).willReturn(value).willThrow(failure);

    assertThat(read.apply(rowSet)).isEqualTo(value);
    assertThatExceptionOfType(InvalidResultSetAccessException.class)
            .isThrownBy(() -> read.apply(rowSet)).withCause(failure);
  }

  static Stream<Arguments> cursorReads() {
    return Stream.of(
            operation("absolute", rs -> rs.absolute(3), rs -> rs.absolute(3), true),
            operation("first", ResultSet::first, SqlRowSet::first, true),
            operation("last", ResultSet::last, SqlRowSet::last, true),
            operation("next", ResultSet::next, SqlRowSet::next, true),
            operation("previous", ResultSet::previous, SqlRowSet::previous, true),
            operation("relative", rs -> rs.relative(-2), rs -> rs.relative(-2), true),
            operation("getRow", ResultSet::getRow, SqlRowSet::getRow, 3),
            operation("isAfterLast", ResultSet::isAfterLast, SqlRowSet::isAfterLast, true),
            operation("isBeforeFirst", ResultSet::isBeforeFirst, SqlRowSet::isBeforeFirst, true),
            operation("isFirst", ResultSet::isFirst, SqlRowSet::isFirst, true),
            operation("isLast", ResultSet::isLast, SqlRowSet::isLast, true),
            operation("wasNull", ResultSet::wasNull, SqlRowSet::wasNull, true));
  }

  @Test
  void forwardsBeforeAndAfterCursorMovesAndTranslatesFailures() throws SQLException {
    rowSet.beforeFirst();
    rowSet.afterLast();
    verify(resultSet).beforeFirst();
    verify(resultSet).afterLast();
    var failure = new SQLException("cursor unavailable");
    willThrow(failure).given(resultSet).beforeFirst();
    willThrow(failure).given(resultSet).afterLast();

    assertThatExceptionOfType(InvalidResultSetAccessException.class).isThrownBy(rowSet::beforeFirst).withCause(failure);
    assertThatExceptionOfType(InvalidResultSetAccessException.class).isThrownBy(rowSet::afterLast).withCause(failure);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("additionalColumnReads")
  void forwardsAdditionalLabelBasedReadsAndTranslatesFailure(String name, DriverRead driver,
          Function<SqlRowSet, ?> read, Object value) throws SQLException {
    given(resultSet.findColumn("value")).willReturn(2);
    forwardsCursorOperationAndTranslatesFailure(name, driver, read, value);
  }

  static Stream<Arguments> additionalColumnReads() {
    var calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    Map<String, Class<?>> map = Map.of("CUSTOM", String.class);
    return Stream.of(
            operation("byte", rs -> rs.getByte(2), rs -> rs.getByte("value"), (byte) 5),
            operation("short", rs -> rs.getShort(2), rs -> rs.getShort("value"), (short) 25),
            operation("national string", rs -> rs.getNString(2), rs -> rs.getNString("value"), "日本語"),
            operation("typed object", rs -> rs.getObject(2, String.class), rs -> rs.getObject("value", String.class), "typed"),
            operation("mapped object", rs -> rs.getObject(2, map), rs -> rs.getObject("value", map), "mapped"),
            operation("date with calendar", rs -> rs.getDate(2, calendar), rs -> rs.getDate("value", calendar), new Date(1234L)),
            operation("time with calendar", rs -> rs.getTime(2, calendar), rs -> rs.getTime("value", calendar), new Time(1234L)),
            operation("timestamp with calendar", rs -> rs.getTimestamp(2, calendar),
                    rs -> rs.getTimestamp("value", calendar), new Timestamp(1234L)));
  }

  private static Arguments operation(String name, DriverRead driver, Function<SqlRowSet, ?> read, Object value) {
    return Arguments.of(name, driver, read, value);
  }

  @FunctionalInterface
  private interface DriverRead {

    Object read(ResultSet resultSet) throws SQLException;
  }

  private void doTest(Method rsetMethod, Method rowsetMethod, Object arg, Object ret) throws Exception {
    if (arg instanceof String) {
      given(resultSet.findColumn((String) arg)).willReturn(1);
      given(rsetMethod.invoke(resultSet, 1)).willReturn(ret).willThrow(new SQLException("test"));
    }
    else {
      given(rsetMethod.invoke(resultSet, arg)).willReturn(ret).willThrow(new SQLException("test"));
    }
    assertThat(rowsetMethod.invoke(rowSet, arg)).isEqualTo(ret);
    assertThatExceptionOfType(InvocationTargetException.class).isThrownBy(() ->
                    rowsetMethod.invoke(rowSet, arg)).
            satisfies(ex -> Assertions.assertThat(ex.getTargetException()).isExactlyInstanceOf(InvalidResultSetAccessException.class));
  }

}
