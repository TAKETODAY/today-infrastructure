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

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalConverter;
import infra.core.conversion.Converter;

/**
 * Convert a {@link Instant} to a {@link Date}.
 *
 * <p>This does not include conversion support for target types which are subtypes
 * of {@code java.util.Date}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Date#from(Instant)
 * @see DateToInstantConverter
 * @since 5.0
 */
final class InstantToDateConverter implements ConditionalConverter, Converter<Instant, Date> {

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return targetType.getType().equals(Date.class);
  }

  @Override
  public Date convert(Instant instant) {
    return Date.from(instant);
  }

}
