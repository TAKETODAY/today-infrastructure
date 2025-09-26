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

package infra.format.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.format.annotation.Delimiter;
import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * Converts a {@link Delimiter delimited} String to an Array.
 *
 * @author Phillip Webb
 */
final class DelimitedStringToArrayConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  DelimitedStringToArrayConverter(ConversionService conversionService) {
    Assert.notNull(conversionService, "ConversionService is required");
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(String.class, Object[].class));
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
    Object target = Array.newInstance(elementDescriptor.getType(), elements.length);
    for (int i = 0; i < elements.length; i++) {
      String sourceElement = elements[i];
      Object targetElement = this.conversionService.convert(sourceElement.trim(), sourceType, elementDescriptor);
      Array.set(target, i, targetElement);
    }
    return target;
  }

  private String[] getElements(String source, String delimiter) {
    return StringUtils.delimitedListToStringArray(source, Delimiter.NONE.equals(delimiter) ? null : delimiter);
  }

}
