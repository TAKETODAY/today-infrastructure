/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.format.datetime;

import java.util.Calendar;
import java.util.Date;

import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterRegistry;
import cn.taketoday.format.FormatterRegistrar;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Configures basic date formatting for use with Framework, primarily for
 * {@link cn.taketoday.format.annotation.DateTimeFormat} declarations.
 * Applies to fields of type {@link Date}, {@link Calendar} and {@code long}.
 *
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDateConverters(ConverterRegistry)} utility method for
 * ad-hoc use against any {@code ConverterRegistry} instance.
 *
 * @author Phillip Webb
 * @see cn.taketoday.format.datetime.standard.DateTimeFormatterRegistrar
 * @see FormatterRegistrar#registerFormatters
 * @since 4.0
 */
public class DateFormatterRegistrar implements FormatterRegistrar {

  @Nullable
  private DateFormatter dateFormatter;

  /**
   * Set a global date formatter to register.
   * <p>If not specified, no general formatter for non-annotated
   * {@link Date} and {@link Calendar} fields will be registered.
   */
  public void setFormatter(DateFormatter dateFormatter) {
    Assert.notNull(dateFormatter, "DateFormatter must not be null");
    this.dateFormatter = dateFormatter;
  }

  @Override
  public void registerFormatters(FormatterRegistry registry) {
    addDateConverters(registry);
    // In order to retain back compatibility we only register Date/Calendar
    // types when a user defined formatter is specified (see SPR-10105)
    if (this.dateFormatter != null) {
      registry.addFormatter(this.dateFormatter);
      registry.addFormatterForFieldType(Calendar.class, this.dateFormatter);
    }
    registry.addFormatterForFieldAnnotation(new DateTimeFormatAnnotationFormatterFactory());
  }

  /**
   * Add date converters to the specified registry.
   *
   * @param converterRegistry the registry of converters to add to
   */
  public static void addDateConverters(ConverterRegistry converterRegistry) {
    converterRegistry.addConverter(new DateToLongConverter());
    converterRegistry.addConverter(new DateToCalendarConverter());
    converterRegistry.addConverter(new CalendarToDateConverter());
    converterRegistry.addConverter(new CalendarToLongConverter());
    converterRegistry.addConverter(new LongToDateConverter());
    converterRegistry.addConverter(new LongToCalendarConverter());
  }

  private static class DateToLongConverter implements Converter<Date, Long> {

    @Override
    public Long convert(Date source) {
      return source.getTime();
    }
  }

  private static class DateToCalendarConverter implements Converter<Date, Calendar> {

    @Override
    public Calendar convert(Date source) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(source);
      return calendar;
    }
  }

  private static class CalendarToDateConverter implements Converter<Calendar, Date> {

    @Override
    public Date convert(Calendar source) {
      return source.getTime();
    }
  }

  private static class CalendarToLongConverter implements Converter<Calendar, Long> {

    @Override
    public Long convert(Calendar source) {
      return source.getTimeInMillis();
    }
  }

  private static class LongToDateConverter implements Converter<Long, Date> {

    @Override
    public Date convert(Long source) {
      return new Date(source);
    }
  }

  private static class LongToCalendarConverter implements Converter<Long, Calendar> {

    @Override
    public Calendar convert(Long source) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(source);
      return calendar;
    }
  }

}
