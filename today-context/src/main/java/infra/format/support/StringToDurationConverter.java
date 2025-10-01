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

package infra.format.support;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.Converter;
import infra.core.conversion.GenericConverter;
import infra.format.annotation.DurationFormat;
import infra.format.annotation.DurationFormat.Style;
import infra.format.annotation.DurationFormat.Unit;
import infra.format.annotation.DurationUnit;
import infra.format.datetime.standard.DurationFormatterUtils;
import infra.util.ObjectUtils;

/**
 * {@link Converter} to convert from a {@link String} to a {@link Duration}. Supports
 * {@link Duration#parse(CharSequence)} as well a more readable {@code 10s} form.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DurationFormat
 * @see DurationUnit
 * @since 4.0
 */
final class StringToDurationConverter implements GenericConverter {

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(String.class, Duration.class));
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (ObjectUtils.isEmpty(source)) {
      return null;
    }

    Unit unit = null;
    Style style = null;
    DurationFormat durationFormat = targetType.getAnnotation(DurationFormat.class);
    if (durationFormat != null) {
      style = durationFormat.style();
      unit = durationFormat.defaultUnit();
    }

    DurationUnit annotation = targetType.getAnnotation(DurationUnit.class);
    if (annotation != null) {
      unit = Unit.fromChronoUnit(annotation.value());
    }

    if (style == null) {
      return DurationFormatterUtils.detectAndParse(source.toString(), unit);
    }
    return DurationFormatterUtils.parse(source.toString(), style, unit);
  }

}
