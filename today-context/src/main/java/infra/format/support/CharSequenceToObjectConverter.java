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

import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.core.conversion.GenericConverter;

/**
 * {@link ConditionalGenericConverter} to convert {@link CharSequence} type by delegating
 * to existing {@link String} converters.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class CharSequenceToObjectConverter implements ConditionalGenericConverter {

  private static final TypeDescriptor STRING = TypeDescriptor.valueOf(String.class);

  private static final TypeDescriptor BYTE_ARRAY = TypeDescriptor.valueOf(byte[].class);

  private static final Set<GenericConverter.ConvertiblePair> TYPES;

  private final ThreadLocal<Boolean> disable = new ThreadLocal<>();

  static {
    TYPES = Collections.singleton(new ConvertiblePair(CharSequence.class, Object.class));
  }

  private final ConversionService conversionService;

  CharSequenceToObjectConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return TYPES;
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (sourceType.getType() == String.class || this.disable.get() == Boolean.TRUE) {
      return false;
    }
    this.disable.set(Boolean.TRUE);
    try {
      boolean canDirectlyConvertCharSequence = this.conversionService.canConvert(sourceType, targetType);
      if (canDirectlyConvertCharSequence && !isStringConversionBetter(sourceType, targetType)) {
        return false;
      }
      return this.conversionService.canConvert(STRING, targetType);
    }
    finally {
      this.disable.remove();
    }
  }

  /**
   * Return if String based conversion is better based on the target type. This is
   * required when ObjectTo... conversion produces incorrect results.
   *
   * @param sourceType the source type to test
   * @param targetType the target type to test
   * @return if string conversion is better
   */
  private boolean isStringConversionBetter(TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (this.conversionService instanceof ApplicationConversionService applicationConversionService) {
      if (applicationConversionService.isConvertViaObjectSourceType(sourceType, targetType)) {
        // If an ObjectTo... converter is being used then there might be a better
        // StringTo... version
        return true;
      }
    }
    // StringToArrayConverter / StringToCollectionConverter are better than
    // ObjectToArrayConverter / ObjectToCollectionConverter
    return (targetType.isArray() || targetType.isCollection()) && !targetType.equals(BYTE_ARRAY);
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    return this.conversionService.convert(source != null ? source.toString() : null, STRING, targetType);
  }

}
