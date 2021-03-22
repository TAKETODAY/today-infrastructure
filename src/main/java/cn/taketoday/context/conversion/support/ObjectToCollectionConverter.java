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

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.GenericTypeResolver;

/**
 * Converts an Object to a single-element Collection containing the Object.
 * Will convert the Object to the target Collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ObjectToCollectionConverter implements TypeConverter {
  private final ConversionService conversionService;

  public ObjectToCollectionConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(Class<?> targetType, Object source) {
    // Object.class, Collection.class
    return CollectionUtils.isCollection(targetType);
  }

  @Override
  public Object convert(Class<?> targetType, Object source) {
    final Class<Object> elementType = GenericTypeResolver.resolveTypeArgument(targetType, Collection.class);
    Collection<Object> target = CollectionUtils.createCollection(targetType, elementType, 1);

    if (elementType == null || CollectionUtils.isCollection(elementType)) {
      target.add(source);
    }
    else {
      Object singleElement = this.conversionService.convert(source, elementType);
      target.add(singleElement);
    }
    return target;
  }

}
