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
import cn.taketoday.context.conversion.StringSourceTypeConverter;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * Converts a split-able String to a Collection.
 * If the target collection element type is declared, only matches if
 * {@code String.class} can be converted to it.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY
 * @see StringUtils#split(String)
 * @since 3.0
 */
final class StringToCollectionConverter extends StringSourceTypeConverter {
  private final ConversionService conversionService;

  public StringToCollectionConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supportsInternal(final GenericDescriptor targetType, final Class<?> sourceType) {
    // String.class, Collection.class
    return targetType.isCollection();
  }

  @Override
  protected Object convertInternal(GenericDescriptor targetType, String string) {
    String[] fields = StringUtils.split(string);
    final GenericDescriptor elementType = targetType.getGeneric(Collection.class);

    Collection<Object> target = CollectionUtils.createCollection(
            targetType.getType(), elementType != null ? elementType.getType() : null, fields.length);
    if (elementType == null) {
      for (String field : fields) {
        target.add(field.trim());
      }
    }
    else {
      final ConversionService conversionService = this.conversionService;
      for (String field : fields) {
        Object targetElement = conversionService.convert(field.trim(), elementType);
        target.add(targetElement);
      }
    }
    return target;
  }

}
