/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.conversion.support;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Optional;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.MatchingConverter;

/**
 * Convert an Object to {@code java.util.Optional<T>} if necessary using the
 * {@code ConversionService} to convert the source Object to the generic type
 * of Optional when known.
 *
 * @author Rossen Stoyanchev
 * @author TODAY
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ObjectToOptionalConverter implements MatchingConverter {
  private final ConversionService conversionService;

  public ObjectToOptionalConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(final TypeDescriptor targetType, final Class<?> sourceType) {
    // Collection.class -> Optional.class
    // Object[].class -> Optional.class
    // Object.class -> Optional.class

    if (targetType.is(Optional.class)) {
      final TypeDescriptor valueType = targetType.getGeneric(Optional.class);
      if (valueType != null) {
        return this.conversionService.canConvert(sourceType, valueType);
      }
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object convert(final TypeDescriptor targetType, final Object source) {
    // Optional<E> -> E

    if (source instanceof Optional) {
      final Optional<Object> optional = (Optional<Object>) source;
      if (optional.isPresent()) {
        final TypeDescriptor elementType = targetType.getGeneric(Optional.class);
        final Object original = optional.get();
        if (elementType != null && !elementType.isInstance(original)) {
          final Object converted = conversionService.convert(optional, elementType);
          return Optional.of(converted);
        }
        // return original source
      }
      return source;
    }
    final TypeDescriptor elementType = targetType.getGeneric(Optional.class);
    if (elementType != null) {
      final Object target = this.conversionService.convert(source, elementType);
      if (target == null
              || (target.getClass().isArray() && Array.getLength(target) == 0)
              || (target instanceof Collection && ((Collection<?>) target).isEmpty())) {
        return Optional.empty();
      }
      return Optional.of(target);
    }
    else { // not a Optional
      return Optional.of(source);
    }
  }

}
