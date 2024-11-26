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

package infra.format.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.lang.Nullable;
import infra.util.ObjectUtils;

/**
 * Converts an array to a delimited String.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ArrayToDelimitedStringConverter implements ConditionalGenericConverter {

  private final CollectionToDelimitedStringConverter delegate;

  ArrayToDelimitedStringConverter(ConversionService conversionService) {
    this.delegate = new CollectionToDelimitedStringConverter(conversionService);
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Object[].class, String.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return this.delegate.matches(sourceType, targetType);
  }

  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    List<Object> list = Arrays.asList(ObjectUtils.toObjectArray(source));
    return this.delegate.convert(list, sourceType, targetType);
  }

}
