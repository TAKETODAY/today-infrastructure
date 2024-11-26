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

import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * Converts from a String to a {@link Enum} by calling {@link Enum#valueOf(Class, String)}.
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 * @since 3.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
final class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

  @Override
  public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
    return new StringToEnum(ClassUtils.getEnumType(targetType));
  }

  private record StringToEnum<T extends Enum>(Class<T> enumType) implements Converter<String, T> {

    @Override
    @Nullable
    public T convert(String source) {
      if (StringUtils.isEmpty(source)) {
        // It's an empty enum identifier: reset the enum value to null.
        return null;
      }
      return (T) Enum.valueOf(this.enumType, source.trim());
    }
  }

}
