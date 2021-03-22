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
package cn.taketoday.context.conversion.support;

import java.lang.reflect.Array;
import java.util.Collection;

import cn.taketoday.context.GenericDescriptor;
import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.utils.CollectionUtils;

/**
 * Converts an array to a Collection.
 *
 * <p>First, creates a new Collection of the requested target type.
 * Then adds each array element to the target collection.
 * Will perform an element conversion from the source component type
 * to the collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ArrayToCollectionConverter extends ArraySourceConverter implements TypeConverter {
  private final ConversionService conversionService;

  public ArrayToCollectionConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  protected boolean supportsInternal(GenericDescriptor targetType, Class<?> sourceType) {
    return targetType.isCollection();
  }

  @Override
  public Object convert(GenericDescriptor targetType, Object source) {
    final int length = Array.getLength(source);
    final Class<Object> elementType = targetType.getGeneric(Collection.class);
    final Collection<Object> target = CollectionUtils.createCollection(targetType.getType(), elementType, length);

    if (elementType == null) {
      for (int i = 0; i < length; i++) {
        Object sourceElement = Array.get(source, i);
        target.add(sourceElement);
      }
    }
    else {
      final ConversionService conversionService = this.conversionService;
      for (int i = 0; i < length; i++) {
        Object sourceElement = Array.get(source, i);
        Object targetElement = conversionService.convert(sourceElement, elementType);
        target.add(targetElement);
      }
    }
    return target;
  }

}
