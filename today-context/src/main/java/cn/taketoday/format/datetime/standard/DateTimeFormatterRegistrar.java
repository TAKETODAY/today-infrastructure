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

package cn.taketoday.format.datetime.standard;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.EnumMap;

import cn.taketoday.format.FormatterRegistrar;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.format.annotation.DateTimeFormat.ISO;

/**
 * Configures the JSR-310 <code>java.time</code> formatting system for use with Framework.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see #setUseIsoFormat
 * @see cn.taketoday.format.FormatterRegistrar#registerFormatters
 * @see cn.taketoday.format.datetime.DateFormatterRegistrar
 * @since 4.0
 */
public class DateTimeFormatterRegistrar implements FormatterRegistrar {

  private enum Type {
    DATE, TIME, DATE_TIME
  }

  /**
   * User-defined formatters.
   */
  private final EnumMap<Type, DateTimeFormatter> formatters = new EnumMap<>(Type.class);

  /**
   * Factories used when specific formatters have not been specified.
   */
  private final EnumMap<Type, DateTimeFormatterFactory> factories = new EnumMap<>(Type.class);

  public DateTimeFormatterRegistrar() {
    for (Type type : Type.values()) {
      this.factories.put(type, new DateTimeFormatterFactory());
    }
  }

  /**
   * Set whether standard ISO formatting should be applied to all date/time types.
   * Default is "false" (no).
   * <p>If set to "true", the "dateStyle", "timeStyle" and "dateTimeStyle"
   * properties are effectively ignored.
   */
  public void setUseIsoFormat(boolean useIsoFormat) {
    this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : ISO.NONE);
    this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : ISO.NONE);
    this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : ISO.NONE);
  }

  /**
   * Set the default format style of {@link LocalDate} objects.
   * Default is {@link FormatStyle#SHORT}.
   */
  public void setDateStyle(FormatStyle dateStyle) {
    this.factories.get(Type.DATE).setDateStyle(dateStyle);
  }

  /**
   * Set the default format style of {@link LocalTime} objects.
   * Default is {@link FormatStyle#SHORT}.
   */
  public void setTimeStyle(FormatStyle timeStyle) {
    this.factories.get(Type.TIME).setTimeStyle(timeStyle);
  }

  /**
   * Set the default format style of {@link LocalDateTime} objects.
   * Default is {@link FormatStyle#SHORT}.
   */
  public void setDateTimeStyle(FormatStyle dateTimeStyle) {
    this.factories.get(Type.DATE_TIME).setDateTimeStyle(dateTimeStyle);
  }

  /**
   * Set the formatter that will be used for objects representing date values.
   * <p>This formatter will be used for the {@link LocalDate} type.
   * When specified, the {@link #setDateStyle dateStyle} and
   * {@link #setUseIsoFormat useIsoFormat} properties will be ignored.
   *
   * @param formatter the formatter to use
   * @see #setTimeFormatter
   * @see #setDateTimeFormatter
   */
  public void setDateFormatter(DateTimeFormatter formatter) {
    this.formatters.put(Type.DATE, formatter);
  }

  /**
   * Set the formatter that will be used for objects representing time values.
   * <p>This formatter will be used for the {@link LocalTime} and {@link OffsetTime}
   * types. When specified, the {@link #setTimeStyle timeStyle} and
   * {@link #setUseIsoFormat useIsoFormat} properties will be ignored.
   *
   * @param formatter the formatter to use
   * @see #setDateFormatter
   * @see #setDateTimeFormatter
   */
  public void setTimeFormatter(DateTimeFormatter formatter) {
    this.formatters.put(Type.TIME, formatter);
  }

  /**
   * Set the formatter that will be used for objects representing date and time values.
   * <p>This formatter will be used for {@link LocalDateTime}, {@link ZonedDateTime}
   * and {@link OffsetDateTime} types. When specified, the
   * {@link #setDateTimeStyle dateTimeStyle} and
   * {@link #setUseIsoFormat useIsoFormat} properties will be ignored.
   *
   * @param formatter the formatter to use
   * @see #setDateFormatter
   * @see #setTimeFormatter
   */
  public void setDateTimeFormatter(DateTimeFormatter formatter) {
    this.formatters.put(Type.DATE_TIME, formatter);
  }

  @Override
  public void registerFormatters(FormatterRegistry registry) {
    DateTimeConverters.registerConverters(registry);

    DateTimeFormatter df = getFormatter(Type.DATE);
    DateTimeFormatter tf = getFormatter(Type.TIME);
    DateTimeFormatter dtf = getFormatter(Type.DATE_TIME);

    // Efficient ISO_LOCAL_* variants for printing since they are twice as fast...

    registry.addFormatterForFieldType(LocalDate.class,
            new TemporalAccessorPrinter(
                    df == DateTimeFormatter.ISO_DATE ? DateTimeFormatter.ISO_LOCAL_DATE : df),
            new TemporalAccessorParser(LocalDate.class, df));

    registry.addFormatterForFieldType(LocalTime.class,
            new TemporalAccessorPrinter(
                    tf == DateTimeFormatter.ISO_TIME ? DateTimeFormatter.ISO_LOCAL_TIME : tf),
            new TemporalAccessorParser(LocalTime.class, tf));

    registry.addFormatterForFieldType(LocalDateTime.class,
            new TemporalAccessorPrinter(
                    dtf == DateTimeFormatter.ISO_DATE_TIME ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : dtf),
            new TemporalAccessorParser(LocalDateTime.class, dtf));

    registry.addFormatterForFieldType(ZonedDateTime.class,
            new TemporalAccessorPrinter(dtf),
            new TemporalAccessorParser(ZonedDateTime.class, dtf));

    registry.addFormatterForFieldType(OffsetDateTime.class,
            new TemporalAccessorPrinter(dtf),
            new TemporalAccessorParser(OffsetDateTime.class, dtf));

    registry.addFormatterForFieldType(OffsetTime.class,
            new TemporalAccessorPrinter(tf),
            new TemporalAccessorParser(OffsetTime.class, tf));

    registry.addFormatterForFieldType(Instant.class, new InstantFormatter());
    registry.addFormatterForFieldType(Period.class, new PeriodFormatter());
    registry.addFormatterForFieldType(Duration.class, new DurationFormatter());
    registry.addFormatterForFieldType(Year.class, new YearFormatter());
    registry.addFormatterForFieldType(Month.class, new MonthFormatter());
    registry.addFormatterForFieldType(YearMonth.class, new YearMonthFormatter());
    registry.addFormatterForFieldType(MonthDay.class, new MonthDayFormatter());

    registry.addFormatterForFieldAnnotation(new Jsr310DateTimeFormatAnnotationFormatterFactory());
  }

  private DateTimeFormatter getFormatter(Type type) {
    DateTimeFormatter formatter = this.formatters.get(type);
    if (formatter != null) {
      return formatter;
    }
    DateTimeFormatter fallbackFormatter = getFallbackFormatter(type);
    return this.factories.get(type).createDateTimeFormatter(fallbackFormatter);
  }

  private DateTimeFormatter getFallbackFormatter(Type type) {
    return switch (type) {
      case DATE -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
      case TIME -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
      default -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    };
  }

}
