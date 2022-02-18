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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.format.annotation.DurationFormat;
import cn.taketoday.format.annotation.DurationStyle;
import cn.taketoday.format.annotation.DurationUnit;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link Converter} to convert from a {@link String} to a {@link Duration}. Supports
 * {@link Duration#parse(CharSequence)} as well a more readable {@code 10s} form.
 *
 * @author Phillip Webb
 * @see DurationFormat
 * @see DurationUnit
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
    return convert(source.toString(), getStyle(targetType), getDurationUnit(targetType));
  }

  @Nullable
  private DurationStyle getStyle(TypeDescriptor targetType) {
    DurationFormat annotation = targetType.getAnnotation(DurationFormat.class);
    return (annotation != null) ? annotation.value() : null;
  }

  @Nullable
  private ChronoUnit getDurationUnit(TypeDescriptor targetType) {
    DurationUnit annotation = targetType.getAnnotation(DurationUnit.class);
    return (annotation != null) ? annotation.value() : null;
  }

  private Duration convert(String source, @Nullable DurationStyle style, @Nullable ChronoUnit unit) {
    style = (style != null) ? style : DurationStyle.detect(source);
    return style.parse(source, unit);
  }

}
