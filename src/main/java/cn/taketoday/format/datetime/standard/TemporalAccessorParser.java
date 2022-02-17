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

import java.text.ParseException;
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
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import cn.taketoday.format.Parser;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link Parser} implementation for a JSR-310 {@link TemporalAccessor},
 * using a {@link DateTimeFormatter} (the contextual one, if available).
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Kazuki Shimizu
 * @see DateTimeContextHolder#getFormatter
 * @see LocalDate#parse(CharSequence, DateTimeFormatter)
 * @see LocalTime#parse(CharSequence, DateTimeFormatter)
 * @see LocalDateTime#parse(CharSequence, DateTimeFormatter)
 * @see ZonedDateTime#parse(CharSequence, DateTimeFormatter)
 * @see OffsetDateTime#parse(CharSequence, DateTimeFormatter)
 * @see OffsetTime#parse(CharSequence, DateTimeFormatter)
 * @see YearMonth#parse(CharSequence, DateTimeFormatter)
 * @see MonthDay#parse(CharSequence, DateTimeFormatter)
 * @since 4.0
 */
public final class TemporalAccessorParser implements Parser<TemporalAccessor> {

  private final Class<? extends TemporalAccessor> temporalAccessorType;

  private final DateTimeFormatter formatter;

  @Nullable
  private final String[] fallbackPatterns;

  @Nullable
  private final Object source;

  /**
   * Create a new TemporalAccessorParser for the given TemporalAccessor type.
   *
   * @param temporalAccessorType the specific TemporalAccessor class
   * (LocalDate, LocalTime, LocalDateTime, ZonedDateTime, OffsetDateTime, OffsetTime)
   * @param formatter the base DateTimeFormatter instance
   */
  public TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter) {
    this(temporalAccessorType, formatter, null, null);
  }

  TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter,
                         @Nullable String[] fallbackPatterns, @Nullable Object source) {
    this.temporalAccessorType = temporalAccessorType;
    this.formatter = formatter;
    this.fallbackPatterns = fallbackPatterns;
    this.source = source;
  }

  @Override
  public TemporalAccessor parse(String text, Locale locale) throws ParseException {
    try {
      return doParse(text, locale, this.formatter);
    }
    catch (DateTimeParseException ex) {
      if (!ObjectUtils.isEmpty(this.fallbackPatterns)) {
        for (String pattern : this.fallbackPatterns) {
          try {
            DateTimeFormatter fallbackFormatter = DateTimeFormatterUtils.createStrictDateTimeFormatter(pattern);
            return doParse(text, locale, fallbackFormatter);
          }
          catch (DateTimeParseException ignoredException) {
            // Ignore fallback parsing exceptions since the exception thrown below
            // will include information from the "source" if available -- for example,
            // the toString() of a @DateTimeFormat annotation.
          }
        }
      }
      if (this.source != null) {
        throw new DateTimeParseException(
                String.format("Unable to parse date time value \"%s\" using configuration from %s", text, this.source),
                text, ex.getErrorIndex(), ex);
      }
      // else rethrow original exception
      throw ex;
    }
  }

  private TemporalAccessor doParse(String text, Locale locale, DateTimeFormatter formatter) throws DateTimeParseException {
    DateTimeFormatter formatterToUse = DateTimeContextHolder.getFormatter(formatter, locale);
    if (Instant.class == this.temporalAccessorType) {
      return formatterToUse.parse(text, Instant::from);
    }
    else if (LocalDate.class == this.temporalAccessorType) {
      return LocalDate.parse(text, formatterToUse);
    }
    else if (LocalTime.class == this.temporalAccessorType) {
      return LocalTime.parse(text, formatterToUse);
    }
    else if (LocalDateTime.class == this.temporalAccessorType) {
      return LocalDateTime.parse(text, formatterToUse);
    }
    else if (ZonedDateTime.class == this.temporalAccessorType) {
      return ZonedDateTime.parse(text, formatterToUse);
    }
    else if (OffsetDateTime.class == this.temporalAccessorType) {
      return OffsetDateTime.parse(text, formatterToUse);
    }
    else if (OffsetTime.class == this.temporalAccessorType) {
      return OffsetTime.parse(text, formatterToUse);
    }
    else if (YearMonth.class == this.temporalAccessorType) {
      return YearMonth.parse(text, formatterToUse);
    }
    else if (MonthDay.class == this.temporalAccessorType) {
      return MonthDay.parse(text, formatterToUse);
    }
    else {
      throw new IllegalStateException("Unsupported TemporalAccessor type: " + this.temporalAccessorType);
    }
  }

}
