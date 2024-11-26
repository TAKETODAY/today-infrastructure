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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.lang.Nullable;
import infra.util.ObjectUtils;

/**
 * Converts an array to a comma-delimited String. First adapts the source array
 * to a List, then delegates to {@link CollectionToStringConverter} to perform
 * the target String conversion.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class ArrayToStringConverter implements ConditionalGenericConverter {

  private final CollectionToStringConverter helperConverter;

  public ArrayToStringConverter(ConversionService conversionService) {
    this.helperConverter = new CollectionToStringConverter(conversionService);
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Object[].class, String.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return this.helperConverter.matches(sourceType, targetType);
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    return helperConverter.convert(Arrays.asList(ObjectUtils.toObjectArray(source)), sourceType, targetType);
  }

}
