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

package cn.taketoday.format.datetime.standard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Set;

import cn.taketoday.context.support.EmbeddedValueResolutionSupport;
import cn.taketoday.format.AnnotationFormatterFactory;
import cn.taketoday.format.Parser;
import cn.taketoday.format.Printer;
import cn.taketoday.format.annotation.DateTimeFormat;
import cn.taketoday.util.StringUtils;

/**
 * Formats fields annotated with the {@link DateTimeFormat} annotation using the
 * JSR-310 <code>java.time</code> package in JDK 8.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Kazuki Shimizu
 * @see DateTimeFormat
 * @since 4.0
 */
public class Jsr310DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
        implements AnnotationFormatterFactory<DateTimeFormat> {

  // Create the set of field types that may be annotated with @DateTimeFormat.
  private static final Set<Class<?>> FIELD_TYPES = Set.of(
          Instant.class,
          LocalDate.class,
          LocalTime.class,
          LocalDateTime.class,
          ZonedDateTime.class,
          OffsetDateTime.class,
          OffsetTime.class,
          YearMonth.class,
          MonthDay.class
  );

  @Override
  public final Set<Class<?>> getFieldTypes() {
    return FIELD_TYPES;
  }

  @Override
  public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
    DateTimeFormatter formatter = getFormatter(annotation, fieldType);

    // Efficient ISO_LOCAL_* variants for printing since they are twice as fast...
    if (formatter == DateTimeFormatter.ISO_DATE) {
      if (isLocal(fieldType)) {
        formatter = DateTimeFormatter.ISO_LOCAL_DATE;
      }
    }
    else if (formatter == DateTimeFormatter.ISO_TIME) {
      if (isLocal(fieldType)) {
        formatter = DateTimeFormatter.ISO_LOCAL_TIME;
      }
    }
    else if (formatter == DateTimeFormatter.ISO_DATE_TIME) {
      if (isLocal(fieldType)) {
        formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
      }
    }

    return new TemporalAccessorPrinter(formatter);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
    ArrayList<String> resolvedFallbackPatterns = new ArrayList<>();
    for (String fallbackPattern : annotation.fallbackPatterns()) {
      String resolvedFallbackPattern = resolveEmbeddedValue(fallbackPattern);
      if (StringUtils.isNotEmpty(resolvedFallbackPattern)) {
        resolvedFallbackPatterns.add(resolvedFallbackPattern);
      }
    }

    DateTimeFormatter formatter = getFormatter(annotation, fieldType);
    return new TemporalAccessorParser((Class<? extends TemporalAccessor>) fieldType,
            formatter, StringUtils.toStringArray(resolvedFallbackPatterns), annotation);
  }

  /**
   * Factory method used to create a {@link DateTimeFormatter}.
   *
   * @param annotation the format annotation for the field
   * @param fieldType the declared type of the field
   * @return a {@link DateTimeFormatter} instance
   */
  protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
    DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
    String style = resolveEmbeddedValue(annotation.style());
    if (StringUtils.isNotEmpty(style)) {
      factory.setStylePattern(style);
    }
    factory.setIso(annotation.iso());
    String pattern = resolveEmbeddedValue(annotation.pattern());
    if (StringUtils.isNotEmpty(pattern)) {
      factory.setPattern(pattern);
    }
    return factory.createDateTimeFormatter();
  }

  private boolean isLocal(Class<?> fieldType) {
    return fieldType.getSimpleName().startsWith("Local");
  }

}
