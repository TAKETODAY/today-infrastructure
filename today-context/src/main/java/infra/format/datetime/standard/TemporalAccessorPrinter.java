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

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import infra.format.Printer;

/**
 * {@link Printer} implementation for a JSR-310 {@link TemporalAccessor},
 * using a {@link DateTimeFormatter}) (the contextual one, if available).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DateTimeContextHolder#getFormatter
 * @see DateTimeFormatter#format(TemporalAccessor)
 * @since 4.0
 */
public final class TemporalAccessorPrinter implements Printer<TemporalAccessor> {

  private final DateTimeFormatter formatter;

  /**
   * Create a new TemporalAccessorPrinter.
   *
   * @param formatter the base DateTimeFormatter instance
   */
  public TemporalAccessorPrinter(DateTimeFormatter formatter) {
    this.formatter = formatter;
  }

  @Override
  public String print(TemporalAccessor partial, Locale locale) {
    return DateTimeContextHolder.getFormatter(this.formatter, locale).format(partial);
  }

}
