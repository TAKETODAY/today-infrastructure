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
import java.util.List;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.util.StringUtils;

/**
 * Converts a comma-delimited String to an Array.
 * Only matches if String.class can be converted to the target array element type.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY
 * @see StringUtils#splitAsList(String)
 * @since 3.0
 */
final class StringToArrayConverter extends ToArrayConverter {
  private final ConversionService conversionService;

  public StringToArrayConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  protected boolean supportsInternal(TypeDescriptor targetType, final Class<?> sourceType) {
    // String.class, Object[].class
    return sourceType == String.class;
  }

  @Override
  public Object convert(final TypeDescriptor targetType, final Object source) {
    final String string = (String) source;
    final List<String> fields = StringUtils.splitAsList(string);

    final Class<?> targetElementType = targetType.getComponentType();
    final Object target = Array.newInstance(targetElementType, fields.size());
    final ConversionService conversionService = this.conversionService;

    int i = 0;
    for (final String sourceElement : fields) {
      Object targetElement = conversionService.convert(sourceElement.trim(), targetElementType);
      Array.set(target, i++, targetElement);
    }
    return target;
  }

}
