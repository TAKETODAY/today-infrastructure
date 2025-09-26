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

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.Converter;
import infra.core.conversion.GenericConverter;
import infra.format.annotation.PeriodFormat;
import infra.format.annotation.PeriodStyle;
import infra.format.annotation.PeriodUnit;
import infra.util.ObjectUtils;

/**
 * {@link Converter} to convert from a {@link String} to a {@link Period}. Supports
 * {@link Period#parse(CharSequence)} as well a more readable form.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PeriodFormat
 * @see PeriodUnit
 * @since 4.0
 */
final class StringToPeriodConverter implements GenericConverter {

  @Override
  public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new GenericConverter.ConvertiblePair(String.class, Period.class));
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (ObjectUtils.isEmpty(source)) {
      return null;
    }
    return convert(source.toString(), getStyle(targetType), getPeriodUnit(targetType));
  }

  @Nullable
  private PeriodStyle getStyle(TypeDescriptor targetType) {
    PeriodFormat annotation = targetType.getAnnotation(PeriodFormat.class);
    return (annotation != null) ? annotation.value() : null;
  }

  @Nullable
  private ChronoUnit getPeriodUnit(TypeDescriptor targetType) {
    PeriodUnit annotation = targetType.getAnnotation(PeriodUnit.class);
    return (annotation != null) ? annotation.value() : null;
  }

  private Period convert(String source, @Nullable PeriodStyle style, @Nullable ChronoUnit unit) {
    style = (style != null) ? style : PeriodStyle.detect(source);
    return style.parse(source, unit);
  }

}
