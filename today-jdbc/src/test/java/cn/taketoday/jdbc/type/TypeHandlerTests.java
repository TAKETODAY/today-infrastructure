/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.type;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.ThrowingBiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/6/15 11:15
 */
class TypeHandlerTests {

  protected final ResultSet resultSet = mock(ResultSet.class);

  protected final CallableStatement callableStatement = mock(CallableStatement.class);

  protected final PreparedStatement preparedStatement = mock(PreparedStatement.class);

  interface PreparedStatementFunc<T> {

    void accept(PreparedStatement statement, int parameterIndex, T value) throws SQLException;

  }

  @ParameterizedTest
  @MethodSource("setParameterArgumentSource")
  <T> void setParameter(TypeHandler<T> typeHandler, PreparedStatementFunc<T> consumer, T value, T verifyVal) throws SQLException {
    typeHandler.setParameter(preparedStatement, 0, value);
    consumer.accept(verify(preparedStatement), 0, verifyVal);

    typeHandler.setParameter(preparedStatement, 0, null);
    verify(preparedStatement).setObject(0, null);
  }

  @ParameterizedTest
  @MethodSource("getResultColumnIndexArgumentSource")
  <T> void getResultColumnIndex(TypeHandler<T> typeHandler, BiFunction<ResultSet, Integer, T> consumer,
          T value, T verifyVal, boolean wasNull) throws SQLException {
    given(consumer.apply(resultSet, 1)).willReturn(value);
    given(resultSet.wasNull()).willReturn(wasNull);

    T result = typeHandler.getResult(resultSet, 1);
    assertThat(result).isEqualTo(verifyVal);
  }

  @ParameterizedTest
  @MethodSource("getResultColumnNameArgumentSource")
  <T> void getResultColumnName(TypeHandler<T> typeHandler, BiFunction<ResultSet, String, T> consumer,
          T value, T verifyVal, boolean wasNull) throws SQLException {
    given(consumer.apply(resultSet, "columnName")).willReturn(value);
    given(resultSet.wasNull()).willReturn(wasNull);

    T result = typeHandler.getResult(resultSet, "columnName");
    assertThat(result).isEqualTo(verifyVal);
  }

  @ParameterizedTest
  @MethodSource("getResultColumnIndexFromCallableStatementArgumentSource")
  <T> void getResultColumnIndexFromCallableStatement(TypeHandler<T> typeHandler,
          BiFunction<CallableStatement, Integer, T> consumer, T value, T verifyVal, boolean wasNull) throws SQLException {
    given(consumer.apply(callableStatement, 1)).willReturn(value);
    given(callableStatement.wasNull()).willReturn(wasNull);

    T result = typeHandler.getResult(callableStatement, 1);
    assertThat(result).isEqualTo(verifyVal);
  }

  public static Stream<Arguments> getResultColumnIndexFromCallableStatementArgumentSource() {
    UUID uuid = UUID.randomUUID();
    java.util.Date date = java.util.Date.from(Instant.now());

    OffsetDateTime offsetDateTime = OffsetDateTime.now();
    return Stream.of(
            call(new LongTypeHandler(), CallableStatement::getLong, 1L),
            call(new IntegerTypeHandler(), CallableStatement::getInt, 1),
            call(new DoubleTypeHandler(), CallableStatement::getDouble, 1D),
            call(new FloatTypeHandler(), CallableStatement::getFloat, 1f),
            call(new BooleanTypeHandler(), CallableStatement::getBoolean, true),
            call(new ByteArrayTypeHandler(), CallableStatement::getBytes, new byte[] { 1 }),
            call(new ByteTypeHandler(), CallableStatement::getByte, (byte) 1),
            call(new ObjectTypeHandler(), CallableStatement::getObject, 1),
            call(new ShortTypeHandler(), CallableStatement::getShort, (short) 1),
            call(new StringTypeHandler(), CallableStatement::getString, "0001"),
            call(new SqlTimeTypeHandler(), CallableStatement::getTime, Time.valueOf(LocalTime.now())),
            call(new SqlDateTypeHandler(), CallableStatement::getDate, Date.valueOf(LocalDate.now())),
            call(new SqlTimestampTypeHandler(), CallableStatement::getTimestamp, Timestamp.from(Instant.now())),

            call(new DurationTypeHandler(), CallableStatement::getLong, Duration.ofDays(1).toNanos(), Duration.ofDays(1)),
            call(new DurationTypeHandler(), CallableStatement::getLong, 0L, Duration.ZERO),
            call(new DurationTypeHandler(), CallableStatement::getLong, 0L, null, true),

            call(new InstantTypeHandler(), CallableStatement::getTimestamp, null, null),
            call(new InstantTypeHandler(), CallableStatement::getTimestamp, Timestamp.from(Instant.EPOCH), Instant.EPOCH),

            call(new DateTypeHandler(), CallableStatement::getTimestamp, new Timestamp(new Date(1).getTime()), new Date(1)),
            call(new DateTypeHandler(), CallableStatement::getTimestamp, null),

            call(new CharacterTypeHandler(), CallableStatement::getString, "1", '1'),
            call(new CharacterTypeHandler(), CallableStatement::getString, null, null),

            call(new BigIntegerTypeHandler(), CallableStatement::getBigDecimal, new BigDecimal(BigInteger.valueOf(1)), BigInteger.valueOf(1)),
            call(new BigIntegerTypeHandler(), CallableStatement::getBigDecimal, null, null),

            call(new UUIDTypeHandler(), CallableStatement::getString, uuid.toString(), uuid),
            call(new UUIDTypeHandler(), CallableStatement::getString, null, null),
            call(new UUIDTypeHandler(), CallableStatement::getString, "", null),

            call(new YearTypeHandler(), CallableStatement::getInt, Year.MIN_VALUE, Year.of(Year.MIN_VALUE)),
            call(new YearTypeHandler(), CallableStatement::getInt, 0, null, true),

            call(new YearMonthTypeHandler(), CallableStatement::getString, YearMonth.of(2000, Month.JANUARY).toString(),
                    YearMonth.of(2000, Month.JANUARY)),
            call(new YearMonthTypeHandler(), CallableStatement::getString, null, null),

            call(new MonthTypeHandler(), CallableStatement::getInt, Month.JANUARY.getValue(), Month.JANUARY),
            call(new MonthTypeHandler(), CallableStatement::getInt, 0, null, true),

            call(new AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), OffsetTime.now(ZoneOffset.UTC)),
            call(new AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), null),

            call(new AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), offsetDateTime),
            call(new AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), null),

            call(new AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), null),
            call(new AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), LocalTime.now()),

            call(new AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), LocalDate.now()),
            call(new AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), null),

            call(new AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), LocalDateTime.now()),
            call(new AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), null),

            call(new AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), null),
            call(new AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), ZonedDateTime.now()),

            call(new DateTypeHandler(), CallableStatement::getTimestamp, new Timestamp(date.getTime()), date),
            call(new DateTypeHandler(), CallableStatement::getTimestamp, null, null),

            //call(new BytesInputStreamTypeHandler(), CallableStatement::getBytes, new byte[] { 1 }, new ByteArrayInputStream(new byte[] { 1 })),
            call(new BytesInputStreamTypeHandler(), CallableStatement::getBytes, null, null),

            call(new BigDecimalTypeHandler(), CallableStatement::getBigDecimal, BigDecimal.valueOf(1)),
            call(new BigDecimalTypeHandler(), CallableStatement::getBigDecimal, null, null)
    );
  }

  public static Stream<Arguments> getResultColumnNameArgumentSource() {
    UUID uuid = UUID.randomUUID();
    java.util.Date date = java.util.Date.from(Instant.now());

    OffsetDateTime offsetDateTime = OffsetDateTime.now();
    return Stream.of(
            stringArgs(new LongTypeHandler(), ResultSet::getLong, 1L),
            stringArgs(new IntegerTypeHandler(), ResultSet::getInt, 1),
            stringArgs(new DoubleTypeHandler(), ResultSet::getDouble, 1D),
            stringArgs(new FloatTypeHandler(), ResultSet::getFloat, 1f),
            stringArgs(new BooleanTypeHandler(), ResultSet::getBoolean, true),
            stringArgs(new ByteArrayTypeHandler(), ResultSet::getBytes, new byte[] { 1 }),
            stringArgs(new ByteTypeHandler(), ResultSet::getByte, (byte) 1),
            stringArgs(new ObjectTypeHandler(), ResultSet::getObject, 1),
            stringArgs(new ShortTypeHandler(), ResultSet::getShort, (short) 1),
            stringArgs(new StringTypeHandler(), ResultSet::getString, "0001"),
            stringArgs(new SqlTimeTypeHandler(), ResultSet::getTime, Time.valueOf(LocalTime.now())),
            stringArgs(new SqlDateTypeHandler(), ResultSet::getDate, Date.valueOf(LocalDate.now())),
            stringArgs(new SqlTimestampTypeHandler(), ResultSet::getTimestamp, Timestamp.from(Instant.now())),

            stringArgs(new DurationTypeHandler(), ResultSet::getLong, Duration.ofDays(1).toNanos(), Duration.ofDays(1)),
            stringArgs(new DurationTypeHandler(), ResultSet::getLong, 0L, Duration.ZERO),
            stringArgs(new DurationTypeHandler(), ResultSet::getLong, 0L, null, true),

            stringArgs(new InstantTypeHandler(), ResultSet::getTimestamp, null, null),
            stringArgs(new InstantTypeHandler(), ResultSet::getTimestamp, Timestamp.from(Instant.EPOCH), Instant.EPOCH),

            stringArgs(new DateTypeHandler(), ResultSet::getTimestamp, new Timestamp(new Date(1).getTime()), new Date(1)),
            stringArgs(new DateTypeHandler(), ResultSet::getTimestamp, null),

            stringArgs(new CharacterTypeHandler(), ResultSet::getString, "1", '1'),
            stringArgs(new CharacterTypeHandler(), ResultSet::getString, null, null),

            stringArgs(new BigIntegerTypeHandler(), ResultSet::getBigDecimal, new BigDecimal(BigInteger.valueOf(1)), BigInteger.valueOf(1)),
            stringArgs(new BigIntegerTypeHandler(), ResultSet::getBigDecimal, null, null),

            stringArgs(new UUIDTypeHandler(), ResultSet::getString, uuid.toString(), uuid),
            stringArgs(new UUIDTypeHandler(), ResultSet::getString, null, null),
            stringArgs(new UUIDTypeHandler(), ResultSet::getString, "", null),

            stringArgs(new YearTypeHandler(), ResultSet::getInt, Year.MIN_VALUE, Year.of(Year.MIN_VALUE)),
            stringArgs(new YearTypeHandler(), ResultSet::getInt, 0, null, true),

            stringArgs(new YearMonthTypeHandler(), ResultSet::getString, YearMonth.of(2000, Month.JANUARY).toString(),
                    YearMonth.of(2000, Month.JANUARY)),
            stringArgs(new YearMonthTypeHandler(), ResultSet::getString, null, null),

            stringArgs(new MonthTypeHandler(), ResultSet::getInt, Month.JANUARY.getValue(), Month.JANUARY),
            stringArgs(new MonthTypeHandler(), ResultSet::getInt, 0, null, true),

            stringArgs(new AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), OffsetTime.now(ZoneOffset.UTC)),
            stringArgs(new AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), null),

            stringArgs(new AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), offsetDateTime),
            stringArgs(new AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), null),

            stringArgs(new AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), null),
            stringArgs(new AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), LocalTime.now()),

            stringArgs(new AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), LocalDate.now()),
            stringArgs(new AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), null),

            stringArgs(new AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), LocalDateTime.now()),
            stringArgs(new AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), null),

            stringArgs(new AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), null),
            stringArgs(new AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), ZonedDateTime.now()),

            stringArgs(new DateTypeHandler(), ResultSet::getTimestamp, new Timestamp(date.getTime()), date),
            stringArgs(new DateTypeHandler(), ResultSet::getTimestamp, null, null),

            stringArgs(new BytesInputStreamTypeHandler(), ResultSet::getBytes, null, null),

            stringArgs(new BigDecimalTypeHandler(), ResultSet::getBigDecimal, BigDecimal.valueOf(1)),
            stringArgs(new BigDecimalTypeHandler(), ResultSet::getBigDecimal, null, null)
    );
  }

  public static Stream<Arguments> getResultColumnIndexArgumentSource() {
    UUID uuid = UUID.randomUUID();
    java.util.Date date = java.util.Date.from(Instant.now());

    OffsetDateTime offsetDateTime = OffsetDateTime.now();
    return Stream.of(
            args(new LongTypeHandler(), ResultSet::getLong, 1L),
            args(new IntegerTypeHandler(), ResultSet::getInt, 1),
            args(new DoubleTypeHandler(), ResultSet::getDouble, 1D),
            args(new FloatTypeHandler(), ResultSet::getFloat, 1f),
            args(new BooleanTypeHandler(), ResultSet::getBoolean, true),
            args(new ByteArrayTypeHandler(), ResultSet::getBytes, new byte[] { 1 }),
            args(new ByteTypeHandler(), ResultSet::getByte, (byte) 1),
            args(new ObjectTypeHandler(), (resultSet1, integer) -> resultSet1.getObject(integer), 1),
            args(new ShortTypeHandler(), ResultSet::getShort, (short) 1),
            args(new StringTypeHandler(), ResultSet::getString, "0001"),
            args(new SqlTimeTypeHandler(), (rs, idx) -> rs.getTime(idx), Time.valueOf(LocalTime.now())),
            args(new SqlDateTypeHandler(), (rs, idx) -> rs.getDate(idx), Date.valueOf(LocalDate.now())),
            args(new SqlTimestampTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), Timestamp.from(Instant.now())),

            args(new DurationTypeHandler(), ResultSet::getLong, Duration.ofDays(1).toNanos(), Duration.ofDays(1)),
            args(new DurationTypeHandler(), ResultSet::getLong, 0L, Duration.ZERO),
            args(new DurationTypeHandler(), ResultSet::getLong, 0L, null, true),

            args(new InstantTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), null, null),
            args(new InstantTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), Timestamp.from(Instant.EPOCH), Instant.EPOCH),

            args(new DateTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), new Timestamp(new Date(1).getTime()), new Date(1)),
            args(new DateTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), null),

            args(new CharacterTypeHandler(), ResultSet::getString, "1", '1'),
            args(new CharacterTypeHandler(), ResultSet::getString, null, null),

            args(new BigIntegerTypeHandler(), (rs, idx) -> rs.getBigDecimal(idx), new BigDecimal(BigInteger.valueOf(1)), BigInteger.valueOf(1)),
            args(new BigIntegerTypeHandler(), (rs, idx) -> rs.getBigDecimal(idx), null, null),

            args(new UUIDTypeHandler(), ResultSet::getString, uuid.toString(), uuid),
            args(new UUIDTypeHandler(), ResultSet::getString, null, null),
            args(new UUIDTypeHandler(), ResultSet::getString, "", null),

            args(new YearTypeHandler(), ResultSet::getInt, Year.MIN_VALUE, Year.of(Year.MIN_VALUE)),
            args(new YearTypeHandler(), ResultSet::getInt, 0, null, true),

            args(new YearMonthTypeHandler(), ResultSet::getString, YearMonth.of(2000, Month.JANUARY).toString(),
                    YearMonth.of(2000, Month.JANUARY)),
            args(new YearMonthTypeHandler(), ResultSet::getString, null, null),

            args(new MonthTypeHandler(), ResultSet::getInt, Month.JANUARY.getValue(), Month.JANUARY),
            args(new MonthTypeHandler(), ResultSet::getInt, 0, null, true),

            args(new AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), OffsetTime.now(ZoneOffset.UTC)),
            args(new AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), null),

            args(new AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), offsetDateTime),
            args(new AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), null),

            args(new AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), null),
            args(new AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), LocalTime.now()),

            args(new AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), LocalDate.now()),
            args(new AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), null),

            args(new AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), LocalDateTime.now()),
            args(new AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), null),

            args(new AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), null),
            args(new AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), ZonedDateTime.now()),

            args(new DateTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), new Timestamp(date.getTime()), date),
            args(new DateTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), null, null),

            args(new BytesInputStreamTypeHandler(), ResultSet::getBytes, null, null),

            args(new BigDecimalTypeHandler(), (rs, idx) -> rs.getBigDecimal(idx), BigDecimal.valueOf(1)),
            args(new BigDecimalTypeHandler(), (rs, idx) -> rs.getBigDecimal(idx), null, null)
    );
  }

  public static Stream<Arguments> setParameterArgumentSource() {
    UUID uuid = UUID.randomUUID();
    java.util.Date date = java.util.Date.from(Instant.now());
    return Stream.of(
            args(new LongTypeHandler(), PreparedStatement::setLong, 1L),
            args(new IntegerTypeHandler(), PreparedStatement::setInt, 1),
            args(new DoubleTypeHandler(), PreparedStatement::setDouble, 1D),
            args(new FloatTypeHandler(), PreparedStatement::setFloat, 1f),
            args(new BooleanTypeHandler(), PreparedStatement::setBoolean, true),
            args(new ByteArrayTypeHandler(), PreparedStatement::setBytes, new byte[] { 1 }),
            args(new ByteTypeHandler(), PreparedStatement::setByte, (byte) 1),
            args(new ObjectTypeHandler(), PreparedStatement::setObject, 1),
            args(new ShortTypeHandler(), PreparedStatement::setShort, (short) 1),
            args(new StringTypeHandler(), PreparedStatement::setString, "0001"),
            args(new SqlTimeTypeHandler(), PreparedStatement::setTime, Time.valueOf(LocalTime.now())),
            args(new SqlDateTypeHandler(), PreparedStatement::setDate, Date.valueOf(LocalDate.now())),
            args(new SqlTimestampTypeHandler(), PreparedStatement::setTimestamp, Timestamp.from(Instant.now())),

            args(new DurationTypeHandler(), PreparedStatement::setLong, Duration.ofDays(1).toNanos(), Duration.ofDays(1)),
            args(new InstantTypeHandler(), PreparedStatement::setTimestamp, Timestamp.from(Instant.MIN), Instant.MIN),
            args(new DateTypeHandler(), PreparedStatement::setTimestamp, new Timestamp(new Date(1).getTime()), new Date(1)),
            args(new CharacterTypeHandler(), PreparedStatement::setString, "1", '1'),

            args(new BigIntegerTypeHandler(), PreparedStatement::setBigDecimal, new BigDecimal(BigInteger.valueOf(1)), BigInteger.valueOf(1)),
            args(new UUIDTypeHandler(), PreparedStatement::setString, uuid.toString(), uuid),
            args(new YearTypeHandler(), PreparedStatement::setInt, Year.MIN_VALUE, Year.of(Year.MIN_VALUE)),
            args(new YearMonthTypeHandler(), PreparedStatement::setString, YearMonth.of(2000, Month.JANUARY).toString(),
                    YearMonth.of(2000, Month.JANUARY)),

            args(new AnyTypeHandler<>(LocalDate.class), PreparedStatement::setObject, LocalDate.now()),
            args(new AnyTypeHandler<>(LocalTime.class), PreparedStatement::setObject, LocalTime.now()),
            args(new AnyTypeHandler<>(LocalDateTime.class), PreparedStatement::setObject, LocalDateTime.now()),
            args(new AnyTypeHandler<>(OffsetTime.class), PreparedStatement::setObject, OffsetTime.now()),
            args(new AnyTypeHandler<>(ZonedDateTime.class), PreparedStatement::setObject, ZonedDateTime.now()),
            args(new AnyTypeHandler<>(OffsetDateTime.class), PreparedStatement::setObject, OffsetDateTime.now()),

            args(new MonthTypeHandler(), PreparedStatement::setInt, Month.JANUARY.getValue(), Month.JANUARY),
            args(new DateTypeHandler(), PreparedStatement::setTimestamp, new Timestamp(date.getTime()), date),

            args(new BytesInputStreamTypeHandler(), PreparedStatement::setBinaryStream, new ByteArrayInputStream(new byte[] { 1 })),

            args(new BigDecimalTypeHandler(), PreparedStatement::setBigDecimal, BigDecimal.valueOf(1))
    );
  }

  static <T> Arguments call(TypeHandler<T> typeHandler, ThrowingBiFunction<CallableStatement, Integer, T> consumer, @Nullable T value) {
    return Arguments.arguments(typeHandler, consumer, value, value, false);
  }

  static <T, E> Arguments call(TypeHandler<E> typeHandler, ThrowingBiFunction<CallableStatement, Integer, T> consumer,
          @Nullable T value, @Nullable E verifyVal) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, false);
  }

  static <T, E> Arguments call(TypeHandler<E> typeHandler,
          ThrowingBiFunction<CallableStatement, Integer, T> consumer, T value, @Nullable E verifyVal, boolean wasNull) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, wasNull);
  }

  static <T> Arguments stringArgs(TypeHandler<T> typeHandler, ThrowingBiFunction<ResultSet, String, T> consumer, @Nullable T value) {
    return Arguments.arguments(typeHandler, consumer, value, value, false);
  }

  static <T, E> Arguments stringArgs(TypeHandler<E> typeHandler, ThrowingBiFunction<ResultSet, String, T> consumer,
          @Nullable T value, @Nullable E verifyVal) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, false);
  }

  static <T, E> Arguments stringArgs(TypeHandler<E> typeHandler,
          ThrowingBiFunction<ResultSet, String, T> consumer, T value, @Nullable E verifyVal, boolean wasNull) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, wasNull);
  }

  static <T> Arguments args(TypeHandler<T> typeHandler, ThrowingBiFunction<ResultSet, Integer, T> consumer, @Nullable T value) {
    return Arguments.arguments(typeHandler, consumer, value, value, false);
  }

  static <T, E> Arguments args(TypeHandler<E> typeHandler, ThrowingBiFunction<ResultSet, Integer, T> consumer,
          @Nullable T value, @Nullable E verifyVal) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, false);
  }

  static <T, E> Arguments args(TypeHandler<E> typeHandler,
          ThrowingBiFunction<ResultSet, Integer, T> consumer, T value, @Nullable E verifyVal, boolean wasNull) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, wasNull);
  }

  static <T, E> Arguments args(TypeHandler<E> typeHandler, PreparedStatementFunc<T> consumer, T value) {
    return Arguments.arguments(typeHandler, consumer, value, value);
  }

  static <T, E> Arguments args(TypeHandler<T> typeHandler, PreparedStatementFunc<E> consumer, E verifyVal, T value) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal);
  }

}