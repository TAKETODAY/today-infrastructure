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

import java.util.Optional;
import java.util.function.Supplier;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.MatchingConverter;
import cn.taketoday.util.SingletonSupplier;

/**
 * Convert an Object to {@code java.util.function.Supplier<T>} if necessary using the
 * {@code ConversionService} to convert the source Object to the generic type
 * of Supplier when known.
 *
 * @author TODAY
 * @since 4.0
 */
final class ObjectToSupplierConverter implements MatchingConverter {
  private final ConversionService conversionService;

  public ObjectToSupplierConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(final TypeDescriptor targetType, final Class<?> sourceType) {
    // Object.class -> Supplier.class

    if (targetType.is(Supplier.class)) {
      final TypeDescriptor valueType = targetType.getGeneric(Supplier.class);
      if (valueType != null) {
        return valueType.isAssignableFrom(sourceType)
                || conversionService.canConvert(sourceType, valueType);
      }
    }
    return false;
  }

  @Override
  public Object convert(final TypeDescriptor targetType, final Object source) {
    // Supplier<E> <- E
    TypeDescriptor elementType = targetType.getGeneric(Optional.class);
    if (elementType != null) {
      if (elementType.isAssignableFrom(source.getClass())) {
        return SingletonSupplier.valueOf(source);
      }
      Object target = conversionService.convert(source, elementType);
      return SingletonSupplier.valueOf(target);
    }
    return SingletonSupplier.valueOf(source);
  }

}
