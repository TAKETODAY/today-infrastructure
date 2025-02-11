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

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.lang.Nullable;
import infra.util.CollectionUtils;

/**
 * Converts an Object to a single-element Collection containing the Object.
 * Will convert the Object to the target Collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ObjectToCollectionConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  public ObjectToCollectionConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Object.class, Collection.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return ConversionUtils.canConvertElements(sourceType, targetType.getElementDescriptor(), this.conversionService);
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }

    TypeDescriptor elementDesc = targetType.getElementDescriptor();
    Collection<Object> target = CollectionUtils.createCollection(targetType.getType(),
            (elementDesc != null ? elementDesc.getType() : null), 1);

    if (elementDesc == null || elementDesc.isCollection()) {
      target.add(source);
    }
    else {
      Object singleElement = this.conversionService.convert(source, sourceType, elementDesc);
      target.add(singleElement);
    }
    return target;
  }

}
