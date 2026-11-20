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

package infra.jdbc.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import infra.jdbc.CannotGetJdbcConnectionException;
import infra.jdbc.datasource.ConnectionHolder;
import infra.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link JdbcUtils}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class JdbcUtilsTests {

  @Test
  public void commonDatabaseName() {
    assertThat(JdbcUtils.commonDatabaseName("Oracle")).isEqualTo("Oracle");
    assertThat(JdbcUtils.commonDatabaseName("DB2-for-Spring")).isEqualTo("DB2");
    assertThat(JdbcUtils.commonDatabaseName("Sybase SQL Server")).isEqualTo("Sybase");
    assertThat(JdbcUtils.commonDatabaseName("Adaptive Server Enterprise")).isEqualTo("Sybase");
    assertThat(JdbcUtils.commonDatabaseName("MySQL")).isEqualTo("MySQL");
    assertThat(JdbcUtils.commonDatabaseName("MariaDB")).isEqualTo("MySQL");
    assertThat(JdbcUtils.commonDatabaseName("ASE")).isEqualTo("Sybase");
    assertThat(JdbcUtils.commonDatabaseName("SQL Server")).isEqualTo("Sybase");
    assertThat(JdbcUtils.commonDatabaseName(null)).isNull();
  }

  @Test
  public void resolveTypeName() {
    assertThat(JdbcUtils.resolveTypeName(Types.VARCHAR)).isEqualTo("VARCHAR");
    assertThat(JdbcUtils.resolveTypeName(Types.NUMERIC)).isEqualTo("NUMERIC");
    assertThat(JdbcUtils.resolveTypeName(Types.INTEGER)).isEqualTo("INTEGER");
    assertThat(JdbcUtils.resolveTypeName(JdbcUtils.TYPE_UNKNOWN)).isNull();
  }

  @Test
  public void convertUnderscoreNameToPropertyName() {
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("MY_NAME")).isEqualTo("myName");
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("yOUR_nAME")).isEqualTo("yourName");
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("a_name")).isEqualTo("AName");
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("someone_elses_name")).isEqualTo("someoneElsesName");
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName(null)).isEmpty();
    assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("X")).isEqualTo("x");
  }

  @ParameterizedTest
  @ValueSource(strings = { "`name`", "\"name\"", "[name]" })
  void removesMatchingIdentifierQuotes(String name) {
    assertThat(JdbcUtils.unquoteIdentifier(name)).isEqualTo("name");
    assertThat(JdbcUtils.isQuotedIdentifier("(" + name + ")", 1, name.length() + 1)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = { "plain", "[]", "`name]", "\"name`", "[name\"" })
  void preservesUnquotedIdentifiers(String name) {
    assertThat(JdbcUtils.unquoteIdentifier(name)).isEqualTo(name);
  }

  @ParameterizedTest
  @ValueSource(ints = { Types.BIT, Types.BIGINT, Types.DECIMAL, Types.DOUBLE, Types.FLOAT,
          Types.INTEGER, Types.NUMERIC, Types.REAL, Types.SMALLINT, Types.TINYINT })
  void recognizesNumericTypes(int type) {
    assertThat(JdbcUtils.isNumeric(type)).isTrue();
  }

  @Test
  void usesColumnNameIfDriverHasNoLabel() throws SQLException {
    var metadata = mock(ResultSetMetaData.class);
    given(metadata.getColumnLabel(2)).willReturn("");
    given(metadata.getColumnName(2)).willReturn("NAME");

    assertThat(JdbcUtils.lookupColumnName(metadata, 2)).isEqualTo("NAME");
    assertThat(JdbcUtils.isNumeric(Types.VARCHAR)).isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("typedColumns")
  void usesTheMatchingJdbcGetter(Class<?> type, ColumnReader getter, Object value) throws SQLException {
    var resultSet = mock(ResultSet.class);
    given(getter.read(resultSet, 2)).willReturn(value);

    assertThat(JdbcUtils.getResultSetValue(resultSet, 2, type)).isEqualTo(value);
  }

  static Stream<Arguments> typedColumns() throws SQLException {
    return Stream.of(
            column(String.class, ResultSet::getString, "text"),
            column(boolean.class, ResultSet::getBoolean, true),
            column(Boolean.class, ResultSet::getBoolean, true),
            column(byte.class, ResultSet::getByte, (byte) 2),
            column(Byte.class, ResultSet::getByte, (byte) 2),
            column(short.class, ResultSet::getShort, (short) 3),
            column(Short.class, ResultSet::getShort, (short) 3),
            column(int.class, ResultSet::getInt, 4),
            column(Integer.class, ResultSet::getInt, 4),
            column(long.class, ResultSet::getLong, 5L),
            column(Long.class, ResultSet::getLong, 5L),
            column(float.class, ResultSet::getFloat, 6.5f),
            column(Float.class, ResultSet::getFloat, 6.5f),
            column(double.class, ResultSet::getDouble, 7.5),
            column(Double.class, ResultSet::getDouble, 7.5),
            column(Number.class, ResultSet::getDouble, 7.5),
            column(BigDecimal.class, ResultSet::getBigDecimal, new BigDecimal("23.42")),
            column(Date.class, ResultSet::getDate, Date.valueOf("2026-01-02")),
            column(Time.class, ResultSet::getTime, Time.valueOf("03:04:05")),
            column(Timestamp.class, ResultSet::getTimestamp, Timestamp.valueOf("2026-01-02 03:04:05")),
            column(java.util.Date.class, ResultSet::getTimestamp, Timestamp.valueOf("2026-01-02 03:04:05")),
            column(byte[].class, ResultSet::getBytes, new byte[] { 1, 2 }),
            column(Blob.class, ResultSet::getBlob, new SerialBlob(new byte[] { 1, 2 })),
            column(Clob.class, ResultSet::getClob, new SerialClob(new char[] { 'a', 'b' })));
  }

  @Test
  void preservesSqlNullWhenDriverReturnsPrimitiveDefault() throws SQLException {
    var resultSet = mock(ResultSet.class);
    given(resultSet.wasNull()).willReturn(true);

    assertThat(JdbcUtils.getResultSetValue(resultSet, 2, Integer.class)).isNull();
    verify(resultSet).getInt(2);
  }

  @Test
  void exposesEnumRepresentationsForLaterConversion() throws SQLException {
    var resultSet = mock(ResultSet.class);
    given(resultSet.getObject(1)).willReturn("NEW", 1L, new Object());
    given(resultSet.getString(1)).willReturn("RUNNABLE");

    assertThat(JdbcUtils.getResultSetValue(resultSet, 1, Thread.State.class)).isEqualTo("NEW");
    assertThat(JdbcUtils.getResultSetValue(resultSet, 1, Thread.State.class)).isEqualTo(1);
    assertThat(JdbcUtils.getResultSetValue(resultSet, 1, Thread.State.class)).isEqualTo("RUNNABLE");
  }

  @Test
  void fallsBackForDriversWithoutTypedObjectSupport() throws SQLException {
    var resultSet = mock(ResultSet.class);
    var date = Date.valueOf("2026-01-02");
    var time = Time.valueOf("03:04:05");
    var timestamp = Timestamp.valueOf("2026-01-02 03:04:05");
    var id = UUID.fromString("daf53360-52da-42e2-9794-f0166ffdb8e9");
    given(resultSet.getObject(1, LocalDate.class)).willThrow(new AbstractMethodError());
    given(resultSet.getObject(1, LocalTime.class)).willThrow(new SQLFeatureNotSupportedException());
    given(resultSet.getObject(1, LocalDateTime.class)).willThrow(new SQLException("unsupported conversion"));
    given(resultSet.getObject(1, UUID.class)).willReturn(id).willThrow(new SQLFeatureNotSupportedException());
    given(resultSet.getObject(1)).willReturn(id.toString());
    given(resultSet.getDate(1)).willReturn(date);
    given(resultSet.getTime(1)).willReturn(time);
    given(resultSet.getTimestamp(1)).willReturn(timestamp);

    assertThat(JdbcUtils.getResultSetValue(resultSet, 1, LocalDate.class)).isEqualTo(date);
    assertThat(JdbcUtils.getResultSetValue(resultSet, 1, LocalTime.class)).isEqualTo(time);
    assertThat(JdbcUtils.getResultSetValue(resultSet, 1, LocalDateTime.class)).isEqualTo(timestamp);
    assertThat(JdbcUtils.getResultSetValue(resultSet, 1, UUID.class)).isEqualTo(id);
    assertThat(JdbcUtils.getResultSetValue(resultSet, 1, UUID.class)).isEqualTo(id.toString());
  }

  @Test
  void materializesUntypedLobs() throws SQLException {
    var resultSet = mock(ResultSet.class);
    given(resultSet.getObject(1)).willReturn(new SerialBlob(new byte[] { 1, 2 }));
    given(resultSet.getObject(2)).willReturn(new SerialClob(new char[] { 'a', 'b' }));

    assertThat(JdbcUtils.getResultSetValue(resultSet, 1, null)).isEqualTo(new byte[] { 1, 2 });
    assertThat(JdbcUtils.getResultSetValue(resultSet, 2)).isEqualTo("ab");
  }

  @Test
  void convertsSqlDateToTimestampWhenMetadataReportsTimestamp() throws SQLException {
    var resultSet = mock(ResultSet.class);
    var metadata = mock(ResultSetMetaData.class);
    var date = Date.valueOf("2026-01-02");
    var timestamp = Timestamp.valueOf("2026-01-02 03:04:05");
    given(resultSet.getObject(1)).willReturn(date);
    given(resultSet.getMetaData()).willReturn(metadata);
    given(metadata.getColumnClassName(1)).willReturn("java.sql.Timestamp");
    given(resultSet.getTimestamp(1)).willReturn(timestamp);

    assertThat(JdbcUtils.getResultSetValue(resultSet, 1)).isSameAs(timestamp);
  }

  @Test
  void closesAllResourcesInOrderDespiteSqlExceptions() throws SQLException {
    var resultSet = mock(ResultSet.class);
    var statement = mock(Statement.class);
    var connection = mock(Connection.class);
    willThrow(new SQLException("result set")).given(resultSet).close();
    willThrow(new SQLException("statement")).given(statement).close();
    willThrow(new SQLException("connection")).given(connection).close();

    assertThatCode(() -> JdbcUtils.closeQuietly(connection, statement, resultSet)).doesNotThrowAnyException();
    assertThatCode(() -> JdbcUtils.closeQuietly(null, null, null)).doesNotThrowAnyException();

    InOrder order = inOrder(resultSet, statement, connection);
    order.verify(resultSet).close();
    order.verify(statement).close();
    order.verify(connection).close();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void stillClosesConnectionAfterFailedCommitOrRollback(boolean commit) throws SQLException {
    var connection = mock(Connection.class);
    var failure = new SQLException("transaction failed");
    var closeFailure = new SQLException("close failed");
    willThrow(closeFailure).given(connection).close();
    if (commit) {
      willThrow(failure).given(connection).commit();
      assertThatThrownBy(() -> JdbcUtils.commitAndClose(connection)).isSameAs(failure);
      JdbcUtils.commitAndCloseQuietly(connection);
      JdbcUtils.commitAndClose(null);
    }
    else {
      willThrow(failure).given(connection).rollback();
      assertThatThrownBy(() -> JdbcUtils.rollbackAndClose(connection)).isSameAs(failure);
      JdbcUtils.rollbackAndCloseQuietly(connection);
      JdbcUtils.rollbackAndClose(null);
    }
    assertThat(failure.getSuppressed()).contains(closeFailure);
  }

  @Test
  void printsWarningChainAndHandlesFailureToReadWarnings() throws SQLException {
    var connection = mock(Connection.class);
    var warning = new SQLWarning("first warning");
    warning.setNextWarning(new SQLWarning("second warning"));
    given(connection.getWarnings()).willReturn(warning).willThrow(new SQLException("driver failure"));
    var buffer = new StringWriter();
    var writer = new PrintWriter(buffer);

    JdbcUtils.printWarnings(connection, writer);
    JdbcUtils.printWarnings(connection, writer);
    JdbcUtils.printWarnings(null, writer);

    assertThat(buffer.toString()).containsSubsequence("first warning", "Next SQLException:", "second warning", "driver failure");
  }

  @Test
  void retriesMetadataWithUnboundConnectionButLeavesTransactionConnectionOpen() throws Exception {
    var source = mock(DataSource.class);
    var transactional = mock(Connection.class);
    var fresh = mock(Connection.class);
    var metadata = mock(DatabaseMetaData.class);
    given(source.getConnection()).willReturn(fresh);
    given(transactional.getMetaData()).willThrow(new SQLException("transactional metadata unavailable"));
    given(fresh.getMetaData()).willReturn(metadata);
    given(metadata.getDatabaseProductName()).willReturn("example");
    TransactionSynchronizationManager.bindResource(source, new ConnectionHolder(transactional));
    try {
      String product = JdbcUtils.extractDatabaseMetaData(source, DatabaseMetaData::getDatabaseProductName);
      assertThat(product).isEqualTo("example");
      verify(transactional, never()).close();
      verify(fresh).close();
    }
    finally {
      TransactionSynchronizationManager.unbindResource(source);
    }
  }

  @Test
  void reportsMetadataFailuresWithoutRetryOutsideTransaction() throws SQLException {
    var source = mock(DataSource.class);
    var connection = mock(Connection.class);
    var failure = new SQLException("metadata unavailable");
    given(source.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willThrow(failure);

    assertThatThrownBy(() -> JdbcUtils.extractDatabaseMetaData(source, DatabaseMetaData::getDatabaseProductName))
            .isInstanceOf(MetaDataAccessException.class).hasCause(failure);
    verify(source).getConnection();
    verify(connection).close();
  }

  @Test
  void distinguishesMissingMetadataFromUnavailableConnection() throws SQLException {
    var source = mock(DataSource.class);
    var connection = mock(Connection.class);
    var failure = new SQLException("connection unavailable");
    given(source.getConnection()).willReturn(connection).willThrow(failure);

    assertThatThrownBy(() -> JdbcUtils.extractDatabaseMetaData(source, DatabaseMetaData::getDatabaseProductName))
            .isInstanceOf(MetaDataAccessException.class).hasMessageContaining("was null");
    assertThatThrownBy(() -> JdbcUtils.extractDatabaseMetaData(source, DatabaseMetaData::getDatabaseProductName))
            .isInstanceOf(MetaDataAccessException.class).hasCauseInstanceOf(CannotGetJdbcConnectionException.class)
            .hasRootCause(failure);
    verify(connection).close();
  }

  @Test
  void reportsUnsupportedMetadataMethodAndReleasesConnection() throws SQLException {
    var source = mock(DataSource.class);
    var connection = mock(Connection.class);
    var metadata = mock(DatabaseMetaData.class);
    var failure = new AbstractMethodError("old JDBC driver");
    given(source.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(metadata);
    given(metadata.getDatabaseProductName()).willThrow(failure);

    assertThatThrownBy(() -> JdbcUtils.extractDatabaseMetaData(source, DatabaseMetaData::getDatabaseProductName))
            .isInstanceOf(MetaDataAccessException.class).hasMessageContaining("not implemented").hasCause(failure);
    verify(connection).close();
  }

  private static Arguments column(Class<?> type, ColumnReader reader, Object value) {
    return Arguments.of(type, reader, value);
  }

  @FunctionalInterface
  private interface ColumnReader {

    Object read(ResultSet resultSet, int index) throws SQLException;
  }

}
