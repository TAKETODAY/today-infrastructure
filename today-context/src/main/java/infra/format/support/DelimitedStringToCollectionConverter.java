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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.format.annotation.Delimiter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * Converts a {@link Delimiter delimited} String to a Collection.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DelimitedStringToCollectionConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  DelimitedStringToCollectionConverter(ConversionService conversionService) {
    Assert.notNull(conversionService, "ConversionService is required");
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(String.class, Collection.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return targetType.getElementDescriptor() == null
            || this.conversionService.canConvert(sourceType, targetType.getElementDescriptor());
  }

  @Nullable
  @Override
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    return convert((String) source, sourceType, targetType);
  }

  private Object convert(String source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    Delimiter delimiter = targetType.getAnnotation(Delimiter.class);
    String[] elements = getElements(source, delimiter != null ? delimiter.value() : ",");
    TypeDescriptor elementDescriptor = targetType.getElementDescriptor();
    Collection<Object> target = createCollection(targetType, elementDescriptor, elements.length);
    Stream<Object> stream = Arrays.stream(elements).map(String::trim);
    if (elementDescriptor != null) {
      stream = stream.map((element) -> this.conversionService.convert(element, sourceType, elementDescriptor));
    }
    stream.forEach(target::add);
    return target;
  }

  private Collection<Object> createCollection(
          TypeDescriptor targetType, @Nullable TypeDescriptor elementDescriptor, int length) {
    return CollectionUtils.createCollection(
            targetType.getType(), elementDescriptor != null ? elementDescriptor.getType() : null, length);
  }

  private String[] getElements(String source, String delimiter) {
    return StringUtils.delimitedListToStringArray(source, Delimiter.NONE.equals(delimiter) ? null : delimiter);
  }

}
