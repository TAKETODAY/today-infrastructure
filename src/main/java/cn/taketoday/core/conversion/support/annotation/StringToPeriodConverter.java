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

package cn.taketoday.core.conversion.support.annotation;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link Converter} to convert from a {@link String} to a {@link Period}. Supports
 * {@link Period#parse(CharSequence)} as well a more readable form.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @see PeriodFormat
 * @see PeriodUnit
 */
final class StringToPeriodConverter implements GenericConverter {

  @Override
  public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new GenericConverter.ConvertiblePair(String.class, Period.class));
  }

  @Override
  public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (ObjectUtils.isEmpty(source)) {
      return null;
    }
    return convert(source.toString(), getStyle(targetType), getPeriodUnit(targetType));
  }

  private PeriodStyle getStyle(TypeDescriptor targetType) {
    PeriodFormat annotation = targetType.getAnnotation(PeriodFormat.class);
    return (annotation != null) ? annotation.value() : null;
  }

  private ChronoUnit getPeriodUnit(TypeDescriptor targetType) {
    PeriodUnit annotation = targetType.getAnnotation(PeriodUnit.class);
    return (annotation != null) ? annotation.value() : null;
  }

  private Period convert(String source, PeriodStyle style, ChronoUnit unit) {
    style = (style != null) ? style : PeriodStyle.detect(source);
    return style.parse(source, unit);
  }

}
