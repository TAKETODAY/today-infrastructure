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

package cn.taketoday.core.conversion.support;

import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.NumberUtils;

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

  private static final class StringToNumber<T extends Number> implements Converter<String, T> {

    private final Class<T> targetType;

    public StringToNumber(Class<T> targetType) {
      this.targetType = targetType;
    }

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
