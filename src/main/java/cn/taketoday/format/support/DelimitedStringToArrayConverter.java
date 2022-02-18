/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.format.support;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.annotation.Delimiter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Converts a {@link Delimiter delimited} String to an Array.
 *
 * @author Phillip Webb
 */
final class DelimitedStringToArrayConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  DelimitedStringToArrayConverter(ConversionService conversionService) {
    Assert.notNull(conversionService, "ConversionService must not be null");
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
