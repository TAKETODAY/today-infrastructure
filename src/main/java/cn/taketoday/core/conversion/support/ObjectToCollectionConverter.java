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

import java.util.Collection;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.MatchingConverter;
import cn.taketoday.util.CollectionUtils;

/**
 * Converts an Object to a single-element Collection containing the Object.
 * Will convert the Object to the target Collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ObjectToCollectionConverter implements MatchingConverter {
  private final ConversionService conversionService;

  public ObjectToCollectionConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(final TypeDescriptor targetType, final Class<?> sourceType) {
    // Object.class, Collection.class
    if (targetType.isCollection()) {
      final TypeDescriptor elementType = targetType.getGeneric(Collection.class);
      return elementType == null || conversionService.canConvert(sourceType, elementType);
    }
    return false;
  }

  @Override
  public Object convert(final TypeDescriptor targetType, final Object source) {
    final TypeDescriptor elementType = targetType.getElementDescriptor();
    Collection<Object> target = CollectionUtils.createCollection(
            targetType.getType(), (elementType != null ? elementType.getType() : null), 1);
    if (elementType == null || elementType.isCollection()) {
      target.add(source);
    }
    else {
      Object singleElement = this.conversionService.convert(source, elementType);
      target.add(singleElement);
    }
    return target;
  }

}
