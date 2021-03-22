/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.conversion.support;

import java.io.StringWriter;

import cn.taketoday.context.conversion.TypeConverter;

/**
 * Simply calls {@link Object#toString()} to convert any supported object
 * to a {@link String}.
 *
 * <p>Supports {@link CharSequence}, {@link StringWriter}, and any class
 * with a String constructor or one of the following static factory methods:
 * {@code valueOf(String)}, {@code of(String)}, {@code from(String)}.
 *
 * <p>Used by the {@link DefaultConversionService} as a fallback if there
 * are no other explicit to-String converters registered.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ObjectToObjectConverter
 * @since 3.0
 */
final class FallbackObjectToStringConverter implements TypeConverter {

  // Object.class -> String.class

  @Override
  public boolean supports(Class<?> targetType, Class<?> sourceType) {
    if (String.class == sourceType) {
      // no conversion required
      return false;
    }
    return (CharSequence.class.isAssignableFrom(sourceType)
            || StringWriter.class.isAssignableFrom(sourceType)
            || ObjectToObjectConverter.hasConversionMethodOrConstructor(sourceType, String.class));
  }

  @Override
  public Object convert(Class<?> targetType, Object source) {
    return (source != null ? source.toString() : null);
  }
}
