/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.core.conversion.support;

import java.io.StringWriter;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.MatchingConverter;

/**
 * Simply calls {@link Object#toString()} to convert any supported object
 * to a {@link String}.
 * If source is instance of {@code targetType}, just return {@code source}
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
final class FallbackConverter implements MatchingConverter, Ordered {

  // Object.class -> String.class

  @Override
  public boolean supports(TypeDescriptor targetType, Class<?> sourceType) {
    if (targetType.isAssignableFrom(sourceType)) {
      return true;
    }
    if (String.class == sourceType) {
      // no conversion required
      return false;
    }
    return CharSequence.class.isAssignableFrom(sourceType)
            || StringWriter.class.isAssignableFrom(sourceType)
            || ObjectToObjectConverter.hasConversionMethodOrConstructor(sourceType, String.class);
  }

  @Override
  public Object convert(final TypeDescriptor targetType, final Object source) {
    if (targetType.isInstance(source)) {
      return source;
    }
    return (source != null ? source.toString() : null);
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }
}
