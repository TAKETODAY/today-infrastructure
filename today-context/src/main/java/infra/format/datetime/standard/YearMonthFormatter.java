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

package infra.format.datetime.standard;

import java.text.ParseException;
import java.time.YearMonth;
import java.util.Locale;

import infra.format.Formatter;

/**
 * {@link Formatter} implementation for a JSR-310 {@link YearMonth},
 * following JSR-310's parsing rules for a YearMonth.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see YearMonth#parse
 * @since 4.0
 */
class YearMonthFormatter implements Formatter<YearMonth> {

  @Override
  public YearMonth parse(String text, Locale locale) throws ParseException {
    return YearMonth.parse(text);
  }

  @Override
  public String print(YearMonth object, Locale locale) {
    return object.toString();
  }

}
