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

package cn.taketoday.web.config.format;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import cn.taketoday.util.StringUtils;

/**
 * {@link DateTimeFormatter Formatters} for dates, times, and date-times.
 *
 * @author Andy Wilkinson
 * @author Gaurav Pareek
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/15 12:59
 */
public class DateTimeFormatters {

  private DateTimeFormatter dateFormatter;

  private String datePattern;

  private DateTimeFormatter timeFormatter;

  private DateTimeFormatter dateTimeFormatter;

  /**
   * Configures the date format using the given {@code pattern}.
   *
   * @param pattern the pattern for formatting dates
   * @return {@code this} for chained method invocation
   */
  public DateTimeFormatters dateFormat(String pattern) {
    if (isIso(pattern)) {
      this.dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
      this.datePattern = "yyyy-MM-dd";
    }
    else {
      this.dateFormatter = formatter(pattern);
      this.datePattern = pattern;
    }
    return this;
  }

  /**
   * Configures the time format using the given {@code pattern}.
   *
   * @param pattern the pattern for formatting times
   * @return {@code this} for chained method invocation
   */
  public DateTimeFormatters timeFormat(String pattern) {
    this.timeFormatter = isIso(pattern) ? DateTimeFormatter.ISO_LOCAL_TIME :
                         (isIsoOffset(pattern) ? DateTimeFormatter.ISO_OFFSET_TIME : formatter(pattern));
    return this;
  }

  /**
   * Configures the date-time format using the given {@code pattern}.
   *
   * @param pattern the pattern for formatting date-times
   * @return {@code this} for chained method invocation
   */
  public DateTimeFormatters dateTimeFormat(String pattern) {
    this.dateTimeFormatter = isIso(pattern)
                             ? DateTimeFormatter.ISO_LOCAL_DATE_TIME
                             : (isIsoOffset(pattern) ? DateTimeFormatter.ISO_OFFSET_DATE_TIME : formatter(pattern));
    return this;
  }

  DateTimeFormatter getDateFormatter() {
    return this.dateFormatter;
  }

  String getDatePattern() {
    return this.datePattern;
  }

  DateTimeFormatter getTimeFormatter() {
    return this.timeFormatter;
  }

  DateTimeFormatter getDateTimeFormatter() {
    return this.dateTimeFormatter;
  }

  boolean isCustomized() {
    return this.dateFormatter != null || this.timeFormatter != null || this.dateTimeFormatter != null;
  }

  private static DateTimeFormatter formatter(String pattern) {
    return StringUtils.hasText(pattern)
           ? DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.SMART) : null;
  }

  private static boolean isIso(String pattern) {
    return "iso".equalsIgnoreCase(pattern);
  }

  private static boolean isIsoOffset(String pattern) {
    return "isooffset".equalsIgnoreCase(pattern) || "iso-offset".equalsIgnoreCase(pattern);
  }

}
