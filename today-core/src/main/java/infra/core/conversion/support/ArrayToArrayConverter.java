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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.util.ObjectUtils;

/**
 * Converts an array to another array. First adapts the source array to a List,
 * then delegates to {@link CollectionToArrayConverter} to perform the target
 * array conversion.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 3.0
 */
final class ArrayToArrayConverter implements ConditionalGenericConverter {

  private final CollectionToArrayConverter helperConverter;

  private final ConversionService conversionService;

  public ArrayToArrayConverter(ConversionService conversionService) {
    this.helperConverter = new CollectionToArrayConverter(conversionService);
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Object[].class, Object[].class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return helperConverter.matches(sourceType, targetType);
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (conversionService instanceof GenericConversionService conversion) {
      TypeDescriptor targetElement = targetType.getElementDescriptor();
      if (targetElement != null && targetType.getType().isInstance(source) &&
              conversion.canBypassConvert(sourceType.getElementDescriptor(), targetElement)) {
        return source;
      }
    }
    List<Object> sourceList = Arrays.asList(ObjectUtils.toObjectArray(source));
    return helperConverter.convert(sourceList, sourceType, targetType);
  }

}
