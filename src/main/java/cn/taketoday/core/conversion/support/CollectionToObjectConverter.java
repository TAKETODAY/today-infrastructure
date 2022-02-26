/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Nullable;

/**
 * Converts a Collection to an Object by returning the first collection element after converting it to the desired targetType.
 *
 * @author Keith Donald
 * @author TODAY
 * @since 3.0
 */
final class CollectionToObjectConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  public CollectionToObjectConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Collection.class, Object.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return ConversionUtils.canConvertElements(sourceType.getElementDescriptor(), targetType, conversionService);
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    if (sourceType.isAssignableTo(targetType)) {
      return source;
    }
    Collection<?> sourceCollection = (Collection<?>) source;
    if (sourceCollection.isEmpty()) {
      return null;
    }
    Object firstElement = sourceCollection.iterator().next();
    return conversionService.convert(firstElement, sourceType.elementDescriptor(firstElement), targetType);
  }

}
