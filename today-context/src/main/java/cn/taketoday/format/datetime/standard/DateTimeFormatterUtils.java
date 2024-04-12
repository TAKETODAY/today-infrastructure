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

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import cn.taketoday.util.StringUtils;

/**
 * Internal {@link DateTimeFormatter} utilities.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class DateTimeFormatterUtils {

  static DateTimeFormatter createStrictDateTimeFormatter(String pattern) {
    // Using strict parsing to align with Joda-Time and standard DateFormat behavior:
    // otherwise, an overflow like e.g. Feb 29 for a non-leap-year wouldn't get rejected.
    // However, with strict parsing, a year digit needs to be specified as 'u'...
    String patternToUse = StringUtils.replace(pattern, "yy", "uu");
    return DateTimeFormatter.ofPattern(patternToUse).withResolverStyle(ResolverStyle.STRICT);
  }

}
