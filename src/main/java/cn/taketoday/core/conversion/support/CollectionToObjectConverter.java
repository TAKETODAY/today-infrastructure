/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.util.Collection;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;

/**
 * Converts a Collection to an Object by returning the first
 * collection element after converting it to the desired targetType.
 *
 * @author Keith Donald
 * @author TODAY
 * @since 3.0
 */
final class CollectionToObjectConverter extends CollectionSourceConverter {
  private final ConversionService conversionService;

  public CollectionToObjectConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  protected boolean supportsInternal(TypeDescriptor targetType, Class<?> sourceType) {
    // Collection.class, Object.class
    final ResolvableType resolvableType = ResolvableType.fromClass(sourceType).asCollection();
    if (resolvableType.hasGenerics()) {
      final ResolvableType generic = resolvableType.getGeneric(0);
      return generic != ResolvableType.NONE && this.conversionService.canConvert(generic.toClass(), targetType);
    }
    return false;
  }

  @Override
  protected Object convertInternal(TypeDescriptor targetType, Collection<?> sourceCollection) {
    if (targetType.isInstance(sourceCollection)) {
      return sourceCollection;
    }

    if (sourceCollection.isEmpty()) {
      return null;
    }
    Object firstElement = sourceCollection.iterator().next();
    return this.conversionService.convert(firstElement, targetType);
  }

}
