/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.conversion.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.lang.Assert;

/**
 * Converts a Collection to an array.
 *
 * <p>First, creates a new array of the requested targetType with a length equal to the
 * size of the source Collection. Then sets each collection element into the array.
 * Will perform an element conversion from the collection's parameterized type to the
 * array's component type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY
 * @since 3.0
 */
final class CollectionToArrayConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  public CollectionToArrayConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Collection.class, Object[].class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return ConversionUtils.canConvertElements(
            sourceType.getElementDescriptor(),
            targetType.getElementDescriptor(),
            conversionService);
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    TypeDescriptor targetElementType = targetType.getElementDescriptor();
    Assert.state(targetElementType != null, "No target element type");

    Collection<?> sourceCollection = (Collection<?>) source;
    Object array = Array.newInstance(targetElementType.getType(), sourceCollection.size());
    int i = 0;
    for (Object sourceElement : sourceCollection) {
      Object targetElement = conversionService.convert(sourceElement,
              sourceType.elementDescriptor(sourceElement), targetElementType);
      Array.set(array, i++, targetElement);
    }
    return array;
  }

}
