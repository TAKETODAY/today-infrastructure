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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import infra.format.Formatter;

/**
 * {@link Formatter} implementation for a JSR-310 {@link Instant},
 * following JSR-310's parsing rules for an Instant (that is, not using a
 * configurable {@link DateTimeFormatter}): accepting the
 * default {@code ISO_INSTANT} format as well as {@code RFC_1123_DATE_TIME}
 * (which is commonly used for HTTP date header values)
 *
 * @author Juergen Hoeller
 * @author Andrei Nevedomskii
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Instant#parse
 * @see DateTimeFormatter#ISO_INSTANT
 * @see DateTimeFormatter#RFC_1123_DATE_TIME
 * @since 4.0
 */
public class InstantFormatter implements Formatter<Instant> {

  @Override
  public Instant parse(String text, Locale locale) throws ParseException {
    try {
      return Instant.ofEpochMilli(Long.parseLong(text));
    }
    catch (NumberFormatException ex) {
      if (!text.isEmpty() && Character.isAlphabetic(text.charAt(0))) {
        // assuming RFC-1123 value a la "Tue, 3 Jun 2008 11:05:30 GMT"
        return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(text));
      }
      else {
        // assuming UTC instant a la "2007-12-03T10:15:30.00Z"
        return Instant.parse(text);
      }
    }
  }

  @Override
  public String print(Instant object, Locale locale) {
    return object.toString();
  }

}
