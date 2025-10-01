/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.util.CollectionUtils;

/**
 * Converts an array to a Collection.
 *
 * <p>First, creates a new Collection of the requested target type.
 * Then adds each array element to the target collection.
 * Will perform an element conversion from the source component type
 * to the collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ArrayToCollectionConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  public ArrayToCollectionConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Object[].class, Collection.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return ConversionUtils.canConvertElements(
            sourceType.getElementDescriptor(), targetType.getElementDescriptor(), this.conversionService);
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }

    int length = Array.getLength(source);
    TypeDescriptor elementDesc = targetType.getElementDescriptor();
    Collection<Object> target = createCollection(targetType.getType(),
            (elementDesc != null ? elementDesc.getType() : null), length);

    if (elementDesc == null) {
      for (int i = 0; i < length; i++) {
        Object sourceElement = Array.get(source, i);
        target.add(sourceElement);
      }
    }
    else {
      for (int i = 0; i < length; i++) {
        Object sourceElement = Array.get(source, i);
        Object targetElement = this.conversionService.convert(sourceElement,
                sourceType.elementDescriptor(sourceElement), elementDesc);
        target.add(targetElement);
      }
    }
    return target;
  }

  private Collection<Object> createCollection(Class<?> targetType, @Nullable Class<?> elementType, int length) {
    if (targetType.isInterface() && targetType.isAssignableFrom(ArrayList.class)) {
      // Source is an array -> prefer ArrayList for Collection and SequencedCollection.
      // CollectionFactory.createCollection traditionally prefers LinkedHashSet instead.
      return new ArrayList<>(length);
    }
    return CollectionUtils.createCollection(targetType, elementType, length);
  }

}
