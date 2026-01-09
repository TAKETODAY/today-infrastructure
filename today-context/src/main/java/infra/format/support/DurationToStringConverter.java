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

/**
 * {@link Converter} to convert from a {@link Duration} to a {@link String}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DurationFormat
 * @see DurationUnit
 * @since 4.0
 */
final class DurationToStringConverter implements GenericConverter {

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Duration.class, String.class));
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source instanceof Duration) {
      Unit unit = null;
      Style style = Style.ISO8601;
      DurationFormat durationFormat = sourceType.getAnnotation(DurationFormat.class);
      if (durationFormat != null) {
        style = durationFormat.style();
        unit = durationFormat.defaultUnit();
      }

      DurationUnit durationUnit = sourceType.getAnnotation(DurationUnit.class);
      if (durationUnit != null) {
        unit = Unit.fromChronoUnit(durationUnit.value());
      }
      return DurationFormatterUtils.print((Duration) source, style, unit);
    }
    return null;
  }

}
