/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.core.conversion.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.lang.Nullable;

/**
 * Converts a Collection to a comma-delimited String.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class CollectionToStringConverter implements ConditionalGenericConverter {

  private static final String DELIMITER = ",";

  private final ConversionService conversionService;

  public CollectionToStringConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return ConversionUtils.canConvertElements(
            sourceType.getElementDescriptor(), targetType, this.conversionService);
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    Collection<?> sourceCollection = (Collection<?>) source;
    if (sourceCollection.isEmpty()) {
      return "";
    }
    StringJoiner sj = new StringJoiner(DELIMITER);
    for (Object sourceElement : sourceCollection) {
      Object targetElement = this.conversionService.convert(
              sourceElement, sourceType.elementDescriptor(sourceElement), targetType);
      sj.add(String.valueOf(targetElement));
    }
    return sj.toString();
  }

}
