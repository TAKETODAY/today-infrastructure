/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
