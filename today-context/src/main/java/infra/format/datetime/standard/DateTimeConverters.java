/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.format.datetime.standard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import infra.core.conversion.Converter;
import infra.core.conversion.ConverterRegistry;
import infra.format.datetime.DateFormatterRegistrar;

/**
 * Installs lower-level type converters required to integrate
 * JSR-310 support into Framework's field formatting system.
 *
 * <p>Note: {@link DateTimeFormatterRegistrar} installs these converters but
 * does not rely on them for its formatters. They are just being registered
 * for custom conversion scenarios between different JSR-310 value types
 * and also between {@link Calendar} and JSR-310 value types.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DateTimeConverters {

  private DateTimeConverters() {
  }

  /**
   * Install the converters into the converter registry.
   *
   * @param registry the converter registry
   */
  public static void registerConverters(ConverterRegistry registry) {
    DateFormatterRegistrar.addDateConverters(registry);

    registry.addConverter(new LocalDateTimeToLocalDateConverter());
    registry.addConverter(new LocalDateTimeToLocalTimeConverter());
    registry.addConverter(new ZonedDateTimeToLocalDateConverter());
    registry.addConverter(new ZonedDateTimeToLocalTimeConverter());
    registry.addConverter(new ZonedDateTimeToLocalDateTimeConverter());
    registry.addConverter(new ZonedDateTimeToOffsetDateTimeConverter());
    registry.addConverter(new ZonedDateTimeToInstantConverter());
    registry.addConverter(new OffsetDateTimeToLocalDateConverter());
    registry.addConverter(new OffsetDateTimeToLocalTimeConverter());
    registry.addConverter(new OffsetDateTimeToLocalDateTimeConverter());
    registry.addConverter(new OffsetDateTimeToZonedDateTimeConverter());
    registry.addConverter(new OffsetDateTimeToInstantConverter());
    registry.addConverter(new CalendarToZonedDateTimeConverter());
    registry.addConverter(new CalendarToOffsetDateTimeConverter());
    registry.addConverter(new CalendarToLocalDateConverter());
    registry.addConverter(new CalendarToLocalTimeConverter());
    registry.addConverter(new CalendarToLocalDateTimeConverter());
    registry.addConverter(new CalendarToInstantConverter());
    registry.addConverter(new LongToInstantConverter());
    registry.addConverter(new InstantToLongConverter());
  }

  private static ZonedDateTime calendarToZonedDateTime(Calendar source) {
    if (source instanceof GregorianCalendar) {
      return ((GregorianCalendar) source).toZonedDateTime();
    }
    else {
      return Instant.ofEpochMilli(source.getTimeInMillis()).atZone(source.getTimeZone().toZoneId());
    }
  }

  private static final class LocalDateTimeToLocalDateConverter implements Converter<LocalDateTime, LocalDate> {

    @Override
    public LocalDate convert(LocalDateTime source) {
      return source.toLocalDate();
    }
  }

  private static final class LocalDateTimeToLocalTimeConverter implements Converter<LocalDateTime, LocalTime> {

    @Override
    public LocalTime convert(LocalDateTime source) {
      return source.toLocalTime();
    }
  }

  private static final class ZonedDateTimeToLocalDateConverter implements Converter<ZonedDateTime, LocalDate> {

    @Override
    public LocalDate convert(ZonedDateTime source) {
      return source.toLocalDate();
    }
  }

  private static final class ZonedDateTimeToLocalTimeConverter implements Converter<ZonedDateTime, LocalTime> {

    @Override
    public LocalTime convert(ZonedDateTime source) {
      return source.toLocalTime();
    }
  }

  private static final class ZonedDateTimeToLocalDateTimeConverter implements Converter<ZonedDateTime, LocalDateTime> {

    @Override
    public LocalDateTime convert(ZonedDateTime source) {
      return source.toLocalDateTime();
    }
  }

  private static final class ZonedDateTimeToOffsetDateTimeConverter implements Converter<ZonedDateTime, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(ZonedDateTime source) {
      return source.toOffsetDateTime();
    }
  }

  private static final class ZonedDateTimeToInstantConverter implements Converter<ZonedDateTime, Instant> {

    @Override
    public Instant convert(ZonedDateTime source) {
      return source.toInstant();
    }
  }

  private static final class OffsetDateTimeToLocalDateConverter implements Converter<OffsetDateTime, LocalDate> {

    @Override
    public LocalDate convert(OffsetDateTime source) {
      return source.toLocalDate();
    }
  }

  private static final class OffsetDateTimeToLocalTimeConverter implements Converter<OffsetDateTime, LocalTime> {

    @Override
    public LocalTime convert(OffsetDateTime source) {
      return source.toLocalTime();
    }
  }

  private static final class OffsetDateTimeToLocalDateTimeConverter implements Converter<OffsetDateTime, LocalDateTime> {

    @Override
    public LocalDateTime convert(OffsetDateTime source) {
      return source.toLocalDateTime();
    }
  }

  private static final class OffsetDateTimeToZonedDateTimeConverter implements Converter<OffsetDateTime, ZonedDateTime> {

    @Override
    public ZonedDateTime convert(OffsetDateTime source) {
      return source.toZonedDateTime();
    }
  }

  private static final class OffsetDateTimeToInstantConverter implements Converter<OffsetDateTime, Instant> {

    @Override
    public Instant convert(OffsetDateTime source) {
      return source.toInstant();
    }
  }

  private static final class CalendarToZonedDateTimeConverter implements Converter<Calendar, ZonedDateTime> {

    @Override
    public ZonedDateTime convert(Calendar source) {
      return calendarToZonedDateTime(source);
    }
  }

  private static final class CalendarToOffsetDateTimeConverter implements Converter<Calendar, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(Calendar source) {
      return calendarToZonedDateTime(source).toOffsetDateTime();
    }
  }

  private static final class CalendarToLocalDateConverter implements Converter<Calendar, LocalDate> {

    @Override
    public LocalDate convert(Calendar source) {
      return calendarToZonedDateTime(source).toLocalDate();
    }
  }

  private static final class CalendarToLocalTimeConverter implements Converter<Calendar, LocalTime> {

    @Override
    public LocalTime convert(Calendar source) {
      return calendarToZonedDateTime(source).toLocalTime();
    }
  }

  private static final class CalendarToLocalDateTimeConverter implements Converter<Calendar, LocalDateTime> {

    @Override
    public LocalDateTime convert(Calendar source) {
      return calendarToZonedDateTime(source).toLocalDateTime();
    }
  }

  private static final class CalendarToInstantConverter implements Converter<Calendar, Instant> {

    @Override
    public Instant convert(Calendar source) {
      return calendarToZonedDateTime(source).toInstant();
    }
  }

  private static final class LongToInstantConverter implements Converter<Long, Instant> {

    @Override
    public Instant convert(Long source) {
      return Instant.ofEpochMilli(source);
    }
  }

  private static final class InstantToLongConverter implements Converter<Instant, Long> {

    @Override
    public Long convert(Instant source) {
      return source.toEpochMilli();
    }
  }

}
