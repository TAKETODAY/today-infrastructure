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
import infra.util.NumberUtils;

/**
 * Converts from a String any JDK-standard Number implementation.
 *
 * <p>Support Number classes including Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal. This class
 * delegates to {@link NumberUtils#parseNumber(String, Class)} to perform the conversion.
 *
 * @author Keith Donald
 * @see Byte
 * @see Short
 * @see Integer
 * @see Long
 * @see java.math.BigInteger
 * @see Float
 * @see Double
 * @see java.math.BigDecimal
 * @see NumberUtils
 * @since 3.0
 */
final class StringToNumberConverterFactory implements ConverterFactory<String, Number> {

  @Override
  public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
    return new StringToNumber<>(targetType);
  }

  private record StringToNumber<T extends Number>(Class<T> targetType) implements Converter<String, T> {

    @Override
    @Nullable
    public T convert(String source) {
      if (source.isEmpty()) {
        return null;
      }
      return NumberUtils.parseNumber(source, this.targetType);
    }
  }

}
