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

package infra.expression;

import org.jspecify.annotations.Nullable;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;

/**
 * A type converter can convert values between different types encountered during
 * expression evaluation. This is an SPI for the expression parser; see
 * {@link ConversionService} for the primary
 * user API to Framework's conversion facilities.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface TypeConverter {

  /**
   * Return {@code true} if the type converter can convert the specified type
   * to the desired target type.
   *
   * @param sourceType a type descriptor that describes the source type
   * @param targetType a type descriptor that describes the requested result type
   * @return {@code true} if that conversion can be performed
   */
  boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

  /**
   * Convert (or coerce) a value from one type to another, for example from a
   * {@code boolean} to a {@code String}.
   * <p>The {@link TypeDescriptor} parameters enable support for typed collections:
   * A caller may prefer a {@code List<Integer>}, for example, rather than
   * simply any {@code List}.
   *
   * @param value the value to be converted
   * @param sourceType a type descriptor that supplies extra information about the
   * source object
   * @param targetType a type descriptor that supplies extra information about the
   * requested result type
   * @return the converted value
   * @throws EvaluationException if conversion failed or is not possible to begin with
   */
  @Nullable
  Object convertValue(@Nullable Object value, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}
