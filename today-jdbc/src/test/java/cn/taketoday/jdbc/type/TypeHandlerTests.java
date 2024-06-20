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

            //args(new BytesInputStreamTypeHandler(), ResultSet::getBytes, new byte[] { 1 }, new ByteArrayInputStream(new byte[] { 1 })),
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

            args(new BlobInputStreamTypeHandler(), PreparedStatement::setBlob, new ByteArrayInputStream(new byte[] { 1 })),
            args(new BytesInputStreamTypeHandler(), PreparedStatement::setBinaryStream, new ByteArrayInputStream(new byte[] { 1 })),

            args(new BigDecimalTypeHandler(), PreparedStatement::setBigDecimal, BigDecimal.valueOf(1))
    );
  }

  static <T> Arguments args(TypeHandler<T> typeHandler, ThrowingBiFunction<ResultSet, Integer, T> consumer, @Nullable T value) {
    return Arguments.arguments(typeHandler, consumer, value, value, false);
  }

  static <T, E> Arguments args(TypeHandler<E> typeHandler,
          ThrowingBiFunction<ResultSet, Integer, T> consumer, T value, boolean wasNull) {
    return Arguments.arguments(typeHandler, consumer, value, value, wasNull);
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