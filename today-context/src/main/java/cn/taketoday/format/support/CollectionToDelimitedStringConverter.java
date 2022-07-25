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

package cn.taketoday.format.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.Delimiter;
import cn.taketoday.lang.Nullable;

/**
 * Converts a Collection to a delimited String.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Phillip Webb
 * @since 4.0
 */
final class CollectionToDelimitedStringConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  CollectionToDelimitedStringConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    TypeDescriptor sourceElementType = sourceType.getElementDescriptor();
    if (targetType == null || sourceElementType == null) {
      return true;
    }
    return this.conversionService.canConvert(sourceElementType, targetType)
            || sourceElementType.getType().isAssignableFrom(targetType.getType());
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    Collection<?> sourceCollection = (Collection<?>) source;
    return convert(sourceCollection, sourceType, targetType);
  }

  private Object convert(Collection<?> source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source.isEmpty()) {
      return "";
    }
    return source.stream().map((element) -> convertElement(element, sourceType, targetType))
            .collect(Collectors.joining(getDelimiter(sourceType)));
  }

  private CharSequence getDelimiter(TypeDescriptor sourceType) {
    Delimiter annotation = sourceType.getAnnotation(Delimiter.class);
    return (annotation != null) ? annotation.value() : ",";
  }

  private String convertElement(Object element, TypeDescriptor sourceType, TypeDescriptor targetType) {
    return String.valueOf(
            this.conversionService.convert(element, sourceType.elementDescriptor(element), targetType));
  }

}
