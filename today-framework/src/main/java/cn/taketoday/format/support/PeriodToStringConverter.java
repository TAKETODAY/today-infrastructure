/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.format.support;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.format.annotation.PeriodFormat;
import cn.taketoday.format.annotation.PeriodStyle;
import cn.taketoday.format.annotation.PeriodUnit;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link Converter} to convert from a {@link Period} to a {@link String}.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PeriodFormat
 * @see PeriodUnit
 * @since 4.0
 */
final class PeriodToStringConverter implements GenericConverter {

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Period.class, String.class));
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (ObjectUtils.isEmpty(source)) {
      return null;
    }
    return convert((Period) source, getPeriodStyle(sourceType), getPeriodUnit(sourceType));
  }

  @Nullable
  private PeriodStyle getPeriodStyle(TypeDescriptor sourceType) {
    PeriodFormat annotation = sourceType.getAnnotation(PeriodFormat.class);
    return annotation != null ? annotation.value() : null;
  }

  private String convert(Period source, @Nullable PeriodStyle style, @Nullable ChronoUnit unit) {
    style = (style != null) ? style : PeriodStyle.ISO8601;
    return style.print(source, unit);
  }

  @Nullable
  private ChronoUnit getPeriodUnit(TypeDescriptor sourceType) {
    PeriodUnit annotation = sourceType.getAnnotation(PeriodUnit.class);
    return (annotation != null) ? annotation.value() : null;
  }

}
