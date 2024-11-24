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

import java.time.Period;
import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.Converter;
import infra.core.conversion.GenericConverter;
import infra.format.annotation.PeriodFormat;
import infra.format.annotation.PeriodUnit;
import infra.lang.Nullable;

/**
 * {@link Converter} to convert from a {@link Number} to a {@link Period}. Supports
 * {@link Period#parse(CharSequence)} as well a more readable {@code 10m} form.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PeriodFormat
 * @see PeriodUnit
 * @since 4.0
 */
final class NumberToPeriodConverter implements GenericConverter {

  private final StringToPeriodConverter delegate = new StringToPeriodConverter();

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Number.class, Period.class));
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    return this.delegate.convert((source != null) ? source.toString() : null,
            TypeDescriptor.valueOf(String.class), targetType);
  }

}
