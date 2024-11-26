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

package infra.format.support;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import infra.format.Formatter;

/**
 * A {@link Formatter} for {@link OffsetDateTime} that uses
 * {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME ISO offset formatting}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class IsoOffsetFormatter implements Formatter<OffsetDateTime> {

  @Override
  public String print(OffsetDateTime object, Locale locale) {
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(object);
  }

  @Override
  public OffsetDateTime parse(String text, Locale locale) throws ParseException {
    return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }

}
