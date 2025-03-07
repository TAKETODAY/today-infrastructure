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

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.Converter;
import infra.core.conversion.GenericConverter;
import infra.format.annotation.DurationFormat;
import infra.format.annotation.DurationUnit;
import infra.lang.Nullable;

/**
 * {@link Converter} to convert from a {@link Number} to a {@link Duration}. Supports
 * {@link Duration#parse(CharSequence)} as well a more readable {@code 10s} form.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DurationFormat
 * @see DurationUnit
 * @since 4.0
 */
final class NumberToDurationConverter implements GenericConverter {

  private final StringToDurationConverter delegate = new StringToDurationConverter();

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Number.class, Duration.class));
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    return this.delegate.convert(
            source != null ? source.toString() : null, TypeDescriptor.valueOf(String.class), targetType);
  }

}
