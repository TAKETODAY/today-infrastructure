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

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.MatchingConverter;
import cn.taketoday.util.CollectionUtils;

/**
 * Converts a Collection to an array.
 *
 * <p>First, creates a new array of the requested targetType with a length equal to the
 * size of the source Collection. Then sets each collection element into the array.
 * Will perform an element conversion from the collection's parameterized type to the
 * array's component type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY
 * @since 3.0
 */
final class CollectionToArrayConverter extends ToArrayConverter implements MatchingConverter {
  final ConversionService conversionService;

  public CollectionToArrayConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  protected boolean supportsInternal(TypeDescriptor targetType, Class<?> sourceType) {
    //Collection.class, Object[].class
    return CollectionUtils.isCollection(sourceType);
  }

  @Override
  public Object convert(TypeDescriptor targetType, Object source) {
    final Class<?> elementType = targetType.getComponentType();
    Collection<?> sourceCollection = (Collection<?>) source;
    Object array = Array.newInstance(elementType, sourceCollection.size());
    int i = 0;
    final ConversionService conversionService = this.conversionService;
    for (Object sourceElement : sourceCollection) {
      Object targetElement = conversionService.convert(sourceElement, elementType);
      Array.set(array, i++, targetElement);
    }
    return array;
  }

}
