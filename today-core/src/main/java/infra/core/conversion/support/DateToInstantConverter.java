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

package infra.core.conversion.support;

import java.time.Instant;
import java.util.Date;

import infra.core.conversion.Converter;

/**
 * Convert a {@link Date} to a {@link Instant}.
 *
 * <p>This includes conversion support for {@link java.sql.Timestamp} and other
 * subtypes of {@code java.util.Date}. Note, however, that an attempt to convert
 * a {@link java.sql.Date} or {@link java.sql.Time} to a {@code java.time.Instant}
 * results in an {@link UnsupportedOperationException} since those types do not
 * have time or date components, respectively.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Date#toInstant()
 * @see InstantToDateConverter
 * @since 5.0
 */
final class DateToInstantConverter implements Converter<Date, Instant> {

  @Override
  public Instant convert(Date date) {
    return date.toInstant();
  }

}
