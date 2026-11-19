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

package infra.jdbc.core;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.stream.Stream;

import infra.jdbc.support.SqlValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 31.08.2004
 */
public class StatementCreatorUtilsTests {

  private final PreparedStatement preparedStatement = mock(PreparedStatement.class);

  @Nullable
  private Boolean previousIgnoreGetParameterType;

  @BeforeEach
  void saveParameterMetadataSetting() {
    previousIgnoreGetParameterType = StatementCreatorUtils.shouldIgnoreGetParameterType;
    StatementCreatorUtils.shouldIgnoreGetParameterType = false;
  }

  @AfterEach
  void restoreParameterMetadataSetting() {
    StatementCreatorUtils.shouldIgnoreGetParameterType = previousIgnoreGetParameterType;
  }

  @Test
  public void testSetParameterValueWithNullAndType() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, null, null);
    verify(preparedStatement).setNull(1, Types.VARCHAR);
  }

  @Test
  public void testSetParameterValueWithNullAndTypeName() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, "mytype", null);
    verify(preparedStatement).setNull(1, Types.VARCHAR, "mytype");
  }

  @Test
  public void testSetParameterValueWithNullAndUnknownType() throws SQLException {
    StatementCreatorUtils.shouldIgnoreGetParameterType = true;
    Connection con = mock();
    DatabaseMetaData dbmd = mock();
    given(preparedStatement.getConnection()).willReturn(con);
    given(dbmd.getDatabaseProductName()).willReturn("Oracle");
    given(dbmd.getDriverName()).willReturn("Oracle Driver");
    given(con.getMetaData()).willReturn(dbmd);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
    verify(preparedStatement).setNull(1, Types.NULL);
  }

  @Test
  public void testSetParameterValueWithNullAndUnknownTypeOnInformix() throws SQLException {
    StatementCreatorUtils.shouldIgnoreGetParameterType = true;
    Connection con = mock();
    DatabaseMetaData dbmd = mock();
    given(preparedStatement.getConnection()).willReturn(con);
    given(con.getMetaData()).willReturn(dbmd);
    given(dbmd.getDatabaseProductName()).willReturn("Informix Dynamic Server");
    given(dbmd.getDriverName()).willReturn("Informix Driver");
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
    verify(dbmd).getDatabaseProductName();
    verify(dbmd).getDriverName();
    verify(preparedStatement).setObject(1, null);
  }

  @Test
  public void testSetParameterValueWithNullAndGetParameterTypeWorking() throws SQLException {
    ParameterMetaData pmd = mock();
    given(preparedStatement.getParameterMetaData()).willReturn(pmd);
    given(pmd.getParameterType(1)).willReturn(Types.SMALLINT);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
    verify(pmd).getParameterType(1);
    verify(preparedStatement, never()).getConnection();
    verify(preparedStatement).setNull(1, Types.SMALLINT);
  }

  @Test
  public void testSetParameterValueWithString() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, null, "test");
    verify(preparedStatement).setString(1, "test");
  }

  @Test
  public void testSetParameterValueWithStringAndSpecialType() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.CHAR, null, "test");
    verify(preparedStatement).setObject(1, "test", Types.CHAR);
  }

  @Test
  public void testSetParameterValueWithStringAndUnknownType() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, "test");
    verify(preparedStatement).setString(1, "test");
  }

  @Test
  public void testSetParameterValueWithSqlDate() throws SQLException {
    java.sql.Date date = new java.sql.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DATE, null, date);
    verify(preparedStatement).setDate(1, date);
  }

  @Test
  public void testSetParameterValueWithDateAndUtilDate() throws SQLException {
    java.util.Date date = new java.util.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DATE, null, date);
    verify(preparedStatement).setDate(1, new java.sql.Date(1000));
  }

  @Test
  public void testSetParameterValueWithDateAndCalendar() throws SQLException {
    java.util.Calendar cal = new GregorianCalendar();
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DATE, null, cal);
    verify(preparedStatement).setDate(1, new java.sql.Date(cal.getTime().getTime()), cal);
  }

  @Test
  public void testSetParameterValueWithSqlTime() throws SQLException {
    java.sql.Time time = new java.sql.Time(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIME, null, time);
    verify(preparedStatement).setTime(1, time);
  }

  @Test
  public void testSetParameterValueWithTimeAndUtilDate() throws SQLException {
    java.util.Date date = new java.util.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIME, null, date);
    verify(preparedStatement).setTime(1, new java.sql.Time(1000));
  }

  @Test
  public void testSetParameterValueWithTimeAndCalendar() throws SQLException {
    java.util.Calendar cal = new GregorianCalendar();
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIME, null, cal);
    verify(preparedStatement).setTime(1, new java.sql.Time(cal.getTime().getTime()), cal);
  }

  @Test
  public void testSetParameterValueWithSqlTimestamp() throws SQLException {
    java.sql.Timestamp timestamp = new java.sql.Timestamp(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP, null, timestamp);
    verify(preparedStatement).setTimestamp(1, timestamp);
  }

  @Test
  public void testSetParameterValueWithTimestampAndUtilDate() throws SQLException {
    java.util.Date date = new java.util.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP, null, date);
    verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(1000));
  }

  @Test
  public void testSetParameterValueWithTimestampAndCalendar() throws SQLException {
    java.util.Calendar cal = new GregorianCalendar();
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP, null, cal);
    verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(cal.getTime().getTime()), cal);
  }

  @Test
  public void testSetParameterValueWithDateAndUnknownType() throws SQLException {
    java.util.Date date = new java.util.Date(1000);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, date);
    verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(1000));
  }

  @Test
  public void testSetParameterValueWithCalendarAndUnknownType() throws SQLException {
    java.util.Calendar cal = new GregorianCalendar();
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, cal);
    verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(cal.getTime().getTime()), cal);
  }

  @ParameterizedTest
  @MethodSource("javaTimeTypes")
  public void testSetParameterValueWithJavaTimeTypes(Object o, int sqlType) throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, sqlType, null, o);
    verify(preparedStatement).setObject(1, o, sqlType);
  }

  @ParameterizedTest
  @MethodSource("javaTimeTypes")
  void javaTimeTypesToSqlParameterType(Object o, int expectedSqlType) {
    assertThat(StatementCreatorUtils.javaTypeToSqlParameterType(o.getClass()))
            .isEqualTo(expectedSqlType);
  }

  static Stream<Arguments> javaTimeTypes() {
    ZoneOffset PLUS_NINE = ZoneOffset.ofHours(9);
    final LocalDateTime now = LocalDateTime.now();
    return Stream.of(
            Arguments.of(named("LocalTime", LocalTime.NOON), named("TIME", Types.TIME)),
            Arguments.of(named("LocalDate", LocalDate.EPOCH), named("DATE", Types.DATE)),
            Arguments.of(named("LocalDateTime", now), named("TIMESTAMP", Types.TIMESTAMP)),
            Arguments.of(named("OffsetTime", LocalTime.NOON.atOffset(PLUS_NINE)),
                    named("TIME_WITH_TIMEZONE", Types.TIME_WITH_TIMEZONE)),
            Arguments.of(named("OffsetDateTime", now.atOffset(PLUS_NINE)),
                    named("TIMESTAMP_WITH_TIMEZONE", Types.TIMESTAMP_WITH_TIMEZONE))
    );
  }

  @Test  // gh-30556
  public void testSetParameterValueWithOffsetDateTimeAndNotSupported() throws SQLException {
    OffsetDateTime time = OffsetDateTime.now();
    doThrow(new SQLFeatureNotSupportedException()).when(preparedStatement).setObject(1, time, Types.TIMESTAMP_WITH_TIMEZONE);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP_WITH_TIMEZONE, null, time);
    verify(preparedStatement).setObject(1, time, Types.TIMESTAMP_WITH_TIMEZONE);
    verify(preparedStatement).setObject(1, time);
  }

  @Test  // gh-30556
  public void testSetParameterValueWithNullAndNotSupported() throws SQLException {
    doThrow(new SQLFeatureNotSupportedException()).when(preparedStatement).setNull(1, Types.TIMESTAMP_WITH_TIMEZONE);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP_WITH_TIMEZONE, null, null);
    verify(preparedStatement).setNull(1, Types.TIMESTAMP_WITH_TIMEZONE);
    verify(preparedStatement).setNull(1, Types.NULL);
  }

  @Test  //
  public void testSetParameterValueWithStringAndVendorSpecificType() throws SQLException {
    Connection con = mock();
    DatabaseMetaData dbmd = mock();
    given(preparedStatement.getConnection()).willReturn(con);
    given(dbmd.getDatabaseProductName()).willReturn("Oracle");
    given(con.getMetaData()).willReturn(dbmd);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.OTHER, null, "test");
    verify(preparedStatement).setString(1, "test");
  }

  @Test  //
  public void testSetParameterValueWithNullAndVendorSpecificType() throws SQLException {
    StatementCreatorUtils.shouldIgnoreGetParameterType = true;
    Connection con = mock();
    DatabaseMetaData dbmd = mock();
    given(preparedStatement.getConnection()).willReturn(con);
    given(dbmd.getDatabaseProductName()).willReturn("Oracle");
    given(dbmd.getDriverName()).willReturn("Oracle Driver");
    given(con.getMetaData()).willReturn(dbmd);
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.OTHER, null, null);
    verify(preparedStatement).setNull(1, Types.NULL);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("driversForUnknownNull")
  void bindsUnknownNullUsingDriverFallback(String driver, String product, boolean lookup, int expectedType)
          throws SQLException {
    StatementCreatorUtils.shouldIgnoreGetParameterType = null;
    Connection connection = mock();
    DatabaseMetaData metadata = mock();
    ParameterMetaData parameterMetadata = mock();
    given(preparedStatement.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(metadata);
    given(metadata.getDriverName()).willReturn(driver);
    given(metadata.getDatabaseProductName()).willReturn(product);
    given(preparedStatement.getParameterMetaData()).willReturn(parameterMetadata);
    given(parameterMetadata.getParameterType(1)).willThrow(new SQLException("metadata unavailable"));

    StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null);

    if (lookup) {
      verify(parameterMetadata).getParameterType(1);
    }
    else {
      verify(preparedStatement, never()).getParameterMetaData();
    }
    if (expectedType == SqlTypeValue.TYPE_UNKNOWN) {
      verify(preparedStatement).setObject(1, null);
    }
    else {
      verify(preparedStatement).setNull(1, expectedType);
    }
  }

  static Stream<Arguments> driversForUnknownNull() {
    return Stream.of(
            Arguments.of("PostgreSQL JDBC Driver", "PostgreSQL", false, Types.NULL),
            Arguments.of("Microsoft JDBC Driver for SQL Server", "Microsoft SQL Server", false, SqlTypeValue.TYPE_UNKNOWN),
            Arguments.of("Microsoft JDBC Driver", "OtherDB", true, Types.NULL),
            Arguments.of("IBM driver", "DB2/Linux", true, Types.VARCHAR),
            Arguments.of("jConnect", "Sybase", true, Types.VARCHAR),
            Arguments.of("SQLServer legacy driver", "SQL Server", true, Types.VARCHAR),
            Arguments.of("Generic driver", "OtherDB", true, Types.NULL));
  }

  @Test
  void doesNotRetryUnsupportedGenericNull() throws SQLException {
    var failure = new SQLFeatureNotSupportedException("NULL not supported");
    doThrow(failure).when(preparedStatement).setNull(1, Types.NULL);

    assertThatThrownBy(() -> StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.NULL, null))
            .isSameAs(failure);
    verify(preparedStatement).setNull(1, Types.NULL);
  }

  @Test
  void runtimeParameterOverridesDeclaredTypeAndTypeNameIndependently() throws SQLException {
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR,
            new SqlParameterValue(Types.NVARCHAR, "text"));
    StatementCreatorUtils.setParameterValue(preparedStatement, 2, Types.OTHER, "declared",
            new SqlParameterValue(SqlTypeValue.TYPE_UNKNOWN, "runtime", null));
    StatementCreatorUtils.setParameterValue(preparedStatement, 3, Types.OTHER, "declared",
            new SqlParameterValue(SqlTypeValue.TYPE_UNKNOWN, null));

    verify(preparedStatement).setNString(1, "text");
    verify(preparedStatement).setNull(2, Types.OTHER, "runtime");
    verify(preparedStatement).setNull(3, Types.OTHER, "declared");
  }

  @Test
  void choosesLobBindingByLengthAndNationalCharacterType() throws Exception {
    String shortText = "x".repeat(4000);
    String longText = shortText + "y";
    var clobText = ArgumentCaptor.forClass(Reader.class);
    var nclobText = ArgumentCaptor.forClass(Reader.class);

    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.CLOB, shortText);
    StatementCreatorUtils.setParameterValue(preparedStatement, 2, Types.NCLOB, shortText);
    StatementCreatorUtils.setParameterValue(preparedStatement, 3, Types.CLOB, longText);
    StatementCreatorUtils.setParameterValue(preparedStatement, 4, Types.NCLOB, longText);

    verify(preparedStatement).setString(1, shortText);
    verify(preparedStatement).setNString(2, shortText);
    verify(preparedStatement).setClob(eq(3), clobText.capture(), eq((long) longText.length()));
    verify(preparedStatement).setNClob(eq(4), nclobText.capture(), eq((long) longText.length()));
    try (var clobReader = clobText.getValue(); var nclobReader = nclobText.getValue();
            var clob = new StringWriter(); var nclob = new StringWriter()) {
      clobReader.transferTo(clob);
      nclobReader.transferTo(nclob);
      assertThat(clob.toString()).isEqualTo(longText);
      assertThat(nclob.toString()).isEqualTo(longText);
    }
  }

  @Test
  void bindsNumericBooleanAndBinaryValuesWithoutStringCoercion() throws SQLException {
    var amount = new BigDecimal("12.34");
    byte[] bytes = { 1, 2 };
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DECIMAL, amount);
    StatementCreatorUtils.setParameterValue(preparedStatement, 2, new SqlParameter(Types.NUMERIC, 2), "12.34");
    StatementCreatorUtils.setParameterValue(preparedStatement, 3, Types.BOOLEAN, true);
    StatementCreatorUtils.setParameterValue(preparedStatement, 4, Types.BOOLEAN, 1);
    StatementCreatorUtils.setParameterValue(preparedStatement, 5, SqlTypeValue.TYPE_UNKNOWN, bytes);

    verify(preparedStatement).setBigDecimal(1, amount);
    verify(preparedStatement).setObject(2, "12.34", Types.NUMERIC, 2);
    verify(preparedStatement).setBoolean(3, true);
    verify(preparedStatement).setObject(4, 1, Types.BOOLEAN);
    verify(preparedStatement).setBytes(5, bytes);
  }

  @Test
  void delegatesCustomBindingsAndCleansUpWrappedValues() throws SQLException {
    SqlValue value = mock();
    DisposableSqlTypeValue typedValue = mock();
    StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.OTHER, value);
    StatementCreatorUtils.setParameterValue(preparedStatement, 2, Types.OTHER, "custom", typedValue);
    StatementCreatorUtils.cleanupParameters(new SqlParameterValue(Types.OTHER, value), typedValue, "plain", null);

    verify(value).setValue(preparedStatement, 1);
    verify(value).cleanup();
    verify(typedValue).setTypeValue(preparedStatement, 2, Types.OTHER, "custom");
    verify(typedValue).cleanup();
    verify(preparedStatement, never()).setObject(eq(1), any());
  }

}
