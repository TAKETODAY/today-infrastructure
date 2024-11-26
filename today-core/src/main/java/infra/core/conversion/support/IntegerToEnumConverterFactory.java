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
import infra.util.ClassUtils;

/**
 * Converts from a Integer to a {@link Enum} by calling {@link Class#getEnumConstants()}.
 *
 * @author Yanming Zhou
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
final class IntegerToEnumConverterFactory implements ConverterFactory<Integer, Enum> {

  @Override
  public <T extends Enum> Converter<Integer, T> getConverter(Class<T> targetType) {
    return new IntegerToEnum(ClassUtils.getEnumType(targetType));
  }

  private record IntegerToEnum<T extends Enum>(Class<T> enumType) implements Converter<Integer, T> {

    @Override
    public T convert(Integer source) {
      return this.enumType.getEnumConstants()[source];
    }
  }

}
