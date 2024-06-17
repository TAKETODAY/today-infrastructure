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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/6/15 11:15
 */
class BaseTypeHandlerTests {

  protected final PreparedStatement preparedStatement = mock(PreparedStatement.class);

  interface PreparedStatementFunc<T> {

    void accept(PreparedStatement statement, int parameterIndex, T value) throws SQLException;

  }

  @ParameterizedTest
  @MethodSource("argumentSource")
  <T> void setParameter(BaseTypeHandler<T> typeHandler, PreparedStatementFunc<T> consumer, T value, T verifyVal) throws SQLException {
    typeHandler.setParameter(preparedStatement, 0, value);
    consumer.accept(verify(preparedStatement), 0, verifyVal);

    typeHandler.setParameter(preparedStatement, 0, null);
    verify(preparedStatement).setObject(0, null);
  }

  public static Stream<Arguments> argumentSource() {
    UUID uuid = UUID.randomUUID();
    java.util.Date date = java.util.Date.from(Instant.now());
    return Stream.of(
            args(new LongTypeHandler(), PreparedStatement::setLong, 1L),
            args(new IntegerTypeHandler(), PreparedStatement::setInt, 1),
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
            args(new LocalDateTimeTypeHandler(), PreparedStatement::setTimestamp, Timestamp.valueOf(LocalDateTime.MIN), LocalDateTime.MIN),
            args(new DateTypeHandler(), PreparedStatement::setTimestamp, new Timestamp(new Date(1).getTime()), new Date(1)),
            args(new CharacterTypeHandler(), PreparedStatement::setString, "1", '1'),
            args(new ZonedDateTimeTypeHandler(), PreparedStatement::setTimestamp,
                    Timestamp.from(ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()).toInstant()),
                    ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault())),
            args(new BigIntegerTypeHandler(), PreparedStatement::setBigDecimal, new BigDecimal(BigInteger.valueOf(1)), BigInteger.valueOf(1)),
            args(new UUIDTypeHandler(), PreparedStatement::setString, uuid.toString(), uuid),
            args(new YearTypeHandler(), PreparedStatement::setInt, Year.MIN_VALUE, Year.of(Year.MIN_VALUE)),
            args(new YearMonthTypeHandler(), PreparedStatement::setString, YearMonth.of(2000, Month.JANUARY).toString(),
                    YearMonth.of(2000, Month.JANUARY)),
            args(new OffsetTimeTypeHandler(), PreparedStatement::setTime, Time.valueOf(OffsetTime.MIN.toLocalTime()), OffsetTime.MIN),
            args(new OffsetDateTimeTypeHandler(), PreparedStatement::setTimestamp, Timestamp.from(OffsetDateTime.MAX.toInstant()), OffsetDateTime.MAX),
            args(new MonthTypeHandler(), PreparedStatement::setInt, Month.JANUARY.getValue(), Month.JANUARY),
            args(new LocalTimeTypeHandler(), PreparedStatement::setTime, Time.valueOf(LocalTime.MIN), LocalTime.MIN),
            args(new LocalDateTypeHandler(), PreparedStatement::setDate, Date.valueOf(LocalDate.MAX), LocalDate.MAX),
            args(new LocalDateTimeTypeHandler(), PreparedStatement::setTimestamp, Timestamp.valueOf(LocalDateTime.MAX), LocalDateTime.MAX),
            args(new InstantTypeHandler(), PreparedStatement::setTimestamp, Timestamp.from(Instant.MAX), Instant.MAX),
            args(new DateTypeHandler(), PreparedStatement::setTimestamp, new Timestamp(date.getTime()), date),

//            args(new ClobReaderTypeHandler(), PreparedStatement::setString, "1",'1'),

            args(new BlobInputStreamTypeHandler(), PreparedStatement::setBlob, new ByteArrayInputStream(new byte[] { 1 })),
            args(new BytesInputStreamTypeHandler(), PreparedStatement::setBinaryStream, new ByteArrayInputStream(new byte[] { 1 })),

            args(new BigDecimalTypeHandler(), PreparedStatement::setBigDecimal, BigDecimal.valueOf(1))
    );
  }

  static <T, E> Arguments args(BaseTypeHandler<E> typeHandler, PreparedStatementFunc<T> consumer, T value) {
    return Arguments.arguments(typeHandler, consumer, value, value);
  }

  static <T, E> Arguments args(BaseTypeHandler<T> typeHandler, PreparedStatementFunc<E> consumer, E verifyVal, T value) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal);
  }

}