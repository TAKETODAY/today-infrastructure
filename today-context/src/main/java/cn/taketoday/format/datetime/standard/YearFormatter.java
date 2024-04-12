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

import java.text.ParseException;
import java.time.Year;
import java.util.Locale;

import cn.taketoday.format.Formatter;

/**
 * {@link Formatter} implementation for a JSR-310 {@link Year},
 * following JSR-310's parsing rules for a Year.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Year#parse
 * @since 4.0
 */
class YearFormatter implements Formatter<Year> {

  @Override
  public Year parse(String text, Locale locale) throws ParseException {
    return Year.parse(text);
  }

  @Override
  public String print(Year object, Locale locale) {
    return object.toString();
  }

}
