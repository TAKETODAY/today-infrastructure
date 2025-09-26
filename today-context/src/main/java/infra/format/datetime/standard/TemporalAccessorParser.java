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

import org.jspecify.annotations.Nullable;

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

import infra.format.Parser;
import infra.util.ObjectUtils;

/**
 * {@link Parser} implementation for a JSR-310 {@link TemporalAccessor},
 * using a {@link DateTimeFormatter} (the contextual one, if available).
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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

  TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType,
          DateTimeFormatter formatter, @Nullable String[] fallbackPatterns, @Nullable Object source) {
    this.temporalAccessorType = temporalAccessorType;
    this.fallbackPatterns = fallbackPatterns;
    this.formatter = formatter;
    this.source = source;
  }

  @Override
  public TemporalAccessor parse(String text, Locale locale) throws ParseException {
    try {
      return doParse(text, locale, formatter);
    }
    catch (DateTimeParseException ex) {
      if (ObjectUtils.isNotEmpty(fallbackPatterns)) {
        for (String pattern : fallbackPatterns) {
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
      else {
        // Fallback to ISO-based default java.time type parsing
        try {
          return defaultParse(text);
        }
        catch (DateTimeParseException ignoredException) {
          // Ignore fallback parsing exception like above
        }
      }
      if (source != null) {
        throw new DateTimeParseException(String.format(
                "Unable to parse date time value \"%s\" using configuration from %s", text, this.source),
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

  private TemporalAccessor defaultParse(String text) throws DateTimeParseException {
    if (Instant.class == this.temporalAccessorType) {
      return Instant.parse(text);
    }
    else if (LocalDate.class == this.temporalAccessorType) {
      return LocalDate.parse(text);
    }
    else if (LocalTime.class == this.temporalAccessorType) {
      return LocalTime.parse(text);
    }
    else if (LocalDateTime.class == this.temporalAccessorType) {
      return LocalDateTime.parse(text);
    }
    else if (ZonedDateTime.class == this.temporalAccessorType) {
      return ZonedDateTime.parse(text);
    }
    else if (OffsetDateTime.class == this.temporalAccessorType) {
      return OffsetDateTime.parse(text);
    }
    else if (OffsetTime.class == this.temporalAccessorType) {
      return OffsetTime.parse(text);
    }
    else if (YearMonth.class == this.temporalAccessorType) {
      return YearMonth.parse(text);
    }
    else if (MonthDay.class == this.temporalAccessorType) {
      return MonthDay.parse(text);
    }
    else {
      throw new IllegalStateException("Unsupported TemporalAccessor type: " + this.temporalAccessorType);
    }
  }

}
