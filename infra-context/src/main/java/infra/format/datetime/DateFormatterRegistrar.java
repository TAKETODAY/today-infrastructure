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

package infra.format.datetime;

import org.jspecify.annotations.Nullable;

import java.util.Calendar;
import java.util.Date;

import infra.core.conversion.Converter;
import infra.core.conversion.ConverterRegistry;
import infra.format.FormatterRegistrar;
import infra.format.FormatterRegistry;
import infra.lang.Assert;

/**
 * Configures basic date formatting for use with Framework, primarily for
 * {@link infra.format.annotation.DateTimeFormat} declarations.
 * Applies to fields of type {@link Date}, {@link Calendar} and {@code long}.
 *
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDateConverters(ConverterRegistry)} utility method for
 * ad-hoc use against any {@code ConverterRegistry} instance.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.format.datetime.standard.DateTimeFormatterRegistrar
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
    Assert.notNull(dateFormatter, "DateFormatter is required");
    this.dateFormatter = dateFormatter;
  }

  @Override
  public void registerFormatters(FormatterRegistry registry) {
    addDateConverters(registry);
    // In order to retain back compatibility we only register Date/Calendar
    // types when a user defined formatter is specified
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

  private static final class DateToLongConverter implements Converter<Date, Long> {

    @Override
    public Long convert(Date source) {
      return source.getTime();
    }
  }

  private static final class DateToCalendarConverter implements Converter<Date, Calendar> {

    @Override
    public Calendar convert(Date source) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(source);
      return calendar;
    }
  }

  private static final class CalendarToDateConverter implements Converter<Calendar, Date> {

    @Override
    public Date convert(Calendar source) {
      return source.getTime();
    }
  }

  private static final class CalendarToLongConverter implements Converter<Calendar, Long> {

    @Override
    public Long convert(Calendar source) {
      return source.getTimeInMillis();
    }
  }

  private static final class LongToDateConverter implements Converter<Long, Date> {

    @Override
    public Date convert(Long source) {
      return new Date(source);
    }
  }

  private static final class LongToCalendarConverter implements Converter<Long, Calendar> {

    @Override
    public Calendar convert(Long source) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(source);
      return calendar;
    }
  }

}
