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

package cn.taketoday.format.support;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.format.annotation.DurationFormat;
import cn.taketoday.format.annotation.DurationFormat.Unit;
import cn.taketoday.format.annotation.DurationUnit;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link Converter} to convert from a {@link Duration} to a {@link Number}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DurationFormat
 * @see DurationUnit
 * @since 4.0
 */
final class DurationToNumberConverter implements GenericConverter {

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Duration.class, Number.class));
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    Unit unit = getUnit(sourceType);
    if (unit == null) {
      unit = Unit.MILLIS;
    }
    return convert((Duration) source, unit, targetType.getObjectType());
  }

  @Nullable
  static Unit getUnit(TypeDescriptor sourceType) {
    DurationUnit annotation = sourceType.getAnnotation(DurationUnit.class);
    if (annotation != null) {
      return Unit.fromChronoUnit(annotation.value());
    }

    DurationFormat durationFormat = sourceType.getAnnotation(DurationFormat.class);
    if (durationFormat != null) {
      return durationFormat.defaultUnit();
    }
    return null;
  }

  private Object convert(Duration source, Unit unit, Class<?> type) {
    try {
      return type.getConstructor(String.class)
              .newInstance(String.valueOf(unit.longValue(source)));
    }
    catch (Exception ex) {
      ReflectionUtils.rethrowRuntimeException(ex);
      throw new IllegalStateException(ex);
    }
  }

}
