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

package infra.core.conversion.support;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;

/**
 * Convert an {@link Optional} to an {@link Object} by unwrapping the {@code Optional},
 * using the {@link ConversionService} to convert the object contained in the
 * {@code Optional} (potentially {@code null}) to the target type.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ObjectToOptionalConverter
 * @since 5.0
 */
final class OptionalToObjectConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  OptionalToObjectConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Set.of(new ConvertiblePair(Optional.class, Object.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return ConversionUtils.canConvertElements(sourceType.getElementDescriptor(), targetType, this.conversionService);
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    Optional<?> optional = (Optional<?>) source;
    Object unwrappedSource = optional.orElse(null);
    TypeDescriptor unwrappedSourceType = TypeDescriptor.forObject(unwrappedSource);
    return this.conversionService.convert(unwrappedSource, unwrappedSourceType, targetType);
  }

}
