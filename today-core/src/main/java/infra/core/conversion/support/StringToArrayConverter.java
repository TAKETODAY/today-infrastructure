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

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConditionalGenericConverter;
import infra.core.conversion.ConversionService;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * Converts a comma-delimited String to an Array.
 * Only matches if String.class can be converted to the target array element type.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 3.0
 */
final class StringToArrayConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  public StringToArrayConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(String.class, Object[].class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return ConversionUtils.canConvertElements(
            sourceType, targetType.getElementDescriptor(), conversionService);
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    String string = (String) source;
    String[] fields = StringUtils.commaDelimitedListToStringArray(string);
    TypeDescriptor targetElementType = targetType.getElementDescriptor();
    Assert.state(targetElementType != null, "No target element type");
    Object target = Array.newInstance(targetElementType.getType(), fields.length);
    for (int i = 0; i < fields.length; i++) {
      String sourceElement = fields[i];
      Object targetElement = conversionService.convert(sourceElement.trim(), sourceType, targetElementType);
      Array.set(target, i, targetElement);
    }
    return target;
  }

}
