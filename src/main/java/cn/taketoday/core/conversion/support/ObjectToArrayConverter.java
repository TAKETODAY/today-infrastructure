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

import java.lang.reflect.Array;

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.util.GenericDescriptor;

/**
 * Converts an Object to a single-element array containing the Object.
 * Will convert the Object to the target array's component type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY
 * @since 3.0
 */
final class ObjectToArrayConverter extends ToArrayConverter {
  private final ConversionService conversionService;

  public ObjectToArrayConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

	@Override
	protected boolean supportsInternal(GenericDescriptor targetType, final Class<?> sourceType) {
		// Object.class, Object[].class
    final Class<?> componentType = targetType.getComponentType();
    return conversionService.canConvert(sourceType, componentType);
	}

	@Override
	public Object convert(GenericDescriptor targetType, Object source) {
    final GenericDescriptor targetElementType = targetType.getElementDescriptor();

    Object target = Array.newInstance(targetElementType.getType(), 1);
    Object targetElement = this.conversionService.convert(source, targetElementType);
    Array.set(target, 0, targetElement);
    return target;
  }

}
