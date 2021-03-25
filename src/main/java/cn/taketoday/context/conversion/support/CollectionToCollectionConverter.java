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

import java.util.Collection;

import cn.taketoday.context.GenericDescriptor;
import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.utils.CollectionUtils;

/**
 * Converts from a Collection to another Collection.
 *
 * <p>First, creates a new Collection of the requested targetType with a size equal to the
 * size of the source Collection. Then copies each element in the source collection to the
 * target collection. Will perform an element conversion from the source collection's
 * parameterized type to the target collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY
 * @since 3.0
 */
final class CollectionToCollectionConverter extends CollectionSourceConverter {
  private final ConversionService conversionService;

  public CollectionToCollectionConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  protected boolean supportsInternal(GenericDescriptor targetType, Class<?> sourceType) {
    // Collection.class, Collection.class
    return targetType.isCollection();
  }

  @Override
  protected Object convertInternal(GenericDescriptor targetType, Collection<?> sourceCollection) {
    // Shortcut if possible...
    boolean copyRequired = !targetType.isInstance(sourceCollection);
    if (!copyRequired && sourceCollection.isEmpty()) {
      return sourceCollection;
    }

    final GenericDescriptor elementType = targetType.getGeneric(Collection.class);
    if (elementType == null && !copyRequired) {
      return sourceCollection;
    }

    // At this point, we need a collection copy in any case, even if just for finding out about element copies...
    Collection<Object> target = CollectionUtils.createCollection(
            targetType.getType(), elementType != null ? elementType.getType() : null, sourceCollection.size());

    if (elementType == null) {
      target.addAll(sourceCollection);
    }
    else {
      final ConversionService conversionService = this.conversionService;
      for (Object sourceElement : sourceCollection) {
        Object targetElement = conversionService.convert(sourceElement, elementType);
        target.add(targetElement);
        if (sourceElement != targetElement) {
          copyRequired = true;
        }
      }
    }

    return (copyRequired ? target : sourceCollection);
  }

}
