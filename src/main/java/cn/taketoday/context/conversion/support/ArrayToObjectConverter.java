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

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.utils.GenericDescriptor;

/**
 * Converts an array to an Object by returning the first array element
 * after converting it to the desired target type.
 *
 * @author Keith Donald
 * @author TODAY
 * @since 3.0
 */
final class ArrayToObjectConverter extends ArraySourceConverter {

  private final ConversionService conversionService;

  public ArrayToObjectConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  protected boolean supportsInternal(final GenericDescriptor targetType, Class<?> sourceType) {
    // Object[].class -> Object.class
    final Class<?> componentType = sourceType.getComponentType();
    return conversionService.canConvert(componentType, targetType.getType());
  }

  @Override
  public Object convert(final GenericDescriptor targetType, final Object source) {
    if (targetType.isInstance(source)) {
      return source;
    }
    if (Array.getLength(source) == 0) {
      return null;
    }
    // first element
    Object firstElement = Array.get(source, 0);
    return this.conversionService.convert(firstElement, targetType);
  }

}
